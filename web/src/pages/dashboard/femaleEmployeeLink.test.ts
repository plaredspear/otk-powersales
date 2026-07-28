import { describe, it, expect } from 'vitest';
import { femaleEmployeeLinkState } from './femaleEmployeeLink';

/** 활성 조건을 모두 만족하는 기본 입력 — 각 테스트가 필요한 축만 뒤집는다. */
const ENABLED = { branchCode: '5815', isActiveScope: true, canView: true };

describe('femaleEmployeeLinkState', () => {
  it('단일 지점 + 재직 기준 + 권한 보유면 여사원 현황 링크를 연다', () => {
    const state = femaleEmployeeLinkState(ENABLED);

    expect(state).toEqual({
      to: '/female-employee?status=%EC%9E%AC%EC%A7%81&costCenterCode=5815',
    });
  });

  it("집계 기준이 '재직+휴직' 이면 비활성 + 사유 안내", () => {
    const state = femaleEmployeeLinkState({ ...ENABLED, isActiveScope: false });

    expect(state).toEqual({
      disabledReason: expect.stringContaining("집계 기준이 '재직'"),
    });
  });

  it('지점이 전체/다중이면 비활성 + 사유 안내', () => {
    const state = femaleEmployeeLinkState({ ...ENABLED, branchCode: null });

    expect(state).toEqual({
      disabledReason: expect.stringContaining('지점을 하나만'),
    });
  });

  it('두 조건이 모두 어긋나면 사유를 함께 안내한다', () => {
    const state = femaleEmployeeLinkState({
      ...ENABLED,
      branchCode: null,
      isActiveScope: false,
    });

    const reason = (state as { disabledReason: string }).disabledReason;
    expect(reason).toContain("집계 기준이 '재직'");
    expect(reason).toContain('지점을 하나만');
  });

  it('권한이 없으면 조회 조건과 무관하게 권한 사유만 안내한다', () => {
    // 대시보드는 권한 가드가 없어 female_employee 권한 없는 사용자도 보므로, 링크만 열면 이동 후 403.
    const state = femaleEmployeeLinkState({ ...ENABLED, canView: false });

    expect(state).toEqual({ disabledReason: expect.stringContaining('권한') });
    expect((state as { disabledReason: string }).disabledReason).not.toContain('집계 기준');
  });
});
