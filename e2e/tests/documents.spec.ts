import { test, expect } from '@playwright/test';

/**
 * Document attachments end to end in the default storage.mode=filesystem
 * (files land under e2e/.storage). Runs as admin, the only role that may
 * delete. MinIO mode is covered separately by DocumentStorageServiceMinioTest.
 */
test('attach, view and delete a document from the asset slideout', async ({ page }) => {
  const fileName = `receipt-${Date.now()}.pdf`;
  const content = '%PDF-1.4\n% e2e receipt\n';

  await page.goto('/assets?openAsset=E2E-ASSET-STORAGE');
  const panel = page.locator('#itDetailOffcanvas');
  await expect(panel.locator('#editAssetTag')).toHaveValue('E2E-ASSET-STORAGE');
  await panel.locator('#enableEditBtn').click();

  await panel.locator('#assetDetailDocumentFiles').setInputFiles({
    name: fileName,
    mimeType: 'application/pdf',
    buffer: Buffer.from(content),
  });
  await panel.locator('#assetDetailDocumentPreview select[name=documentCategories]')
    .selectOption('Official Receipt / Invoice');
  await Promise.all([page.waitForEvent('load'), panel.locator('#saveEditBtn').click()]);

  // Stored, listed under the asset, and served back byte for byte.
  const documents: Array<{ documentId: number; fileName: string; documentCategory: string; uploadedBy: string }> =
    await (await page.request.get('/documents/list?refType=IT_EQUIPMENT&refId=E2E-ASSET-STORAGE')).json();
  const uploaded = documents.find((doc) => doc.fileName === fileName);
  expect(uploaded).toMatchObject({ documentCategory: 'Official Receipt / Invoice', uploadedBy: 'admin@phlpost.gov.ph' });
  const view = await page.request.get(`/documents/${uploaded!.documentId}/view`);
  expect(view.headers()['content-type']).toContain('application/pdf');
  expect(await view.text()).toBe(content);

  // Listed in the slideout; deleting asks for confirmation in a modal, then removes it.
  await page.goto('/assets?openAsset=E2E-ASSET-STORAGE');
  const row = page.locator('#assetDocumentsTableBody tr', { hasText: fileName });
  await expect(row).toBeVisible();
  await row.locator('.js-doc-delete').click();
  const confirm = page.locator('.modal.show', { hasText: 'Delete Attachment' });
  await confirm.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(row).toHaveCount(0);
  expect((await page.request.get(`/documents/${uploaded!.documentId}/view`)).status()).toBe(404);
});

test('a file type outside the allowed list is refused', async ({ page }) => {
  await page.goto('/assets');
  const token = await page.locator('input[name=_csrf]').first().inputValue();
  const response = await page.request.post('/documents/add', {
    headers: { 'X-CSRF-TOKEN': token },
    multipart: {
      refType: 'IT_EQUIPMENT',
      refId: 'E2E-ASSET-STORAGE',
      documentCategories: 'Photographs',
      documentFiles: { name: 'payload.exe', mimeType: 'application/octet-stream', buffer: Buffer.from('MZ') },
    },
  });
  expect(response.status()).toBe(400);
  expect((await response.json()).error).toContain('not an allowed type');
});
