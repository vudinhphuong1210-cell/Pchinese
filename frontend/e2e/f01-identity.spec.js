/**
 * Playwright / E2E Specification Test for F01 Identity, Refresh, Session & Admin Journeys
 * Uses stable data-testid selectors required by CONSTITUTION Article 5.
 */

import { test, expect } from '@playwright/test';

test.describe('F01 End-to-End Identity & Admin Journeys', () => {

  test('Learner Registration, Verification, Login & Session Management Flow', async ({ page }) => {
    // 1. Visit App
    await page.goto('/');

    // 2. Navigate to Auth / Register
    await page.getByTestId('nav-auth').click();
    await page.getByTestId('register-link').click();

    // 3. Fill Neutral Registration
    await page.getByTestId('register-email-input').fill('learner.test@pchinese.net');
    await page.getByTestId('register-password-input').fill('StrongPass123!');
    await page.getByTestId('register-submit-btn').click();

    // 4. Neutral Response Verification
    await expect(page.getByTestId('register-success')).toBeVisible();

    // 5. Navigate to Login
    await page.getByTestId('login-link').click();
    await expect(page.getByTestId('login-form')).toBeVisible();

    // 6. Login
    await page.getByTestId('login-email-input').fill('learner.test@pchinese.net');
    await page.getByTestId('login-password-input').fill('StrongPass123!');
    await page.getByTestId('login-submit-btn').click();

    // 7. Verify Dashboard Session state
    await expect(page.getByTestId('profile-card')).toContainText('Học viên PChinese');
  });

  test('Admin User Management list and selected-user state management flow', async ({ page }) => {
    await page.goto('/');

    // Navigate to Admin tab
    await page.getByTestId('nav-admin').click();
    await expect(page.getByTestId('account-roles-tab')).toBeVisible();

    // Select a server-provided safe directory row; there is no manually entered UUID path.
    await expect(page.getByTestId('user-directory-list')).toBeVisible();
    await page.getByTestId(/^manage-user-/).first().click();

    // Projection card should render
    await expect(page.getByTestId('projection-card')).toBeVisible();
    await expect(page.getByTestId('access-state-badge')).toBeVisible();
  });
});
