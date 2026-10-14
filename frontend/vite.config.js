import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const strip = (path) => path.replace(/^\/api/, '')

const target = (port) => ({
  target: `http://localhost:${port}`,
  changeOrigin: true,
  rewrite: strip,
})

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/auth': target(8081),
      '/api/residents': target(8081),
      '/api/apartments': target(8081),
      '/api/invoices': target(8082),
      '/api/maintenance': target(8083),
    }
  }
})
