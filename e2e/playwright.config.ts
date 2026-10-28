import { defineConfig, devices } from '@playwright/test';

/**
 * Browser tests for the MISD Master Asset Registry.
 *
 * The suite drives the real Spring Boot app against a disposable MySQL started
 * by docker-compose.yml (port 3307, seeded from db/02-seed.sql). Tests share one
 * database and some of them change it, so they run serially in one worker.
 *
 *   npm run db:up      # start + seed MySQL (once per session)
 *   npm test           # boots the app on :8090 if it is not already running
 *   npm run db:reset   # back to the seed state
 */
const PORT = Number(process.env.E2E_PORT ?? 8090);

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  timeout: 45_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], storageState: '.auth/user.json' },
      dependencies: ['setup'],
    },
  ],
  webServer: {
    command: 'node scripts/start-app.mjs',
    url: `http://localhost:${PORT}/login`,
    reuseExistingServer: true,
    timeout: 240_000,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});
