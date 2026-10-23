import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      proxy: {
        // The gateway owns routing, CORS and JWT validation; it strips the /api prefix.
        '/api': {
          target: env.GATEWAY_URL || 'http://localhost:8080', // dev-only fallback
          changeOrigin: true,
        },
      },
    },
  }
})
