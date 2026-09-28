import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5174,
    host: true,
    // Dev proxy: forward /api/* to the local Spring Boot backend.
    // This mirrors the Nginx proxy used in Docker, so the frontend
    // code is identical in both environments (relative /api/* paths).
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
