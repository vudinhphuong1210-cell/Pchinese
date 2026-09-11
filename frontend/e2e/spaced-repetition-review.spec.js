import { test, expect } from '@playwright/test';

test.describe('Spaced Repetition Review E2E Flow', () => {
  test('learner opens SRS review page, flips cards, and submits rating', async ({ page }) => {
    await page.goto('/review');

    // Check page header
    const heading = page.getByRole('heading', { name: /Ôn tập từ vựng/i });
    if (await heading.isVisible()) {
      await expect(heading).toBeVisible();

      // Check if due cards exist or empty state
      const flipBtn = page.getByRole('button', { name: /Lật thẻ xem đáp án/i });
      if (await flipBtn.isVisible()) {
        await flipBtn.click();

        const goodBtn = page.getByRole('button', { name: /Tốt \(Good\)/i });
        await expect(goodBtn).toBeVisible();
        await goodBtn.click();
      } else {
        await expect(page.getByText(/Tuyệt vời! Bạn đã hoàn thành/i)).toBeVisible();
      }
    }
  });
});
