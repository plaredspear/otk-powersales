/**
 * 대시보드의 탭 · 조회 조건 ↔ URL 쿼리 변환.
 *
 * 기본 현황 탭에서 "여사원 현황에서 보기" 로, 매출현황 탭에서 "월 매출(물류배부)" 로 이동한 뒤
 * 브라우저 back 으로 돌아왔을 때 **보고 있던 탭과 조회 조건(조회월 · 지점)** 을 그대로 되살리기
 * 위한 것이다. 대시보드는 탭 전환 / 조회 실행 시 `replace` 로 URL 을 갱신하므로 history 항목이
 * 늘지 않고, back 은 항상 "떠나기 직전 화면" 으로 되돌아온다.
 */

/**
 * 기본 현황 탭 key. 이 탭에서는 조회월 셀렉터를 잠근다.
 *
 * 사유: 기본 현황의 인원 집계는 **사원 마스터의 현재 상태 스냅샷**이라 조회월과 무관하다
 * (레거시 SF `DKRetail__Employee__c` 도 기간 조건 없는 현재 상태 오브젝트이며, 레거시 인원현황
 * 리포트에 기간 필터가 없다). 셀렉터가 열려 있으면 과거 이력을 조회할 수 있는 것처럼 보이지만
 * 실제로는 값이 바뀌지 않아 오해를 준다.
 *
 * 이 탭의 3개 차트는 모두 같은 기준일을 쓴다 — 유일하게 조회월을 쓰던 '근무형태별 고정/격고/순회'
 * (선택월 MFEIS 환산인원) 는 기준 시점이 섞이는 혼선을 없애기 위해 제거했다(사용자 결정).
 * 실제 기준일은 각 카드 하단에 표기하고(asOfBadge), 잠금 사유는 탭 라벨의 info 아이콘이 안내한다.
 */
export const BASIC_TAB_KEY = 'basic';

/** 탭 키 목록 — URL `?tab=` 복원 시 알 수 없는 값을 걸러내기 위한 화이트리스트. */
export const TAB_KEYS = ['sales', 'deployment', BASIC_TAB_KEY] as const;

export const DEFAULT_TAB = 'sales';

export interface DashboardUrlState {
  tab: string;
  year: number;
  month: number;
  branchCodes: string[];
  /**
   * 조회 조건이 URL 에 실려 있었는지 — 시스템 관리자의 "조회를 눌러야 조회" 규칙을 back 복원에서만
   * 면제하기 위한 플래그. 최초 진입(빈 URL)에서는 false 라 자동 전사 조회가 그대로 차단된다.
   */
  hasSearched: boolean;
}

/**
 * URL 쿼리에서 대시보드 초기 상태를 복원한다.
 *
 * 손상되거나 알 수 없는 값(범위를 벗어난 월, 정의되지 않은 탭 키)은 기본값으로 되돌린다 —
 * 사용자가 URL 을 직접 편집하거나 오래된 링크를 열었을 때 화면이 깨지지 않도록.
 */
export function readDashboardUrlState(searchParams: URLSearchParams, today: Date): DashboardUrlState {
  const tab = searchParams.get('tab') ?? '';
  const matched = /^(\d{4})-(\d{2})$/.exec(searchParams.get('yearMonth') ?? '');
  const month = matched ? Number(matched[2]) : 0;
  // 월 범위를 벗어나면(예: 2026-13) 조회월 전체를 무시하고 당월로 되돌린다.
  const validMonth = matched != null && month >= 1 && month <= 12;
  const branchCodes = (searchParams.get('branchCodes') ?? '')
    .split(',')
    .map((code) => code.trim())
    .filter(Boolean);

  return {
    tab: (TAB_KEYS as readonly string[]).includes(tab) ? tab : DEFAULT_TAB,
    year: validMonth ? Number(matched![1]) : today.getFullYear(),
    month: validMonth ? month : today.getMonth() + 1,
    branchCodes,
    hasSearched: validMonth || branchCodes.length > 0,
  };
}

/**
 * 탭 · 조회 조건을 URL 쿼리로 직렬화한다.
 *
 * 여기 실리는 값이 back 으로 돌아왔을 때 복원되는 전부다 ([readDashboardUrlState] 와 짝).
 */
export function toDashboardSearchParams(
  tab: string,
  yearMonth: string,
  branchCodes?: string[],
): URLSearchParams {
  const params = new URLSearchParams();
  params.set('tab', tab);
  params.set('yearMonth', yearMonth);
  if (branchCodes?.length) params.set('branchCodes', branchCodes.join(','));
  return params;
}
