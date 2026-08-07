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
  // react-resizable 이 의존하는 react-draggable 이 런타임에 process.env.DRAGGABLE_DEBUG
  // 를 참조한다. Vite 브라우저 빌드에는 process 전역이 없어 "process is not defined"
  // ReferenceError 가 발생하므로, 해당 디버그 플래그를 정적으로 false 치환한다.
  define: {
    'process.env.DRAGGABLE_DEBUG': 'false',
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
    // 전역으로 통일 — 신규 테스트 추가 시에도 일관 적용.
    // 20초인 이유: Vite transform 캐시가 비어 있는 첫 실행(CI 신규 러너 / 캐시 삭제 후)에는
    // 62개 파일 병렬 transform 이 워커를 굶겨 개별 테스트 wall-clock 이 평소의 수 배가 된다.
    // 10초에서는 이때 무거운 antd 폼 테스트가 일제히 timeout 났다(캐시 warm 상태면 전부 통과).
    testTimeout: 20000,
  },
})
