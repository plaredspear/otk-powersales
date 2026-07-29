import '@testing-library/jest-dom';
import { afterEach, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';

// ─────────────────────────────────────────────────────────────────────────────
// 실제 네트워크 요청 전면 차단.
//
// web 테스트는 backend 등 타 플랫폼에 의존해서는 안 된다. jsdom 의 XMLHttpRequest/fetch 는
// 실제 소켓을 열기 때문에, mock 이 누락된 API 호출이 남아 있으면 (1) 테스트 결과가 backend
// 기동 여부에 좌우되고 (2) 연결 시도/실패 대기가 렌더 루프에 누적되어 testTimeout flaky 의
// 원인이 된다. 요청 시도 자체를 차단하고, 시도가 있었던 테스트는 실패시켜 mock 누락을
// 조용히 넘어가지 않고 즉시 드러낸다. 해결책은 timeout 상향이 아니라 해당 api 모듈/훅의 mock 추가.
// ─────────────────────────────────────────────────────────────────────────────
const attemptedRequests: string[] = [];

function recordBlockedRequest(method: string, url: string): Error {
  const request = `${method.toUpperCase()} ${url}`;
  attemptedRequests.push(request);
  return new Error(
    `[test] 실제 네트워크 요청이 차단되었습니다: ${request} — ` +
      'web 테스트는 타 플랫폼(backend) 에 의존할 수 없으므로 해당 api 모듈이나 훅을 vi.mock 으로 대체하세요.',
  );
}

type TrackedXhr = XMLHttpRequest & { __method?: string; __url?: string };

const originalXhrOpen = XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open = function open(
  this: TrackedXhr,
  method: string,
  url: string | URL,
  ...rest: unknown[]
) {
  this.__method = method;
  this.__url = String(url);
  return (originalXhrOpen as (...args: unknown[]) => void).call(
    this,
    method,
    url,
    ...rest,
  );
} as typeof XMLHttpRequest.prototype.open;

// send 를 동기 throw 시키면 axios 의 xhr adapter 가 Promise executor 안에서 이를 받아
// reject 로 전파한다 (소켓은 열리지 않는다).
XMLHttpRequest.prototype.send = function send(this: TrackedXhr) {
  throw recordBlockedRequest(this.__method ?? 'GET', this.__url ?? '(unknown)');
};

Object.defineProperty(globalThis, 'fetch', {
  configurable: true,
  writable: true,
  value: (input: RequestInfo | URL, init?: RequestInit) => {
    const url =
      typeof input === 'string' || input instanceof URL
        ? String(input)
        : input.url;
    return Promise.reject(recordBlockedRequest(init?.method ?? 'GET', url));
  },
});

beforeEach(() => {
  attemptedRequests.length = 0;
});

afterEach(() => {
  if (attemptedRequests.length === 0) return;
  const requests = [...new Set(attemptedRequests)].join(', ');
  attemptedRequests.length = 0;
  throw new Error(
    `[test] mock 되지 않은 실제 네트워크 요청 시도: ${requests} — ` +
      '해당 api 모듈이나 훅을 vi.mock 으로 대체하세요.',
  );
});

// userEvent 의 기본 delay(키 입력 사이 setTimeout 0ms) 는 jsdom + fake/real timer 환경에서
// 키 한 글자마다 microtask + macrotask 를 누적시켜, 한글 다중 필드 입력 폼 테스트가 전체
// 병렬 실행 시 10초 testTimeout 을 초과해 flaky 하게 실패하는 원인이 된다. setup 의 기본
// 옵션을 delay: null 로 패치해 입력 시뮬레이션의 인위적 지연을 제거한다.
// userEvent.type/click 등 default export 직접 호출도 내부적으로 이 setup 을 거치므로 함께 적용된다.
const originalSetup = userEvent.setup.bind(userEvent);
// userEvent.setup 은 타입상 read-only 라 직접 재할당하면 TS2540 이 발생한다.
// defineProperty 로 덮어써 동일한 런타임 패치를 적용한다.
Object.defineProperty(userEvent, 'setup', {
  configurable: true,
  writable: true,
  value: ((options = {}) =>
    originalSetup({ delay: null, ...options })) as typeof userEvent.setup,
});

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

// jsdom 에는 ResizeObserver 가 없으므로 no-op stub 을 제공한다.
// 테이블 높이 자동 계산 훅(useFlexTableScrollY) 등 ResizeObserver 를 쓰는 컴포넌트를
// 렌더하는 테스트가 ReferenceError 로 실패하지 않도록 한다.
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver;
}
