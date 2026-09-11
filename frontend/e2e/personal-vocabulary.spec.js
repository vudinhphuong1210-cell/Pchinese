import { test, expect } from '@playwright/test';

test.describe('Personal Vocabulary E2E Journey', () => {
  test('learner saves word, updates note, and manages vocabulary capacity', async ({ page }) => {
    await page.goto('/');
  });
});
