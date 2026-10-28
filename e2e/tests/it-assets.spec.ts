import { test, expect } from '@playwright/test';
import {
  chooseRowAction,
  expectSuccessToast,
  pickPersonnel,
  searchTable,
  submitFormAsBrowser,
  tableRow,
  uniqueSuffix,
} from './support/app';

test.describe('IT assets module', () => {
  test('lists seeded assets with owner and status badges', async ({ page }) => {
    await page.goto('/assets');
    await expect(page.getByRole('heading', { name: 'IT Assets', exact: true })).toBeVisible();

    await searchTable(page, 'assetsTable', 'E2E-ASSET-DEPLOYED');
    const row = tableRow(page, 'assetsTable', 'E2E-ASSET-DEPLOYED');
    await expect(row).toContainText('Santos, Juan');
    await expect(row).toContainText('Reyes, Maria'); // manager column
    await expect(row).toContainText('Deployment: Deployed');
    await expect(row).toContainText('Lifecycle: Active');
  });

  test('detail slideout shows catalog, assignee and valuation', async ({ page }) => {
    await page.goto('/assets');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-DEPLOYED');
    await tableRow(page, 'assetsTable', 'E2E-ASSET-DEPLOYED').locator('a.asset-detail-link').click();

    const panel = page.locator('#itDetailOffcanvas');
    await expect(panel).toBeVisible();
    await expect(panel.locator('#editAssetTag')).toHaveValue('E2E-ASSET-DEPLOYED');
    await expect(panel.locator('#editSerialNumber')).toHaveValue('SN-E2E-0002');
    await expect(panel.locator('#assetCatalogSummary')).toContainText('Latitude 5440');
    await expect(panel.locator('#assetAssigneeSummary')).toContainText('Santos');
    await expect(panel.locator('#assetCurrentValuation')).not.toHaveText('');
    // Read-only until Edit is pressed.
    await expect(panel.locator('#editRemarks')).toBeDisabled();
  });

  test('openAsset deep link opens the slideout', async ({ page }) => {
    await page.goto('/assets?openAsset=E2E-ASSET-STORAGE');
    await expect(page.locator('#itDetailOffcanvas')).toBeVisible();
    await expect(page.locator('#editAssetTag')).toHaveValue('E2E-ASSET-STORAGE');
  });

  test('history modal lists seeded audit entries', async ({ page }) => {
    await page.goto('/assets');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-DEPLOYED');
    await chooseRowAction(tableRow(page, 'assetsTable', 'E2E-ASSET-DEPLOYED'), 'View Asset History');

    const modal = page.locator('#assetHistoryModal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('#assetHistoryTable')).toContainText('Asset Received');
    await expect(modal.locator('#assetHistoryTable')).toContainText('Checkout');
  });

  test('editing remarks in the slideout persists', async ({ page }) => {
    const remark = `E2E remark ${uniqueSuffix()}`;
    await page.goto('/assets?openAsset=E2E-ASSET-STORAGE');
    const panel = page.locator('#itDetailOffcanvas');
    await expect(panel.locator('#editAssetTag')).toHaveValue('E2E-ASSET-STORAGE');

    await panel.locator('#enableEditBtn').click();
    await panel.locator('#editRemarks').fill(remark);
    await Promise.all([page.waitForEvent('load'), panel.locator('#saveEditBtn').click()]);

    const details = await page.request.get('/assets/E2E-ASSET-STORAGE');
    expect((await details.json()).remarks).toBe(remark);
  });

  test('assign an in-storage asset to an employee', async ({ page }) => {
    await page.goto('/assets');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-ASSIGN');
    const row = tableRow(page, 'assetsTable', 'E2E-ASSET-ASSIGN');
    // "Assign Asset" on a fresh seed, "Re-assign User" on a re-run.
    await chooseRowAction(row, /Assign Asset|Re-assign User/);

    const modal = page.locator('#assignModal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('#assignAssetTagInput')).toHaveValue('E2E-ASSET-ASSIGN');
    await pickPersonnel(modal, 'Cruz', 'Cruz, Ana');
    await modal.locator('textarea[name=conditionNotes]').fill('Issued by Playwright');
    await modal.getByRole('button', { name: 'Complete Assignment' }).click();

    await expectSuccessToast(page, 'Asset assigned successfully.');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-ASSIGN');
    await expect(tableRow(page, 'assetsTable', 'E2E-ASSET-ASSIGN')).toContainText('Cruz, Ana');
    await expect(tableRow(page, 'assetsTable', 'E2E-ASSET-ASSIGN')).toContainText('Deployment: Deployed');
  });

  test('BER assets offer no deployment actions', async ({ page }) => {
    await page.goto('/assets');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-BER');
    const row = tableRow(page, 'assetsTable', 'E2E-ASSET-BER');
    await row.getByRole('button', { name: 'Actions' }).click();
    const menu = row.locator('.dropdown-menu');
    await expect(menu.getByText('View Asset History')).toBeVisible();
    await expect(menu.getByText(/Assign Asset|Re-assign User/)).toHaveCount(0);
  });

  test('receive a new asset from the dashboard', async ({ page }) => {
    const tag = `E2E-RCV-${uniqueSuffix()}`;
    await page.goto('/');
    await page.getByRole('button', { name: '+ Receive Asset' }).click();

    const panel = page.locator('#receiveAssetOffcanvas');
    await expect(panel).toBeVisible();
    await panel.locator('#receiveCatalogSelect').selectOption('1'); // Dell Latitude 5440 in the seed
    await panel.locator('#receiveAssetTag').fill(tag);
    await panel.locator('#receiveSerialNumber').fill(`SN-${tag}`);
    await panel.locator('input[name=purchasePrice]').fill('55000');
    await panel.getByRole('button', { name: 'Receive into Storage' }).click();

    await expectSuccessToast(page, 'Successfully received 1 asset(s) into storage.');
    const details = await page.request.get(`/assets/${tag}`);
    expect(details.ok()).toBeTruthy();
    expect((await details.json()).deploymentStatus).toBe('In Storage');
  });

  test('technician picker lists technicians as soon as it opens', async ({ page }) => {
    await page.goto('/assets');
    await searchTable(page, 'assetsTable', 'E2E-ASSET-STORAGE');
    await chooseRowAction(tableRow(page, 'assetsTable', 'E2E-ASSET-STORAGE'), 'For MISD Maintenance');

    const modal = page.locator('#misdMaintenanceModal');
    await modal.locator('.ts-control').click();
    // Nothing typed: the list must already hold the seeded technician, and only technicians.
    await expect(modal.locator('.ts-dropdown .option', { hasText: 'Garcia, Leo' })).toBeVisible();
    await expect(modal.locator('.ts-dropdown .option', { hasText: 'Cruz, Ana' })).toHaveCount(0);
  });

  test('a failed workflow action tells the user why', async ({ page }) => {
    await page.goto('/assets');
    // A technician who is not one: the server rejects it with a flash error.
    await submitFormAsBrowser(page, '/assets/misd-maintenance', {
      assetTag: 'E2E-ASSET-STORAGE',
      technicianID: 'E2E-002',
    });
    await page.waitForURL(/\/assets/);

    await expect(page.locator('#errorToast')).toContainText(/must be a Computer Maintenance Technologist/);
  });

  test('a BER asset cannot be assigned even by a direct request', async ({ page }) => {
    await page.goto('/assets');
    await submitFormAsBrowser(page, '/assets/assign', { assetTag: 'E2E-ASSET-BER', employeeID: 'E2E-001' });
    await page.waitForURL(/\/assets/);

    await expect(page.locator('#errorToast')).toContainText('Cannot assign it: asset E2E-ASSET-BER');
    const details = await (await page.request.get('/assets/E2E-ASSET-BER')).json();
    expect(details.currentOwnerID).toBeNull();
  });
});
