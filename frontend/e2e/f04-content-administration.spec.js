import { test, expect } from '@playwright/test';

const correlationId = '00000000-0000-4000-8000-000000000004';
const envelope = (data, meta = {}) => ({ success: true, data, error: null, meta: { correlationId, ...meta } });

test.describe('F04 content administration journey', () => {
  test('an authenticated admin manages safe projections and cannot re-approve terminal media', async ({ page }) => {
    const topic = {
      id: '11111111-1111-1111-1111-111111111111', entityType: 'TOPIC', publicationStatus: 'DRAFT', version: 0,
      title: 'HSK 1 Stories', slug: 'hsk-1-stories', hskLevel: 1,
    };
    const terminalMedia = {
      id: '22222222-2222-2222-2222-222222222222', entityType: 'MEDIA', publicationStatus: 'REJECTED', version: 3,
      title: 'Rejected video', providerName: 'YOUTUBE', youtubeVideoId: 'dQw4w9WgXcQ', approvalEligible: false,
    };

    await page.route('**/api/v1/auth/refresh', (route) => route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(envelope({
        accessToken: 'test-access-token', expiresAt: new Date(Date.now() + 60000).toISOString(),
        roles: ['ADMIN'], browserSessionId: 'e2e-admin-session',
      })),
    }));
    await page.route('**/api/v1/admin/content/**', (route) => {
      const path = new URL(route.request().url()).pathname;
      const data = path.endsWith('/topics') ? [topic] : path.endsWith('/media') ? [terminalMedia] : [];
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(envelope(data, {
        page: 0, size: 50, totalElements: data.length, totalPages: 1,
      })) });
    });
    await page.route('**/api/v1/topics/*/publish', (route) => route.fulfill({
      contentType: 'application/json', body: JSON.stringify(envelope({ ...topic, publicationStatus: 'PUBLISHED', version: 1 })),
    }));

    await page.goto('/');
    await expect(page.getByTestId('nav-admin-content')).toBeVisible();
    await page.getByTestId('nav-admin-content').click();
    await expect(page.getByTestId('content-admin-page')).toBeVisible();
    await expect(page.getByTestId(`topic-card-${topic.id}`)).toBeVisible();

    await page.getByTestId('publish-btn').click();
    await expect(page.getByTestId(`topic-card-${topic.id}`)).toBeVisible();

    await page.getByTestId('subtab-media').click();
    await expect(page.getByTestId(`media-card-${terminalMedia.id}`)).toBeVisible();
    await expect(page.getByTestId(`youtube-thumbnail-${terminalMedia.id}`)).toHaveAttribute(
      'src',
      'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
    );
    await expect(page.getByTestId('approve-media-btn')).toHaveCount(0);
    await expect(page.getByTestId('reject-media-btn')).toHaveCount(0);
    await expect(page.getByTestId('quarantine-media-btn')).toHaveCount(0);
  });

  test('an admin registers a YouTube reference through JSON without a file input', async ({ page }) => {
    const media = {
      id: '33333333-3333-3333-3333-333333333333', entityType: 'MEDIA', publicationStatus: 'PENDING_SCAN', version: 0,
      title: 'Greeting video', providerName: 'YOUTUBE', youtubeVideoId: 'dQw4w9WgXcQ', approvalEligible: false,
    };

    await page.route('**/api/v1/auth/refresh', (route) => route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(envelope({
        accessToken: 'test-access-token', expiresAt: new Date(Date.now() + 60000).toISOString(),
        roles: ['ADMIN'], browserSessionId: 'e2e-admin-session',
      })),
    }));
    await page.route('**/api/v1/admin/content/**', (route) => route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(envelope([], { page: 0, size: 50, totalElements: 0, totalPages: 0 })),
    }));
    await page.route('**/api/v1/media', async (route) => {
      expect(route.request().method()).toBe('POST');
      expect(route.request().headers()['content-type']).toContain('application/json');
      expect(JSON.parse(route.request().postData() || '{}')).toEqual({
        youtubeVideoReference: 'https://youtu.be/dQw4w9WgXcQ',
        durationMilliseconds: 120000,
        title: 'Greeting video',
        altText: '',
      });
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(envelope(media)) });
    });

    await page.goto('/');
    await page.getByTestId('nav-admin-content').click();
    await expect(page.getByTestId('content-admin-page')).toBeVisible();
    await page.getByTestId('subtab-media').click();
    await page.getByTestId('create-content-btn').click();
    await expect(page.getByTestId('media-youtube-reference-input')).toBeVisible();
    await expect(page.locator('input[type="file"]')).toHaveCount(0);

    await page.getByTestId('media-youtube-reference-input').fill('https://youtu.be/dQw4w9WgXcQ');
    await page.getByTestId('media-title-input').fill('Greeting video');
    await page.getByTestId('content-editor-submit').click();
    await expect(page.getByTestId('content-editor-form')).toHaveCount(0);
    await expect(page.getByRole('status')).toContainText('YouTube video registered as ID: dQw4w9WgXcQ');
  });
});
