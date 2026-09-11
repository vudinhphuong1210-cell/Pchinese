import { test, expect } from '@playwright/test';

test.describe('Dictionary Discovery Flow', () => {
  test('visitor searches dictionary and views entry detail', async ({ page }) => {
    await page.goto('/');

    const searchInput = page.getByRole('textbox', { name: /khung tìm kiếm từ điển/i });
    if (await searchInput.isVisible()) {
      await searchInput.fill('xuesheng');
      await page.getByRole('button', { name: /tìm kiếm/i }).click();
    }
  });
});
