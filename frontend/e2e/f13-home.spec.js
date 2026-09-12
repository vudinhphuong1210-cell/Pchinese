import { test, expect } from '@playwright/test';

const lesson = {
  id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
  title: 'Chào hỏi tự nhiên',
  slug: 'chao-hoi-tu-nhien',
  description: 'Hội thoại mở đầu dành cho HSK 1',
  hskLevel: 1,
  accessLevel: 'FREE',
  estimatedDurationSeconds: 95,
  publishedSegmentCount: 8,
};

async function mockPublicHome(page) {
  await page.route('**/api/v1/auth/refresh', (route) => route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ success: false, data: null, error: { code: 'REFRESH_TOKEN_INVALID', message: 'Guest' }, meta: {} }) }));
  await page.route('**/api/v1/topics?**', (route) => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ success: true, data: { content: [{ id: 'topic-1', title: 'Đời sống', publishedLessonCount: 1 }], totalElements: 1 }, error: null, meta: {} }) }));
  await page.route('**/api/v1/lessons?**', (route) => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ success: true, data: { content: [lesson], totalElements: 1 }, error: null, meta: {} }) }));
}

test.describe('F13 Home experience', () => {
  test('renders the complete guest journey at mobile width without horizontal page overflow', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 800 });
    await mockPublicHome(page);
    await page.goto('/');

    await expect(page.getByTestId('home-page')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Học tiếng Trung thú vị và hiệu quả hơn mỗi ngày' })).toBeVisible();
    await expect(page.getByTestId(`home-lesson-${lesson.id}`)).toBeVisible();
    await expect(page.getByTestId('daily-streak-widget')).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  });

  test('switches among all ten themes while preserving the home page', async ({ page }) => {
    await mockPublicHome(page);
    await page.goto('/');

    const themes = ['light', 'dark', 'light-purple', 'dark-purple', 'light-pink', 'dark-pink', 'light-blue', 'dark-blue', 'light-red', 'dark-red'];
    for (const theme of themes) {
      await page.getByTestId('theme-picker-trigger').click();
      await page.getByTestId(`theme-option-${theme}`).click();
      await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
      await expect(page.getByTestId('home-page')).toBeVisible();
    }
  });
});
