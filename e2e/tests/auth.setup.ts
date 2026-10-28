import { test as setup, expect } from '@playwright/test';
import { DEMO_USER } from './support/app';

setup('sign in once and save the session', async ({ page }) => {
  await page.goto('/login');
  await page.locator('#username').fill(DEMO_USER.email);
  await page.locator('#password').fill(DEMO_USER.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('heading', { name: 'Enterprise Asset Overview' })).toBeVisible();
  await page.context().storageState({ path: '.auth/user.json' });
});
