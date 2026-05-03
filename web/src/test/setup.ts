import '@testing-library/jest-dom';

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
