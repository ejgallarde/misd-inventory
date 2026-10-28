// Starts the Spring Boot app against the e2e MySQL container. Used by the
// Playwright webServer hook; can also be run by hand: `node scripts/start-app.mjs`.
// Every value can be overridden from the environment.
import { spawn } from 'node:child_process';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const e2eDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(e2eDir, '..');
const storageRoot = process.env.MISD_STORAGE_ROOT ?? resolve(e2eDir, '.storage');
mkdirSync(storageRoot, { recursive: true });

// --demo: the sample-data database (port 3308, npm run demo:up) on port 8091,
// signing in as the sample people from tools/sample-data.
const demo = process.argv.includes('--demo');
const demoUsers = 'earl.gallarde@phlpost.gov.ph:ADMIN,joshua.dejesus@phlpost.gov.ph:CLERK,'
  + 'don.melgarejo@phlpost.gov.ph:CLERK,marigold.libuit-arroyo@phlpost.gov.ph:FINANCE';

const env = {
  ...process.env,
  SERVER_PORT: process.env.E2E_PORT ?? (demo ? '8091' : '8090'),
  SPRING_DATASOURCE_URL:
    process.env.SPRING_DATASOURCE_URL ??
    `jdbc:mysql://localhost:${demo ? 3308 : 3307}/misd_inventory?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC`,
  ...(demo && !process.env.SPRING_APPLICATION_JSON
    ? { SPRING_APPLICATION_JSON: JSON.stringify({ 'app.security.demo-users': demoUsers }) }
    : {}),
  MISD_DB_USERNAME: process.env.MISD_DB_USERNAME ?? 'root',
  MISD_DB_PASSWORD: process.env.MISD_DB_PASSWORD ?? 'e2e-root-pw',
  MISD_MINIO_ACCESS_KEY: process.env.MISD_MINIO_ACCESS_KEY ?? 'unused',
  MISD_MINIO_SECRET_KEY: process.env.MISD_MINIO_SECRET_KEY ?? 'unused',
  MISD_STORAGE_ROOT: storageRoot,
  MISD_DEMO_USER_PASSWORD: process.env.MISD_DEMO_USER_PASSWORD ?? 'e2e-password',
};

// Node refuses to spawn a .cmd directly, and `shell: true` does not reliably
// resolve mvnw.cmd from cwd, so on Windows cmd.exe is invoked explicitly with
// the quoted absolute path (the repo lives under "VS Code Workspace").
const mavenArgs = '-q -B spring-boot:run';
const child =
  process.platform === 'win32'
    ? spawn('cmd.exe', ['/d', '/s', '/c', `""${resolve(repoRoot, 'mvnw.cmd')}" ${mavenArgs}"`], {
        cwd: repoRoot,
        env,
        stdio: 'inherit',
        windowsVerbatimArguments: true,
      })
    : spawn(resolve(repoRoot, 'mvnw'), mavenArgs.split(' '), { cwd: repoRoot, env, stdio: 'inherit' });

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => child.kill(signal));
}
child.on('exit', (code) => process.exit(code ?? 0));
