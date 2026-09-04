import { chromium } from 'playwright';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.resolve(__dirname, '../shots');
const baseURL = process.env.BASE_URL || 'http://localhost:5173';

const viewports = [
  { name: 'mobile', width: 390, height: 844 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'desktop', width: 1440, height: 900 },
];

const routes = [
  { path: '/', name: 'home' },
  { path: '/search', name: 'search' },
  { path: '/product/1', name: 'product' },
  { path: '/login', name: 'login' },
  { path: '/signup', name: 'signup' },
  { path: '/verification', name: 'verification' },
  { path: '/messages', name: 'messages' },
  { path: '/orders', name: 'orders' },
  { path: '/cart', name: 'cart' },
  { path: '/bulletin', name: 'bulletin' },
  { path: '/profile', name: 'profile' },
  { path: '/settings', name: 'settings' },
  { path: '/saved', name: 'saved' },
  { path: '/listing/create', name: 'create-listing' },
];

const browser = await chromium.launch();
for (const vp of viewports) {
  const context = await browser.newContext({
    viewport: { width: vp.width, height: vp.height },
    deviceScaleFactor: 1,
  });
  const page = await context.newPage();
  for (const route of routes) {
    await page.goto(baseURL + route.path, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200); // let skeletons/animations settle
    const file = path.join(outDir, `${vp.name}-${route.name}.png`);
    await page.screenshot({ path: file, fullPage: true });
    console.log(`saved ${file}`);
  }
  await context.close();
}
await browser.close();
console.log('DONE');
