import { useLayoutEffect, useRef, useState } from 'react';

/**
 * 목록 페이지가 페이지 전체로 스크롤되지 않고 테이블 body(행 영역) 만 세로 스크롤되도록,
 * 페이지 컨테이너 높이와 antd Table `scroll.y` 를 상단 가변 요소에 맞춰 자동 산출한다.
 *
 * 배경: `calc(100vh - N)` 하드코딩은 헤더/브레드크럼/대행 배너 등 높이가 상황마다 달라지는
 * 상단 요소와 어긋나 페이지 전체 스크롤이 남는다. 대신 컨테이너의 화면상 top 위치(getBoundingClientRect)
 * 를 실측해 `100vh - top - 하단여백` 을 컨테이너 높이로 고정하면, 배너 노출/사이드바 접힘/창 리사이즈로
 * 상단 높이가 바뀌어도 컨테이너가 항상 뷰포트 안에 정확히 들어가 페이지 자체는 스크롤되지 않는다.
 *
 * 그 컨테이너 안에서 툴바/필터바는 고정(flexShrink:0), 테이블 wrapper 는 flex:1+minHeight:0 로 남은
 * 높이를 채우고, 그 wrapper 의 실측 높이에서 헤더 행 높이를 뺀 값을 `scroll.y` 로 주어 body 만 스크롤한다.
 *
 * 사용:
 *   const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY();
 *   <div ref={containerRef} style={{ display:'flex', flexDirection:'column', height: containerHeight, minHeight:0 }}>
 *     <ToolbarRow style={{ flexShrink: 0 }} />
 *     <FilterRow style={{ flexShrink: 0 }} />
 *     <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
 *       <Table scroll={{ x: 2020, y: scrollY }} ... />
 *     </div>
 *   </div>
 *
 * @param bottomGap 테이블 아래에 남길 최소 여백(px). 레이아웃 조상의 padding-bottom/border 는 훅이
 *   자동 실측해 별도로 빼므로, 여기엔 순수 추가 여백만 준다. 기본 16.
 * @param headerReserve 테이블 헤더 행 등 body 외 요소 높이(px). wrapper 높이에서 뺀 값을 scroll.y 로.
 *   기본 39(antd size="small" 헤더 높이).
 */
export function useFlexTableScrollY(bottomGap = 16, headerReserve = 39) {
  const containerRef = useRef<HTMLDivElement>(null);
  const tableWrapperRef = useRef<HTMLDivElement>(null);
  const [containerHeight, setContainerHeight] = useState<string | number>('auto');
  const [scrollY, setScrollY] = useState<number | undefined>(undefined);

  useLayoutEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const measure = () => {
      // 컨테이너의 "문서 기준" top(= 화면상 top + 현재 스크롤량)을 쓴다. getBoundingClientRect().top
      // 은 페이지 스크롤에 따라 변해, 스크롤된 상태에서 재면 컨테이너 높이가 커져 더 넘치는 불안정이
      // 생긴다. window.scrollY 를 더하면 스크롤 상태와 무관한 고정 offset 이 되어 컨테이너가 항상
      // 뷰포트에 정확히 맞고 페이지 자체 스크롤이 사라진다.
      const docTop = container.getBoundingClientRect().top + window.scrollY;
      // 컨테이너 아래쪽에서 뷰포트를 밀어내는 여백을 실측해 함께 뺀다. 컨테이너를 감싸는 조상들의
      // padding-bottom(예: 레이아웃 Outlet wrapper 의 하단 padding)은 docTop 에 안 잡히므로,
      // 조상 체인을 훑어 하단 여백 합을 구한다. 이러면 레이아웃 padding 값이 바뀌어도 자동 대응하고
      // bottomGap 매직넘버에 의존하지 않는다.
      let bottomInset = 0;
      for (let el: HTMLElement | null = container; el && el !== document.body; el = el.parentElement) {
        const parent = el.parentElement;
        if (!parent) break;
        const ps = getComputedStyle(parent);
        bottomInset += parseFloat(ps.paddingBottom) || 0;
        bottomInset += parseFloat(ps.borderBottomWidth) || 0;
      }
      const nextHeight = Math.max(0, window.innerHeight - docTop - bottomInset - bottomGap);
      // 1px 미만 차이는 무시해 measure→set→리렌더→ResizeObserver 재호출 루프의 진동을 끊는다.
      setContainerHeight((prev) =>
        typeof prev === 'number' && Math.abs(prev - nextHeight) < 1 ? prev : nextHeight,
      );

      // 테이블 wrapper 실측 높이에서 헤더 행 높이를 뺀 값이 body 스크롤 영역.
      const wrapper = tableWrapperRef.current;
      if (wrapper) {
        const h = wrapper.clientHeight - headerReserve;
        const nextY = h > 0 ? h : undefined;
        setScrollY((prev) =>
          prev !== undefined && nextY !== undefined && Math.abs(prev - nextY) < 1 ? prev : nextY,
        );
      }
    };

    measure();
    // 컨테이너(상단 요소 변화로 top 이동) + 테이블 wrapper(높이 변화) 양쪽을 관측.
    const ro = new ResizeObserver(measure);
    ro.observe(container);
    if (tableWrapperRef.current) ro.observe(tableWrapperRef.current);
    // 상단 배너 노출 등 컨테이너 밖 레이아웃 변화는 body 리사이즈로 감지.
    ro.observe(document.body);
    window.addEventListener('resize', measure);
    return () => {
      ro.disconnect();
      window.removeEventListener('resize', measure);
    };
  }, [bottomGap, headerReserve]);

  return { containerRef, containerHeight, tableWrapperRef, scrollY };
}
