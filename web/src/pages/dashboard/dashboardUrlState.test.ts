import { describe, it, expect } from 'vitest';
import { readDashboardUrlState, toDashboardSearchParams } from './dashboardUrlState';

/** 조회월이 URL 에 없을 때의 기본값 기준일. */
const TODAY = new Date(2026, 6, 29); // 2026-07-29

describe('readDashboardUrlState', () => {
  it('빈 URL 이면 매출현황 탭 + 당월 + 지점 미선택 (최초 진입)', () => {
    const state = readDashboardUrlState(new URLSearchParams(''), TODAY);

    expect(state).toEqual({
      tab: 'sales',
      year: 2026,
      month: 7,
      branchCodes: [],
      // 시스템 관리자의 "조회를 눌러야 조회" 규칙이 그대로 유지되어야 한다.
      hasSearched: false,
    });
  });

  it('탭 + 조회월 + 지점을 모두 복원한다 (back 으로 돌아온 경우)', () => {
    const state = readDashboardUrlState(
      new URLSearchParams('tab=basic&yearMonth=2026-03&branchCodes=5815,5820'),
      TODAY,
    );

    expect(state).toEqual({
      tab: 'basic',
      year: 2026,
      month: 3,
      branchCodes: ['5815', '5820'],
      hasSearched: true,
    });
  });

  it('지점만 실려 있어도 조회한 상태로 복원한다', () => {
    const state = readDashboardUrlState(new URLSearchParams('branchCodes=5815'), TODAY);

    expect(state).toMatchObject({ branchCodes: ['5815'], hasSearched: true });
  });

  it('정의되지 않은 탭 키는 매출현황으로 되돌린다', () => {
    const state = readDashboardUrlState(new URLSearchParams('tab=unknown'), TODAY);

    expect(state.tab).toBe('sales');
  });

  it('월 범위를 벗어난 조회월은 당월로 되돌린다', () => {
    const state = readDashboardUrlState(new URLSearchParams('yearMonth=2026-13'), TODAY);

    expect(state).toMatchObject({ year: 2026, month: 7, hasSearched: false });
  });

  it('형식이 깨진 조회월은 당월로 되돌린다', () => {
    const state = readDashboardUrlState(new URLSearchParams('yearMonth=2026-7'), TODAY);

    expect(state).toMatchObject({ year: 2026, month: 7, hasSearched: false });
  });

  it('빈 지점 코드 조각은 버린다', () => {
    const state = readDashboardUrlState(new URLSearchParams('branchCodes=,5815, ,5820,'), TODAY);

    expect(state.branchCodes).toEqual(['5815', '5820']);
  });
});

describe('toDashboardSearchParams', () => {
  it('탭 + 조회월 + 지점을 직렬화한다', () => {
    const params = toDashboardSearchParams('basic', '2026-03', ['5815', '5820']);

    expect(params.toString()).toBe('tab=basic&yearMonth=2026-03&branchCodes=5815%2C5820');
  });

  it('지점 미선택이면 branchCodes 를 싣지 않는다 (권한 스코프 전체 조회)', () => {
    const params = toDashboardSearchParams('sales', '2026-07', undefined);

    expect(params.has('branchCodes')).toBe(false);
    expect(params.toString()).toBe('tab=sales&yearMonth=2026-07');
  });

  it('직렬화한 URL 을 그대로 다시 읽으면 같은 상태가 된다 (round-trip)', () => {
    const params = toDashboardSearchParams('deployment', '2026-03', ['5815']);

    expect(readDashboardUrlState(new URLSearchParams(params.toString()), TODAY)).toEqual({
      tab: 'deployment',
      year: 2026,
      month: 3,
      branchCodes: ['5815'],
      hasSearched: true,
    });
  });
});
