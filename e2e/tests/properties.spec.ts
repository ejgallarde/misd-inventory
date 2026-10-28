import { test, expect } from '@playwright/test';
import { chooseRowAction, expectSuccessToast, pickPersonnel, searchTable, tableRow } from './support/app';

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

  test('register a land asset using the province -> city -> barangay pickers', async ({ page }) => {
    const id = Date.now().toString(36).toUpperCase();
    await page.goto('/');
    await page.locator('#assetTabs').getByRole('tab', { name: /Land Assets/ }).click();
    await page.locator('#addLandAssetBtn').click();

    const form = page.locator('#addPropertyForm');
    await expect(form).toBeVisible();
    await form.locator('[name=propertyName]').fill(`E2E Cascade Lot ${id}`);
    await form.locator('#propertyAreaInput').selectOption('Central Office');
    await form.locator('#propertyTitleNumberInput').fill(`TCT-C-${id}`);
    await form.locator('#propertyTaxDeclarationNumberInput').fill(`TD-C-${id}`);
    await form.locator('#propertySurveyPlanNumberInput').fill(`PSD-C-${id}`);
    await form.locator('[name=propertyDetails]').fill('Registered by Playwright');
    await form.locator('[name=addressLine1]').fill('Plaza Lawton');
    await form.locator('[name=addressLine2]').fill('Ermita');

    // Each choice unlocks and fills the next level.
    await expect(form.locator('#propertyCity')).toBeDisabled();
    await form.locator('#propertyProvince').selectOption('Metro Manila');
    await expect(form.locator('#propertyCity')).toBeEnabled();
    await expect(form.locator('#propertyBarangay')).toBeDisabled();
    await form.locator('#propertyCity').selectOption('Manila');
    await expect(form.locator('#propertyBarangay')).toBeEnabled();
    await form.locator('#propertyBarangay').selectOption('Barangay 659');
    // The barangay's PSGC ZIP code is filled in.
    await expect(form.locator('#propertyZipCode')).toHaveValue('1000');

    // Changing province resets the lower levels.
    await form.locator('#propertyProvince').selectOption('Pampanga');
    await expect(form.locator('#propertyBarangay')).toBeDisabled();
    await expect(form.locator('#propertyCity option')).toContainText(['Select city/municipality', 'San Fernando']);
    await form.locator('#propertyProvince').selectOption('Metro Manila');
    await expect(form.locator('#propertyCity')).toBeEnabled();
    await form.locator('#propertyCity').selectOption('Manila');
    await expect(form.locator('#propertyBarangay')).toBeEnabled();
    await form.locator('#propertyBarangay').selectOption('Barangay 659');

    await form.locator('#propertyLegalTitlingStatusInput').selectOption('Titled');
    await form.locator('select[name=operationalStatus]').selectOption('Active/In Use');
    await form.locator('#propertyConditionStatusInput').selectOption('Usable - No Known Issues');
    await form.locator('#propertyLotAreaInput').fill('640');
    await form.locator('#addPropertySubmitBtn').click();

    await expect(page.locator('#successToast')).toContainText('Property added to registry.');
    await page.goto('/properties');
    const row = page.locator('#landPropertiesTable tbody tr', { hasText: `E2E Cascade Lot ${id}` });
    await expect(row).toContainText('Manila');
  });

  test('a Job Order employee cannot be made custodian (accountable)', async ({ page }) => {
    await page.goto('/properties');
    await searchTable(page, 'buildingFacilitiesTable', 'E2E Overdue Building');
    await chooseRowAction(tableRow(page, 'buildingFacilitiesTable', 'E2E Overdue Building'), 'Assign Custodian');
    const modal = page.locator('#custodianModal');
    await pickPersonnel(modal, 'Cruz', /Cruz, Ana .*Job Order/);
    await modal.locator('textarea[name=conditionNotes]').fill('Designation memo 12'); // required
    await modal.locator('button[type=submit]').click();
    await expect(page.locator('#errorToast')).toContainText('is Job Order and cannot be the accountable owner');
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
