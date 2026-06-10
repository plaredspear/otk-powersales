import '@testing-library/jest-dom';
import userEvent from '@testing-library/user-event';

// userEvent 의 기본 delay(키 입력 사이 setTimeout 0ms) 는 jsdom + fake/real timer 환경에서
// 키 한 글자마다 microtask + macrotask 를 누적시켜, 한글 다중 필드 입력 폼 테스트가 전체
// 병렬 실행 시 10초 testTimeout 을 초과해 flaky 하게 실패하는 원인이 된다. setup 의 기본
// 옵션을 delay: null 로 패치해 입력 시뮬레이션의 인위적 지연을 제거한다.
// userEvent.type/click 등 default export 직접 호출도 내부적으로 이 setup 을 거치므로 함께 적용된다.
const originalSetup = userEvent.setup.bind(userEvent);
userEvent.setup = ((options = {}) =>
  originalSetup({ delay: null, ...options })) as typeof userEvent.setup;

// jsdom 에는 matchMedia 가 없으므로 antd 등의 라이브러리가 사용할 수 있도록 stub 을 제공한다.
if (typeof window !== 'undefined' && !window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}
