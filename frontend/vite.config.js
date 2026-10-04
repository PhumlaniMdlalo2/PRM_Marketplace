import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// The browser only ever requests same-origin "/api/..." paths; Vite forwards them to the Spring
// backend in development. That keeps the axios baseURL identical in dev and production, so the
// one thing that differs between the two environments is not the request URL.
const backend = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: backend,
        changeOrigin: true,
      },
    },
  },
})