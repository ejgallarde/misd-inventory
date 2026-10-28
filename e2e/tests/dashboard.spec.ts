import { test, expect } from '@playwright/test';
import { tableRow, trackBrowserErrors } from './support/app';

test.describe('dashboard hub', () => {
  test('exposes all three asset modules as tabs', async ({ page }) => {
    await page.goto('/');
    const tabs = page.locator('#assetTabs');
    await expect(tabs.getByRole('tab', { name: /IT Equipment/ })).toBeVisible();
    await expect(tabs.getByRole('tab', { name: /Fleet Management/ })).toBeVisible();
    await expect(tabs.getByRole('tab', { name: /Land Assets/ })).toBeVisible();
    await expect(tabs.getByRole('tab', { name: /Buildings & Facilities/ })).toBeVisible();
  });

  test('switching tabs shows each module portfolio', async ({ page }) => {
    await page.goto('/');
    const cases: Array<[RegExp, string]> = [
      [/Fleet Management/, 'Fleet Portfolio'],
      [/Land Assets/, 'Land Assets Portfolio'],
      [/Buildings & Facilities/, 'Buildings & Facilities Portfolio'],
      [/IT Equipment/, 'IT Asset Portfolio'],
    ];
    for (const [tab, heading] of cases) {
      await page.locator('#assetTabs').getByRole('tab', { name: tab }).click();
      await expect(page.getByRole('heading', { name: heading })).toBeVisible();
    }
  });

  for (const path of ['/', '/assets', '/fleet', '/properties', '/admin/locations']) {
    test(`${path} loads without browser errors`, async ({ page }) => {
      const errors = trackBrowserErrors(page);
      const response = await page.goto(path);
      expect(response?.status()).toBe(200);
      await page.waitForLoadState('networkidle');
      expect(errors).toEqual([]);
    });
  }
});

test.describe('"requires attention" lists', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('IT: a BER asset is listed', async ({ page }) => {
    await expect(tableRow(page, 'agingTable', 'E2E-ASSET-BER')).toHaveCount(1);
  });

  test('IT: a healthy asset is not listed', async ({ page }) => {
    await expect(tableRow(page, 'agingTable', 'E2E-ASSET-STORAGE')).toHaveCount(0);
  });

  test('IT: a missing / unaccounted asset is listed', async ({ page }) => {
    await expect(tableRow(page, 'agingTable', 'E2E-ASSET-MISSING')).toHaveCount(1);
  });

  test('Fleet: a vehicle past its useful life is listed', async ({ page }) => {
    await expect(tableRow(page, 'fleetActionRequiredTable', 'E2E-1003')).toHaveCount(1);
  });

  test('Fleet: a vehicle with a lapsed registration is listed', async ({ page }) => {
    await expect(tableRow(page, 'fleetActionRequiredTable', 'E2E-1002')).toHaveCount(1);
  });

  test('Buildings: a building with overdue tax is listed', async ({ page }) => {
    await expect(tableRow(page, 'buildingsFacilitiesActionRequiredTable', 'E2E Overdue Building')).toHaveCount(1);
  });

  test('Land: a lot under legal dispute is listed', async ({ page }) => {
    await expect(tableRow(page, 'landAssetsActionRequiredTable', 'E2E Disputed Lot')).toHaveCount(1);
  });

  test('Land: a clean lot is not listed', async ({ page }) => {
    await expect(tableRow(page, 'landAssetsActionRequiredTable', 'E2E Clean Lot')).toHaveCount(0);
  });
});
