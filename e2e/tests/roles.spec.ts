import { test, expect, type Page } from '@playwright/test';
import {
  DEMO_USER,
  chooseRowAction,
  expectSuccessToast,
  searchTable,
  submitFormAsBrowser,
  tableRow,
} from './support/app';

/**
 * Role-based access (config/AppRole). Each test signs in as that role's demo
 * account (application.properties: app.security.demo-users) and checks both
 * what the UI offers and that the server refuses what the UI hides.
 */
test.use({ storageState: { cookies: [], origins: [] } });

const ACCOUNTS = {
  clerk: 'clerk@phlpost.gov.ph',
  finance: 'finance@phlpost.gov.ph',
  viewer: 'viewer@phlpost.gov.ph',
};

async function signInAs(page: Page, email: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(email);
  await page.locator('#password').fill(DEMO_USER.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Enterprise Asset Overview' })).toBeVisible();
}

async function csrfHeader(page: Page): Promise<Record<string, string>> {
  return { 'X-CSRF-TOKEN': await page.locator('input[name=_csrf]').first().inputValue() };
}

test.describe('admin', () => {
  test.beforeEach(async ({ page }) => signInAs(page, DEMO_USER.email));

  test('reloads personnel reference data on demand', async ({ page }) => {
    await page.getByRole('button', { name: /admin/ }).click();
    await page.getByRole('link', { name: 'Manage PSGC Data' }).click();
    await page.getByRole('button', { name: 'Reload Now' }).click();
    await expect(page.getByText('Personnel reference data reloaded.')).toBeVisible();
  });
});

test.describe('viewer', () => {
  test.beforeEach(async ({ page }) => signInAs(page, ACCOUNTS.viewer));

  test('sees the registry but no way to change it', async ({ page }) => {
    await expect(page.locator('#userRoleLabel')).toHaveText('Role: Viewer');
    await expect(page.getByRole('button', { name: '+ Receive Asset' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Manage PSGC Data' })).toHaveCount(0);

    await page.goto('/assets');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-STORAGE');
    const row = tableRow(page, 'assetsTable', 'E2E-ASSET-STORAGE');
    await row.getByRole('button', { name: 'Actions' }).click();
    await expect(row.locator('.dropdown-menu a, .dropdown-menu button')).toHaveText(['View Asset History']);

    await row.locator('a.asset-detail-link').click();
    await expect(page.locator('#itDetailOffcanvas')).toBeVisible();
    await expect(page.locator('#enableEditBtn')).toHaveCount(0);
  });

  test('is refused a direct workflow request, with a message', async ({ page }) => {
    await page.goto('/assets');
    await submitFormAsBrowser(page, '/assets/assign', { assetTag: 'E2E-ASSET-STORAGE', employeeID: 'E2E-001' });
    await page.waitForURL(/\/assets/);
    await expect(page.locator('#errorToast')).toContainText('Your role does not allow this action.');
    expect((await (await page.request.get('/assets/E2E-ASSET-STORAGE')).json()).currentOwnerID).toBeNull();
  });

  test('cannot open the PSGC admin page', async ({ page }) => {
    const response = await page.goto('/admin/locations');
    expect(response?.status()).toBe(403);
  });
});

test.describe('finance', () => {
  test.beforeEach(async ({ page }) => signInAs(page, ACCOUNTS.finance));

  test('updates property tax status but cannot assign custodians', async ({ page }) => {
    await page.goto('/properties');
    await searchTable(page, 'landPropertiesTable', 'E2E Clean Lot');
    const row = tableRow(page, 'landPropertiesTable', 'E2E Clean Lot');
    await row.getByRole('button', { name: 'Actions' }).click();
    await expect(row.getByRole('link', { name: 'Assign Custodian' })).toHaveCount(0);
    await row.getByRole('link', { name: 'Update Tax Status' }).click();

    const modal = page.locator('#taxModal');
    await modal.locator('select[name=taxStatus]').selectOption('Paid (Current Year)');
    await modal.locator('textarea[name=conditionNotes]').fill('OR 778899');
    await modal.getByRole('button', { name: 'Update Status' }).click();
    await expectSuccessToast(page, 'Tax status updated.');

    const history: Array<{ actionType: string; recordedBy: string }> = await (
      await page.request.get('/properties/1/history')
    ).json();
    expect(history[0]).toMatchObject({ actionType: 'Tax Status Updated: Paid (Current Year)', recordedBy: ACCOUNTS.finance });
  });

  test('edits only the purchase price in the asset slideout', async ({ page }) => {
    await page.goto('/assets?openAsset=E2E-ASSET-DEPLOYED');
    const panel = page.locator('#itDetailOffcanvas');
    await expect(panel.locator('#editAssetTag')).toHaveValue('E2E-ASSET-DEPLOYED');
    await panel.locator('#enableEditBtn').click();

    await expect(panel.locator('#editPurchasePrice')).toBeEnabled();
    await expect(panel.locator('#editRemarks')).toBeDisabled();
    await expect(panel.locator('#editDeploymentStatus')).toBeDisabled();
    await expect(panel.locator('#assetDetailDocumentFiles')).toHaveCount(0); // no uploads

    // A new value every run, so a re-run without db:reset still records a change.
    const newPrice = 60000 + (Date.now() % 900) + 1;
    await panel.locator('#editPurchasePrice').fill(`${newPrice}.00`);
    await Promise.all([page.waitForEvent('load'), panel.locator('#saveEditBtn').click()]);

    const details = await (await page.request.get('/assets/E2E-ASSET-DEPLOYED')).json();
    expect(details.purchasePrice).toBe(newPrice);
    expect(details.currentOwnerID).toBe('E2E-001'); // untouched
    const history: Array<{ actionType: string; recordedBy: string; notes: string }> = await (
      await page.request.get('/api/assets/E2E-ASSET-DEPLOYED/history')
    ).json();
    expect(history[0]).toMatchObject({ actionType: 'Asset Cost Updated', recordedBy: ACCOUNTS.finance });
    expect(history[0].notes).toContain(`-> ${newPrice}.00`);
  });

  test('a status change smuggled into a save is ignored', async ({ page }) => {
    await page.goto('/assets');
    const response = await page.request.post('/assets/update', {
      headers: { ...(await csrfHeader(page)), 'Content-Type': 'application/json' },
      data: {
        assetTag: 'E2E-ASSET-STORAGE', catalogID: 1, purchasePrice: 65000,
        deploymentStatus: 'Deployed', currentOwnerID: 'E2E-001',
        maintenanceHealthStatus: 'Operational', lifecycleStatus: 'Active',
      },
    });
    expect(response.ok()).toBeTruthy();
    const details = await (await page.request.get('/assets/E2E-ASSET-STORAGE')).json();
    expect(details.deploymentStatus).toBe('In Storage');
    expect(details.currentOwnerID).toBeNull();
  });

  test('is refused assignments', async ({ page }) => {
    await page.goto('/fleet');
    const response = await page.request.post('/fleet/assign', {
      headers: { ...(await csrfHeader(page)), Accept: 'application/json' },
      form: { vehicleID: '1', employeeID: 'E2E-002' },
    });
    expect(response.status()).toBe(403);
    expect(await response.json()).toEqual({ error: 'Your role does not allow this action.' });
  });
});

test.describe('clerk', () => {
  test.beforeEach(async ({ page }) => signInAs(page, ACCOUNTS.clerk));

  test('runs workflows but cannot touch tax status or costs', async ({ page }) => {
    await expect(page.getByRole('button', { name: '+ Receive Asset' })).toBeVisible();

    await page.goto('/properties');
    await searchTable(page, 'buildingFacilitiesTable', 'E2E Overdue Building');
    const row = tableRow(page, 'buildingFacilitiesTable', 'E2E Overdue Building');
    await row.getByRole('button', { name: 'Actions' }).click();
    await expect(row.getByRole('link', { name: 'Assign Custodian' })).toBeVisible();
    await expect(row.getByRole('link', { name: 'Update Tax Status' })).toHaveCount(0);

    await submitFormAsBrowser(page, '/properties/update-tax', { propertyID: '3', taxStatus: 'Paid (Current Year)' });
    await page.waitForURL(/\/properties/);
    await expect(page.locator('#errorToast')).toContainText('Your role does not allow this action.');

    await page.goto('/assets?openAsset=E2E-ASSET-STORAGE');
    await page.locator('#enableEditBtn').click();
    await expect(page.locator('#editRemarks')).toBeEnabled();
    await expect(page.locator('#editPurchasePrice')).toBeDisabled();
  });

  test('cannot delete documents or import locations', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Manage PSGC Data' })).toHaveCount(0);
    expect((await page.goto('/admin/locations'))?.status()).toBe(403);

    await page.goto('/assets');
    const response = await page.request.delete('/documents/1', {
      headers: { ...(await csrfHeader(page)), 'X-Requested-With': 'XMLHttpRequest' },
    });
    expect(response.status()).toBe(403);
  });

  test('can still read vehicle history', async ({ page }) => {
    await page.goto('/fleet');
    await searchTable(page, 'fleetTable', 'E2E-1001');
    await chooseRowAction(tableRow(page, 'fleetTable', 'E2E-1001'), /History/);
    await expect(page.locator('#fleetHistoryModal')).toBeVisible();
  });
});
