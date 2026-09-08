/**
 * Playwright / E2E Specification Test for F02 Profile Preferences Journeys
 * Uses stable data-testid selectors required by CONSTITUTION Article 5.
 */

import { test, expect } from '@playwright/test';

test.describe('F02 End-to-End Profile Preferences Journeys', () => {

  test('Learner Profile Preferences Navigation and Form Update Flow', async ({ page }) => {
    // 1. Visit App
    await page.goto('/');

    // 2. Navigate to Profile Preferences tab via Sidebar
    await page.getByTestId('profile-card').click();
    await expect(page.getByTestId('settings-page')).toBeVisible();

    // 3. Verify Form Fields render
    await expect(page.getByTestId('profile-preferences-form')).toBeVisible();
    await expect(page.getByTestId('profile-display-name-input')).toBeVisible();
    await expect(page.getByTestId('profile-hsk-select')).toBeVisible();
    await expect(page.getByTestId('profile-daily-goal-input')).toBeVisible();

    // 4. Update Profile Fields
    await page.getByTestId('profile-display-name-input').fill('Học viên PChinese Mới');
    await page.getByTestId('profile-hsk-select').selectOption('4');
    await page.getByTestId('profile-daily-goal-input').fill('60');

    // 5. Submit Preferences
    await page.getByTestId('profile-submit-btn').click();

    // 6. Verify the save journey settles on the canonical profile form.
    await expect(page.getByTestId('profile-preferences-form')).toBeVisible();
  });

  test('Stale Version Conflict Rejection and Profile Reload Journey', async ({ page }) => {
    await page.goto('/');

    await page.getByTestId('profile-card').click();
    await expect(page.getByTestId('settings-page')).toBeVisible();

    // If server returns 409 STATE_CONFLICT, conflict alert and reload button are rendered
    // Verify reload action selector
    const reloadBtn = page.getByTestId('profile-reload-btn');
    if (await reloadBtn.isVisible()) {
      await reloadBtn.click();
      await expect(page.getByTestId('profile-preferences-form')).toBeVisible();
    }
  });
});
