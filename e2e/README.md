# Browser tests (Playwright)

End-to-end tests that drive the real app in Chromium against a disposable MySQL.

## Prerequisites

- Docker Desktop running
- Node.js 20+
- A JDK on `JAVA_HOME` (17 or newer; developed against 25)

## Run

```
cd e2e
npm install
npx playwright install chromium   # first time only
npm run db:up                     # MySQL 8.4 on :3307, schema + db/02-seed.sql
npm test                          # boots the app on :8090 if it isn't running
```

- `npm run test:ui` opens Playwright's interactive runner. `npm run test:headed` shows the browser.
- `npm run report` opens the HTML report. Failures keep a trace, a screenshot and a video.
- `npm run db:reset` returns the database to the seed state. The suite can be re-run without a reset, but a reset gives a known baseline.

The app is started by `scripts/start-app.mjs` with throwaway credentials (demo accounts `admin@`, `clerk@`, `finance@` and `viewer@phlpost.gov.ph`, all with password `e2e-password`; most specs run as admin, and `roles.spec.ts` signs in as each role). Override any of `E2E_PORT`, `SPRING_DATASOURCE_URL`, `MISD_DB_*` or `MISD_DEMO_USER_PASSWORD` through the environment.

## Layout

| Path | Purpose |
| --- | --- |
| `docker-compose.yml` | MySQL with `lower_case_table_names=1` (the dumps are lowercase, the entities are not) |
| `db/01-schema.sh` | Loads `src/main/resources/db/misd_inventory_*.sql` |
| `db/02-seed.sql` | Deterministic `E2E-*` records covering each module and status |
| `tests/support/app.ts` | Shared helpers: table rows, row actions, Tom Select picking, direct form posts, toasts |
| `tests/*.spec.ts` | Auth, dashboard, IT assets, fleet, properties, API/security, roles |

## Known-bug tests

Tests marked `test.fail(true, 'BUG-…')` assert the **correct** behaviour and currently fail on purpose. They pass while the bug exists, and Playwright reports them as "unexpectedly passed" once it is fixed. When that happens, remove the `test.fail` line.
