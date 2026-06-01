import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// 모바일앱용 web (Heroku 레거시 현장 페이지 재구현).
// web/(admin) 과 동일 스택이나 별도 앱으로 분리 — backend `/api/v1/mobile/*` 를 타겟한다.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5174, // web/(admin, 5173) 과 동시 구동 가능하도록 분리
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    // 모바일 WebView 콜드스타트 대비 — 무거운 의존성(echarts) 을 별도 청크로 분리.
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts', 'echarts-for-react'],
          antd: ['antd', '@ant-design/icons'],
        },
      },
    },
  },
})
