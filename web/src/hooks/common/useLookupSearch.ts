import { useCallback, useEffect, useRef, useState } from 'react';

/** 검색 실행 최소 입력 길이. 1자 검색은 결과가 과다해 실효가 없다. */
const MIN_KEYWORD_LENGTH = 2;
/** 입력이 멈춘 뒤 조회하기까지의 지연 (web-conventions.md § Search Debounce). */
const DEBOUNCE_MS = 300;

interface Page<T> {
  content: T[];
  totalElements: number;
}

interface Options<TRow, TItem> {
  /** 키워드/건수를 받아 목록을 조회한다. */
  fetchPage: (params: { keyword: string; size: number }) => Promise<Page<TRow>>;
  /** 응답 row 를 화면이 쓸 형태로 좁힌다. null 을 반환하면 결과에서 제외한다. */
  toItem: (row: TRow) => TItem | null;
  /** 한 번에 가져올 건수. 드롭다운 노출량에 맞춘다. */
  size?: number;
}

export interface LookupSearchResult<TItem> {
  /** 검색 결과 — 옵션 라벨 가공은 호출부가 담당한다. */
  items: TItem[];
  /** 전체 결과 건수. 검색 전이거나 2자 미만이면 null (드롭다운 "총 N개" 표시용). */
  total: number | null;
  searching: boolean;
  /** 고급 검색 모달이 이어받을 키워드 — 입력 중 값을 보관한다. */
  keyword: string;
  /** Select `onSearch` 에 그대로 연결한다. */
  onSearch: (keyword: string) => void;
  /** Select `onClear` 에 연결 — 사용자가 x 로 비운 경우 보관 키워드도 정리한다. */
  clearKeyword: () => void;
  /** 선택 확정 시 호출 — 결과를 고른 1건으로 좁히고 보관 키워드를 비운다. */
  selectItem: (item: TItem) => void;
}

/**
 * 검색어 기반 lookup Select 의 조회 상태 관리 — debounce / 요청 순번 / 키워드 보관.
 *
 * 행사마스터의 거래처·대표제품 lookup 이 공유한다. 조회 함수와 row 매핑을 주입받아
 * 도메인에 중립적이며, 옵션 라벨 형태는 화면마다 다르므로 결과를 가공 없이
 * [LookupSearchResult.items] 로 돌려주고 표시는 호출부가 맡는다.
 *
 * 검색어 보관 규칙 — AntD 는 blur/선택 시 검색어를 비우며 `onSearch('')` 를 호출한다.
 * "고급 검색" 버튼 클릭도 blur 를 유발하므로, 빈 문자열로 보관값을 덮어쓰면 모달이
 * 키워드를 이어받지 못한다. 따라서 빈 값은 무시하고, 사용자가 명시적으로 비운 경우만
 * [LookupSearchResult.clearKeyword] 로 정리한다.
 */
export function useLookupSearch<TRow, TItem>({
  fetchPage,
  toItem,
  size = 20,
}: Options<TRow, TItem>): LookupSearchResult<TItem> {
  const [items, setItems] = useState<TItem[]>([]);
  const [total, setTotal] = useState<number | null>(null);
  const [searching, setSearching] = useState(false);
  const [keyword, setKeyword] = useState('');

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // 요청 순번 — 늦게 도착한 이전 키워드 응답이 최신 결과를 덮어쓰지 않게 검증한다.
  const seqRef = useRef(0);

  // 콜백은 호출부에서 매 렌더 새로 만들어지기 쉬우므로 ref 로 최신값만 유지한다
  // (onSearch 의 참조가 바뀌어 debounce 타이머가 리셋되는 것을 막는다).
  const fetchPageRef = useRef(fetchPage);
  fetchPageRef.current = fetchPage;
  const toItemRef = useRef(toItem);
  toItemRef.current = toItem;

  // 언마운트 시 대기 중인 debounce 타이머 정리 (unmounted 컴포넌트 setState 방지).
  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const onSearch = useCallback(
    (next: string) => {
      // 빈 문자열은 AntD 의 내부 초기화이므로 보관값을 덮어쓰지 않는다(위 주석 참고).
      if (next !== '') setKeyword(next);

      if (timerRef.current) clearTimeout(timerRef.current);
      if (next.length < MIN_KEYWORD_LENGTH) {
        setItems([]);
        setTotal(null);
        setSearching(false);
        return;
      }

      setSearching(true);
      timerRef.current = setTimeout(async () => {
        const seq = ++seqRef.current;
        try {
          const result = await fetchPageRef.current({ keyword: next, size });
          if (seq !== seqRef.current) return;
          setItems(
            result.content
              .map((row) => toItemRef.current(row))
              .filter((item): item is TItem => item != null),
          );
          setTotal(result.totalElements);
        } catch {
          if (seq !== seqRef.current) return;
          setItems([]);
          setTotal(null);
        } finally {
          if (seq === seqRef.current) setSearching(false);
        }
      }, DEBOUNCE_MS);
    },
    [size],
  );

  const clearKeyword = useCallback(() => setKeyword(''), []);

  const selectItem = useCallback((item: TItem) => {
    setItems([item]);
    setKeyword('');
  }, []);

  return { items, total, searching, keyword, onSearch, clearKeyword, selectItem };
}
