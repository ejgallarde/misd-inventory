import { test, expect } from '@playwright/test';

/**
 * Request-level checks of behaviour the browser UI depends on but cannot show
 * directly. Uses the signed-in session from auth.setup.ts.
 */
test.describe('API contracts and security', () => {
  test('state-changing requests without a CSRF token are refused', async ({ request }) => {
    const response = await request.post('/assets/update', {
      data: { assetTag: 'E2E-ASSET-STORAGE', remarks: 'no csrf' },
    });
    expect(response.status()).toBe(403);
  });

  test('updating an unknown asset returns a JSON error, not a 500 page', async ({ page }) => {
    await page.goto('/assets');
    const token = await page.locator('input[name=_csrf]').first().inputValue();
    const response = await page.request.post('/assets/update', {
      headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
      data: { assetTag: 'NO-SUCH-ASSET' },
    });
    expect(response.status()).toBe(404);
    expect(await response.json()).toEqual({ error: 'Asset not found.' });
  });

  test('an asset status outside the configured dropdown is rejected', async ({ page }) => {
    await page.goto('/assets');
    const token = await page.locator('input[name=_csrf]').first().inputValue();
    const response = await page.request.post('/assets/update', {
      headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
      data: {
        assetTag: 'E2E-ASSET-STORAGE',
        catalogID: 1,
        deploymentStatus: 'Anywhere',
        maintenanceHealthStatus: 'Operational',
        lifecycleStatus: 'Active',
      },
    });
    expect(response.status()).toBe(400);
    expect((await response.json()).error).toContain('Unknown deployment status');
  });

  test('document listing rejects an unknown reference type', async ({ request }) => {
    const response = await request.get('/documents/list?refType=&refId=1');
    expect(response.status()).toBe(400);
  });

  test('personnel search returns Select2-shaped results', async ({ request }) => {
    const body = await (await request.get('/api/personnel/search?q=San')).json();
    expect(body.results[0]).toMatchObject({ id: 'E2E-001' });
    expect(body.pagination).toHaveProperty('more');
  });

  test('technician search is filtered by job title', async ({ request }) => {
    const body = await (
      await request.get('/api/personnel/search?q=a&jobTitle=Computer%20Maintenance%20Technologist')
    ).json();
    const ids = body.results.map((r: { id: string }) => r.id);
    expect(ids).toContain('E2E-TECH');
    expect(ids).not.toContain('E2E-002'); // "Cruz, Ana" matches "a" but is a driver
  });

  test('technician picker lists technicians before anything is typed', async ({ request }) => {
    // The MISD-maintenance Select2 uses minimumInputLength 0, so it opens by
    // requesting q="". WebBindingAdvice trims that to null and the JPQL
    // CONCAT('%', NULL, '%') matches nothing.
    const body = await (
      await request.get('/api/personnel/search?q=&jobTitle=Computer%20Maintenance%20Technologist')
    ).json();
    expect(body.results.map((r: { id: string }) => r.id)).toContain('E2E-TECH');
  });
});
