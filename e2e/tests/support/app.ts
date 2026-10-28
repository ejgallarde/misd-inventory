import { expect, type Locator, type Page } from '@playwright/test';

export const DEMO_USER = {
  email: process.env.MISD_DEMO_USER_EMAIL ?? 'admin@phlpost.gov.ph',
  password: process.env.MISD_DEMO_USER_PASSWORD ?? 'e2e-password',
};

/** Collects uncaught page errors and console errors so a test can assert there were none. */
export function trackBrowserErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`));
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console: ${message.text()}`);
  });
  return errors;
}

/** The DataTable row whose text contains `text`. Tables are server-rendered, so every row is in the DOM. */
export function tableRow(page: Page, tableId: string, text: string): Locator {
  return page.locator(`#${tableId} tbody tr`).filter({ hasText: text });
}

/** Searches a DataTable through its own search box so the row is on the visible page. */
export async function searchTable(page: Page, tableId: string, text: string): Promise<void> {
  await page.locator(`#${tableId}_filter input[type=search]`).fill(text);
}

/** Opens a row's Actions dropdown and clicks one of its items. */
export async function chooseRowAction(row: Locator, action: string | RegExp): Promise<void> {
  await row.getByRole('button', { name: 'Actions' }).click();
  // Menu labels are wrapped across lines in the templates; accessible names are
  // whitespace-normalised, getByText(regex) is not. Items are links or, for
  // one-click fleet actions, submit buttons.
  const menu = row.locator('.dropdown-menu');
  await menu.getByRole('link', { name: action }).or(menu.getByRole('button', { name: action })).click();
}

/** Picks a person in a Select2 personnel search inside an open modal. */
export async function pickPersonnel(modal: Locator, search: string, optionText: string | RegExp): Promise<void> {
  await modal.locator('.select2-selection').click();
  await modal.page().locator('.select2-container--open .select2-search__field').fill(search);
  await modal.page().locator('.select2-results__option', { hasText: optionText }).click();
}

export async function expectSuccessToast(page: Page, text: string | RegExp): Promise<void> {
  await expect(page.locator('#successToast')).toContainText(text);
}

/**
 * Submits a real browser form POST (with the page's CSRF token) to an action,
 * the way a stale page or a hand-crafted request would. Used to prove the
 * server enforces rules the UI only hides.
 */
export async function submitFormAsBrowser(page: Page, action: string, fields: Record<string, string>): Promise<void> {
  const csrf = await page.locator('input[name=_csrf]').first().inputValue();
  await page.evaluate(
    ({ action, values }) => {
      const form = document.createElement('form');
      form.method = 'post';
      form.action = action;
      for (const [name, value] of Object.entries(values)) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
      }
      document.body.appendChild(form);
      form.submit();
    },
    { action, values: { _csrf: csrf, ...fields } },
  );
}

/** Unique suffix so tests that create records can be re-run without a database reset. */
export function uniqueSuffix(): string {
  return Date.now().toString(36).toUpperCase();
}
