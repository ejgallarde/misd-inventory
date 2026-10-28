import { test, expect } from '@playwright/test';
import { chooseRowAction, expectSuccessToast, searchTable, tableRow } from './support/app';

test.describe('real estate & property module', () => {
  test('splits lots and buildings into separate tables', async ({ page }) => {
    await page.goto('/properties');
    await expect(tableRow(page, 'landPropertiesTable', 'E2E Clean Lot')).toHaveCount(1);
    await expect(tableRow(page, 'landPropertiesTable', 'E2E Disputed Lot')).toHaveCount(1);
    await expect(tableRow(page, 'landPropertiesTable', 'E2E Overdue Building')).toHaveCount(0);
    await expect(tableRow(page, 'buildingFacilitiesTable', 'E2E Overdue Building')).toHaveCount(1);
  });

  test('detail slideout shows title, address and custodian', async ({ page }) => {
    await page.goto('/properties');
    await searchTable(page, 'landPropertiesTable', 'E2E Clean Lot');
    await tableRow(page, 'landPropertiesTable', 'E2E Clean Lot').locator('a.property-detail-link').first().click();

    const panel = page.locator('#propertyDetailOffcanvas');
    await expect(panel).toBeVisible();
    await expect(panel.locator('#propertyDetailName')).toContainText('E2E Clean Lot');
    await expect(panel.locator('#propertyDetailTitle')).toContainText('TCT-E2E-0001');
    await expect(panel.locator('#propertyDetailCity')).toContainText('Manila');
    await expect(panel.locator('#propertyDetailCustodian')).toContainText('Santos');
  });

  test('history modal lists seeded audit entries', async ({ page }) => {
    await page.goto('/properties');
    await searchTable(page, 'landPropertiesTable', 'E2E Clean Lot');
    await chooseRowAction(tableRow(page, 'landPropertiesTable', 'E2E Clean Lot'), /View Property/);
    const modal = page.locator('#propertyHistoryModal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('#propertyHistoryTable')).toContainText('Property Details Updated');
  });

  test('update tax status from the Actions menu', async ({ page }) => {
    await page.goto('/properties');
    await searchTable(page, 'buildingFacilitiesTable', 'E2E Overdue Building');
    await chooseRowAction(tableRow(page, 'buildingFacilitiesTable', 'E2E Overdue Building'), /Tax/);

    const modal = page.locator('#taxModal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('#taxPropID')).toHaveValue('3');
    await modal.locator('select[name=taxStatus]').selectOption('Pending');
    await modal.locator('textarea[name=conditionNotes]').fill('OR 123456 under review'); // required
    await modal.getByRole('button', { name: 'Update Status' }).click();

    await expectSuccessToast(page, 'Tax status updated.');
    const details = await page.request.get('/properties/3');
    expect((await details.json()).propertyTaxStatus).toBe('Pending');
  });

  test('tax update is recorded under the signed-in user', async ({ page }) => {
    const history = await page.request.get('/properties/3/history');
    const entries: Array<{ actionType: string; recordedBy: string }> = await history.json();
    const taxEntry = entries.find((entry) => entry.actionType.startsWith('Tax Status Updated'));
    test.skip(!taxEntry, 'run after "update tax status" has recorded an entry');
    expect(taxEntry!.recordedBy).toBe('admin@phlpost.gov.ph');
  });

  test('province -> city -> barangay lookup API cascades', async ({ request }) => {
    const provinces = await (await request.get('/api/locations/provinces')).json();
    expect(JSON.stringify(provinces)).toContain('Metro Manila');
    const cities = await (await request.get('/api/locations/cities?provinceCode=1339')).json();
    expect(JSON.stringify(cities)).toContain('Manila');
    const barangays = await (await request.get('/api/locations/barangays?cityMunicipalityCode=133901')).json();
    expect(JSON.stringify(barangays)).toContain('Barangay 659');
  });

  test('unknown property id returns 404 from the detail API', async ({ request }) => {
    expect((await request.get('/properties/999999')).status()).toBe(404);
  });
});
