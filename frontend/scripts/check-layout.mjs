import { chromium } from 'playwright';

const baseURL = process.env.BASE_URL || 'http://localhost:5173';

const viewports = [
  { name: 'mobile', width: 390, height: 844 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'xl', width: 1920, height: 1000 },
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
  { path: '/listing/edit/1', name: 'edit-listing' },
  { path: '/bulletin/create', name: 'create-post' },
  { path: '/bulletin/1', name: 'post-comments' },
  { path: '/profile/edit', name: 'edit-profile' },
];

const browser = await chromium.launch();

for (const vp of viewports) {
  const context = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
  const page = await context.newPage();
  console.log(`\n########## ${vp.name} (${vp.width}x${vp.height}) ##########`);

  for (const route of routes) {
    await page.goto(baseURL + route.path, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1300);

    const report = await page.evaluate(() => {
      const vw = window.innerWidth;
      const sw = document.documentElement.scrollWidth;
      const overflowEls = [];

      if (sw > vw + 1) {
        document.querySelectorAll('body *').forEach((el) => {
          const cls = (el.className || '').toString();
          if (cls.includes('skip-link')) return;
          const r = el.getBoundingClientRect();
          if (r.width === 0 && r.height === 0) return;
          const cs = getComputedStyle(el);
          if (cs.display === 'none' || cs.visibility === 'hidden') return;
          if (r.right > vw + 1 && r.left < vw) {
            overflowEls.push(`R:${Math.round(r.left)}-${Math.round(r.right)} <${el.tagName.toLowerCase()} c="${cls.slice(0,55)}"`);
          }
        });
      }

      // searchbar real overlap
      const inp = document.querySelector('input[aria-label="Search"]');
      let search = null;
      if (inp) {
        const ib = inp.getBoundingClientRect();
        const padL = parseFloat(getComputedStyle(inp).paddingLeft);
        const textStart = ib.left + padL;
        const icon = inp.parentElement.querySelector('svg');
        if (icon) {
          const ic = icon.getBoundingClientRect();
          search = { textStart: Math.round(textStart), iconL: Math.round(ic.left), iconR: Math.round(ic.right), overlap: ic.right > textStart + 1 };
        }
      }

      return { vw, sw, overflowEls: [...new Set(overflowEls)].slice(0, 12), search };
    });

    const problems = [];
    if (report.sw > report.vw + 1) {
      const uniq = [...new Set(report.overflowEls)];
      problems.push(`H-SCROLL (scrollW=${report.sw}) elements: ${uniq.join(' | ') || 'none'}`);
    }
    if (report.search && report.search.overlap) {
      problems.push(`SEARCHBAR-OVERLAP textStart=${report.search.textStart} icon=${report.search.iconL}-${report.search.iconR}`);
    }
    if (problems.length) {
      console.log(`  [${route.name}] ` + problems.join(' ; '));
    }
  }
  await context.close();
}
await browser.close();
console.log('\nDONE');
