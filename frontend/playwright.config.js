import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  projects: [
    {
      name: 'default',
      testIgnore: [/.*performance\.spec\.js/],
      use: {
        baseURL: process.env.PLAYWRIGHT_TEST_BASE_URL || 'http://127.0.0.1:4173',
        headless: true,
      },
    },
    {
      name: 'performance',
      testMatch: [/.*performance\.spec\.js/],
      use: {
        baseURL: process.env.PERFORMANCE_TEST_BASE_URL || 'http://127.0.0.1:4173',
        headless: true,
      },
    },
  ],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 4173',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: true,
  },
});
