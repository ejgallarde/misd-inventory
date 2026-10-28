import { test, expect } from '@playwright/test';
import { DEMO_USER } from './support/app';

test.describe('authentication', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('unauthenticated visitors are sent to the login page', async ({ page }) => {
    for (const path of ['/', '/assets', '/fleet', '/properties', '/admin/locations']) {
      await page.goto(path);
      await expect(page, `GET ${path}`).toHaveURL(/\/login$/);
    }
  });

  test('a wrong password is rejected', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#username').fill(DEMO_USER.email);
    await page.locator('#password').fill('definitely-wrong');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/login\?error/);
  });

  test('sign in then sign out', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#username').fill(DEMO_USER.email);
    await page.locator('#password').fill(DEMO_USER.password);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByRole('heading', { name: 'Enterprise Asset Overview' })).toBeVisible();

    // Spring Security requires logout to be a CSRF-protected POST.
    const token = await page.locator('input[name=_csrf]').first().inputValue();
    const response = await page.request.post('/logout', { form: { _csrf: token }, maxRedirects: 0 });
    expect(response.status()).toBe(302);
    expect(response.headers()['location']).toContain('/login?logout');

    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('the login form carries a single CSRF field', async ({ page }) => {
    await page.goto('/login');
    // login.html declares the hidden _csrf input by hand while Thymeleaf also
    // injects one into every th:action form, so the field is posted twice.
    test.fail(true, 'BUG-UI-03: login.html renders two _csrf hidden inputs');
    await expect(page.locator('form input[name=_csrf]')).toHaveCount(1);
  });
});
