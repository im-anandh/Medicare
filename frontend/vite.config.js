import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// DEVELOPMENT (npm run dev): the page is served by Vite on :5173 and every
// /api request is forwarded to the Spring Boot backend on :8080. To the
// browser it all looks like ONE origin, so no CORS configuration is needed.
//
// PRODUCTION (npm run build): the finished files are written straight into
// the Spring Boot project's static folder, so the backend serves the app
// itself at http://localhost:8080 - again one origin, no CORS, and no Node
// needed just to RUN the project.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
  build: { outDir: '../src/main/resources/static', emptyOutDir: true },
});
