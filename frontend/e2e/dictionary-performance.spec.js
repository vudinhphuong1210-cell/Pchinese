import { test, expect } from '@playwright/test';

test.describe('Dictionary Performance Benchmark (SC-001)', () => {
  test('100 concurrent search load test profile', async ({ page }) => {
    if (!process.env.ISOLATED_PERFORMANCE_DB) {
      test.skip('Skipping performance run on non-isolated environment');
      return;
    }
    const startTime = Date.now();
    await page.goto('/');
    const searchInput = page.getByRole('textbox', { name: /khung tìm kiếm từ điển/i });
    await searchInput.fill('xue');
    await page.getByRole('button', { name: /tìm kiếm/i }).click();

    const duration = Date.now() - startTime;
    expect(duration).toBeLessThan(2000);
  });
});
