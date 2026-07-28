/**
 * 대시보드 기본 현황 → 여사원 현황 이동 링크의 활성 조건.
 *
 * 여사원 현황은 **단일 지점 + 재직 기준** 화면이라, 대시보드의 현재 조회 조건이 그대로 옮겨지는
 * 경우에만 링크를 연다. 조건이 맞지 않는데도 이동시키면 대시보드에서 보던 숫자와 목록 건수가
 * 달라져(다중 지점 합산 / 휴직 포함) 사용자가 불일치를 버그로 읽게 된다.
 *
 * - 지점: 조회에 사용된 지점이 정확히 1개. 여사원 현황의 지점 셀렉터는 단일 선택이고 서버도
 *   `costCenterCode` 단건만 받으므로, 다중 선택/전체 조회는 옮길 수단이 없다.
 * - 집계 기준: '재직'. 여사원 현황은 상태 필터 '재직' 으로 열린다.
 * - 권한: `female_employee:READ`. 대시보드는 권한 가드가 없어 이 권한이 없는 사용자도 보기 때문에,
 *   링크만 열어두면 이동 후 403 을 만난다.
 *
 * 두 화면의 지점 옵션은 동일 resolver(backend `DashboardBranchResolver`)에서 나오므로,
 * 여기서 넘기는 지점 코드는 여사원 현황에서도 항상 선택 가능한 값이다.
 */
export type FemaleEmployeeLinkState = { to: string } | { disabledReason: string };

export interface FemaleEmployeeLinkParams {
  /** 조회에 실제 사용된 지점 코드. 전체/다중 조회는 null. */
  branchCode: string | null;
  /** 기본 현황 집계 기준이 '재직' 인지. */
  isActiveScope: boolean;
  /** 여사원 현황 조회 권한 보유 여부. */
  canView: boolean;
}

export function femaleEmployeeLinkState(params: FemaleEmployeeLinkParams): FemaleEmployeeLinkState {
  // 권한 부재는 조회 조건과 무관한 차단 사유라 단독으로 안내한다 (조건을 맞춰도 열리지 않으므로).
  if (!params.canView) {
    return { disabledReason: '여사원 현황 조회 권한(female_employee)이 없습니다.' };
  }

  // 조건이 둘 다 어긋나면 둘 다 안내한다 — 하나만 고쳐도 여전히 비활성인 이유를 알 수 있도록.
  const reasons: string[] = [];
  if (!params.isActiveScope) {
    reasons.push("집계 기준이 '재직' 일 때만 이동할 수 있습니다. 여사원 현황은 재직 기준으로 열립니다.");
  }
  if (params.branchCode == null) {
    reasons.push('여사원 현황은 지점을 하나만 조회할 수 있습니다. 지점 1개를 선택해 조회해 주세요.');
  }
  if (reasons.length > 0) return { disabledReason: reasons.join('\n') };

  // status 는 여사원 현황의 기본값과 같지만, 링크의 의도(재직 기준)를 URL 에 명시해
  // 기본값이 바뀌어도 이동 결과가 달라지지 않게 한다.
  const search = new URLSearchParams({ status: '재직', costCenterCode: params.branchCode as string });
  return { to: `/female-employee?${search.toString()}` };
}
