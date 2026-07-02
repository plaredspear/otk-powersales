import type { TableProps } from 'antd';

/** B형(보고서형) 페이지의 조회 전 안내 표준 문구. */
export const BEFORE_SEARCH_TEXT = '조회 조건을 설정한 후 조회 버튼을 눌러주세요.';

/** 조회 결과 0건 표준 문구. */
export const NO_RESULT_TEXT = '조회 결과가 없습니다.';

export interface ListTableLocaleOptions {
  /**
   * 조회 실행 여부 (B형 전용).
   * - false: 조회 전 안내 문구를 표시.
   * - true 또는 생략 (A형 — 진입 시 자동 조회라 조회 전 상태 없음): 결과 없음 문구.
   */
  searched?: boolean;
  /** 조회 전 안내 문구 대체 (페이지 고유 필수 조건 안내 등). */
  beforeSearchText?: string;
  /** 결과 없음 문구 대체. */
  emptyText?: string;
}

/**
 * 목록/보고서 테이블의 빈 상태 `locale` 을 생성하는 공통 헬퍼.
 *
 * 조회 전(B형 한정) / 결과 없음 두 상태의 표준 문구를 한곳에 캡슐화한다.
 * - A형(목록형): `listTableLocale()` — 항상 "조회 결과가 없습니다."
 * - B형(보고서형): `listTableLocale({ searched })` — 조회 전에는 안내 문구, 조회 후 0건이면 결과 없음.
 */
export function listTableLocale(
  options: ListTableLocaleOptions = {},
): NonNullable<TableProps<never>['locale']> {
  const { searched = true, beforeSearchText = BEFORE_SEARCH_TEXT, emptyText = NO_RESULT_TEXT } = options;
  return { emptyText: searched ? emptyText : beforeSearchText };
}
