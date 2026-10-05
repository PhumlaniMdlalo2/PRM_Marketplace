import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'

// Kept separate from vite.config.js so the build config stays exactly what it was. The two share the
// React plugin because JSX in .jsx files has to be transformed in tests too -- without it every
// component test fails to parse, which is a confusing first failure for someone new to the suite.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.js'],
    // Route smoke tests are the point of this suite, not an afterthought, so there is no skip flag
    // here to turn them off and make a run look fast. The route list in routes.test.jsx is the
    // contract; a route added to App.jsx belongs in it.
    css: false, // Tailwind is not compiled for tests; no assertion reads a computed style.
    restoreMocks: true,
  },
})