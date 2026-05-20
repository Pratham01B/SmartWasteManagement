import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
<<<<<<< HEAD
=======
      // Proxy to Python AI service
      '/ai': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai/, ''),
      },
>>>>>>> 4ff527895e680a04d3860a9e3bcd1e7c9aba6340
    },
  },
})
