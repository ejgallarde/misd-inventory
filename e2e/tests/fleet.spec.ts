import { test, expect, type Locator, type Page } from '@playwright/test';
import {
  DEMO_USER,
  chooseRowAction,
  expectSuccessToast,
  pickPersonnel,
  searchTable,
  submitFormAsBrowser,
  tableRow,
  uniqueSuffix,
} from './support/app';

async function returnToMotorpool(page: Page, row: Locator): Promise<void> {
  await chooseRowAction(row, 'Return to Motorpool');
  const modal = page.locator('#returnModal');
  await expect(modal.locator('#returnVehicleID')).toHaveValue('4');
  await modal.locator('textarea[name=conditionNotes]').fill('Returned by Playwright'); // required
  await modal.getByRole('button', { name: 'Confirm Return' }).click();
  await expectSuccessToast(page, 'Vehicle returned to motorpool.');
}

test.describe('fleet module', () => {
  test('lists seeded vehicles', async ({ page }) => {
    await page.goto('/fleet');
    await expect(page.getByRole('heading', { name: 'Fleet', exact: true })).toBeVisible();
    for (const plate of ['E2E-1001', 'E2E-1002', 'E2E-1003', 'E2E-1004']) {
      await expect(tableRow(page, 'fleetTable', plate)).toHaveCount(1);
    }
  });

  test('detail slideout shows identifiers and flags a lapsed registration', async ({ page }) => {
    await page.goto('/fleet');
    await searchTable(page, 'fleetTable', 'E2E-1002');
    await tableRow(page, 'fleetTable', 'E2E-1002').locator('a.fleet-detail-link').first().click();

    const panel = page.locator('#fleetDetailOffcanvas');
    await expect(panel).toBeVisible();
    await expect(panel.locator('#fleetDetailPlate')).toContainText('E2E-1002');
    await expect(panel.locator('#fleetDetailMake')).toContainText('Honda');
    await expect(panel.locator('#fleetDetailEngineNumber')).toContainText('ENG-E2E-1002');
    await expect(panel.locator('#fleetDetailRegExpiredNote')).toBeVisible();
  });

  test('history modal lists seeded audit entries', async ({ page }) => {
    await page.goto('/fleet');
    await searchTable(page, 'fleetTable', 'E2E-1001');
    await chooseRowAction(tableRow(page, 'fleetTable', 'E2E-1001'), /History/);
    const modal = page.locator('#fleetHistoryModal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('#fleetHistoryTable')).toContainText('Vehicle Details Updated');
  });

  test('register a vehicle from the dashboard, with a grouped cost value', async ({ page }) => {
    const id = uniqueSuffix();
    const plate = `E2E-R${id}`.slice(0, 20);
    await page.goto('/');
    await page.locator('#assetTabs').getByRole('tab', { name: /Fleet Management/ }).click();
    await page.getByRole('button', { name: '+ Register Vehicle' }).click();

    const form = page.locator('#addVehicleForm');
    await expect(form).toBeVisible();
    await form.locator('[name=plateNumber]').fill(plate);
    await form.locator('[name=vehicleType]').selectOption('Van');
    await form.locator('[name=make]').fill('Isuzu');
    await form.locator('[name=model]').fill('Traviz');
    await form.locator('[name=manufactureYear]').selectOption('2024');
    await form.locator('[name=fuelType]').selectOption('Diesel');
    await form.locator('[name=engineNumber]').fill(`ENG-${id}`);
    await form.locator('[name=chassisNumberVIN]').fill(`VIN-${id}`);
    await form.locator('[name=adminLegaltionalStatus]').selectOption('Registered');
    await form.locator('[name=operationalStatus]').selectOption('Available/Idle');
    await form.locator('[name=maintenanceStatus]').selectOption('Roadworthy');
    await form.locator('[name=acquisitionYear]').selectOption('2024');
    await form.locator('[name=cost]').fill('1,500,000.00');
    await form.locator('[name=registrationExpiry]').fill('2027-12-31');
    await form.locator('[name=insuranceExpiry]').fill('2027-12-31');
    await form.getByRole('button', { name: 'Register Vehicle' }).click();

    await expectSuccessToast(page, 'Vehicle registered');
    await page.goto('/fleet');
    await searchTable(page, 'fleetTable', plate);
    await expect(tableRow(page, 'fleetTable', plate)).toHaveCount(1);
  });

  test('assign a driver, then return the vehicle to the motorpool', async ({ page }) => {
    await page.goto('/fleet');
    await searchTable(page, 'fleetTable', 'E2E-1004');
    let row = tableRow(page, 'fleetTable', 'E2E-1004');

    // A previous run without a db:reset may have left it dispatched.
    if (await row.getByText('Operational: Dispatched').count()) {
      await returnToMotorpool(page, row);
      await searchTable(page, 'fleetTable', 'E2E-1004');
      row = tableRow(page, 'fleetTable', 'E2E-1004');
    }

    await chooseRowAction(row, 'Assign Driver (Accountable Owner)');
    const assign = page.locator('#assignModal');
    await expect(assign.locator('#assignVehicleID')).toHaveValue('4');
    await pickPersonnel(assign, 'Cruz', 'Cruz, Ana');
    await assign.getByRole('button', { name: 'Complete Assignment' }).click();
    await expectSuccessToast(page, 'Vehicle assigned');

    await searchTable(page, 'fleetTable', 'E2E-1004');
    row = tableRow(page, 'fleetTable', 'E2E-1004');
    await expect(row).toContainText('Cruz, Ana');

    await returnToMotorpool(page, row);
    await searchTable(page, 'fleetTable', 'E2E-1004');
    await expect(tableRow(page, 'fleetTable', 'E2E-1004')).toContainText('Operational: Available/Idle');

    // Both steps are attributed to the signed-in user (previously "SYSTEM"/"MOTORPOOL" or not at all).
    const history: Array<{ logType: string; actionType: string; recordedBy: string }> = await (
      await page.request.get('/fleet/4/history')
    ).json();
    for (const action of ['Vehicle Checkout', 'Vehicle Returned']) {
      const entry = history.find((e) => e.logType === 'Lifecycle' && e.actionType === action);
      expect(entry?.recordedBy, action).toBe(DEMO_USER.email);
    }
  });

  test('a stolen vehicle cannot be dispatched even by a direct request', async ({ page }) => {
    await page.goto('/fleet');
    await submitFormAsBrowser(page, '/fleet/assign', { vehicleID: '5', employeeID: 'E2E-002' });
    await page.waitForURL(/\/fleet$/);

    await expect(page.locator('#errorToast')).toContainText('Cannot assign a driver: vehicle E2E-1005');
    const details = await (await page.request.get('/fleet/5')).json();
    expect(details.operationalStatus).toBe('Stolen');
    expect(details.assignedDriverID).toBe('');
  });

  test('a stolen vehicle offers no dispatch actions', async ({ page }) => {
    await page.goto('/fleet');
    await searchTable(page, 'fleetTable', 'E2E-1005');
    const row = tableRow(page, 'fleetTable', 'E2E-1005');
    await row.getByRole('button', { name: 'Actions' }).click();
    await expect(row.getByRole('link', { name: 'Assign Driver (Accountable Owner)' })).toHaveCount(0);
  });

  test('the "problematic" card filter lists the same vehicles as the dashboard', async ({ page }) => {
    // Vehicles the server's attention list must contain, from the seed.
    await page.goto('/');
    const attention = await page.locator('#fleetActionRequiredTable tbody tr').allInnerTexts();
    for (const plate of ['E2E-1002', 'E2E-1003', 'E2E-1005']) {
      expect(attention.join('\n'), `dashboard lists ${plate}`).toContain(plate);
    }

    // The /fleet page filter applied client-side must agree.
    await page.goto('/fleet?filter=problematic');
    await page.locator('#fleetTable_wrapper select').first().selectOption('-1'); // show all rows
    const rows = page.locator('#fleetTable tbody tr');
    await expect(rows.filter({ hasText: 'E2E-1002' })).toHaveCount(1); // registration lapsed
    await expect(rows.filter({ hasText: 'E2E-1003' })).toHaveCount(1); // past useful life
    await expect(rows.filter({ hasText: 'E2E-1005' })).toHaveCount(1); // stolen
    await expect(rows.filter({ hasText: 'E2E-1001' })).toHaveCount(0); // healthy
  });

  test('the legal-issues card filter includes lapsed registrations', async ({ page }) => {
    await page.goto('/fleet?filter=admin-legal-issues');
    const rows = page.locator('#fleetTable tbody tr');
    await expect(rows.filter({ hasText: 'E2E-1002' })).toHaveCount(1);
    await expect(rows.filter({ hasText: 'E2E-1005' })).toHaveCount(1); // Under Investigation
    await expect(rows.filter({ hasText: 'E2E-1001' })).toHaveCount(0);
  });

  test('unknown vehicle id returns 404 from the detail API', async ({ request }) => {
    const response = await request.get('/fleet/999999');
    expect(response.status()).toBe(404);
  });
});
