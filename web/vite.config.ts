import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: ['babel-plugin-react-compiler'],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // userEvent.type 한글 입력 + Form validation + mutation 처리 누적으로 전체 테스트 실행 시
    // 5초 기본 timeout 을 초과하는 케이스가 다수 발생. 개별 it 마다 명시하던 timeout 옵션을
    // 전역 10초로 통일 — 신규 테스트 추가 시에도 일관 적용.
    testTimeout: 10000,
  },
})
