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
    // Route smoke tests are the point of this suite, not an afterthought, so they are not allowed
    // to be skipped to make a run look fast.
    css: false,
    restoreMocks: true,
  },
})