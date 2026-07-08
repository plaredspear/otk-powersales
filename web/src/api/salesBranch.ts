import client from './client';
import type { ApiResponse } from './types';
import type { Branch } from './team-schedule';

/**
 * 매출/실적 계열 화면 지점 셀렉터 옵션 조회.
 *
 * 화면마다 전용 endpoint 로 분리한다 — 향후 화면별 권한/지점 스코프를 독립적으로 조정하기 위함.
 * 모두 `monthly_sales_history` READ 로 가드된다. 여사원 일정의 `/team-schedule/branches`
 * (`team_member_schedule` 가드) 를 빌려쓰면 `team_member_schedule` READ 없는 사용자가 지점 셀렉터에서
 * 403 이 나므로 매출/실적 계열은 `monthly_sales_history` 가드의 전용 endpoint 로 분리한다.
 * 반환 지점 범위는 backend 에서 권한 기준으로 제한된다.
 */
async function fetchBranches(path: string): Promise<Branch[]> {
  const res = await client.get<ApiResponse<Branch[]>>(path);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 월 매출(물류배부) 전용 지점 셀렉터 옵션 조회.
 *
 * 다른 매출/실적 화면이 여사원 일정 스코프(조직 트리 전체)를 반환하는 것과 달리, 월 매출(물류배부)는
 * 대시보드와 동일한 지점 기준(전사 권한자에게 고정 화이트리스트 34개)을 요구하므로 backend 에서
 * 대시보드 리졸버를 재사용한다.
 */
export function fetchMonthlySalesBranches(): Promise<Branch[]> {
  return fetchBranches('/api/v1/admin/sales/monthly/branches');
}

/** 월 매출(전산실적) 전용 지점 셀렉터 옵션 조회 — 조직 트리 스코프. */
export function fetchElectronicSalesBranches(): Promise<Branch[]> {
  return fetchBranches('/api/v1/admin/sales/electronic/branches');
}

/** POS매출 전용 지점 셀렉터 옵션 조회 — 조직 트리 스코프. */
export function fetchPosSalesBranches(): Promise<Branch[]> {
  return fetchBranches('/api/v1/admin/sales/pos/branches');
}

/** 월별 진열사원 투입적합성 전용 지점 셀렉터 옵션 조회 — 조직 트리 스코프. */
export function fetchInputAdequacyBranches(): Promise<Branch[]> {
  return fetchBranches('/api/v1/admin/sales/input-adequacy/branches');
}

/** 진열사원 배치 적합성 전용 지점 셀렉터 옵션 조회 — 조직 트리 스코프. */
export function fetchDeploymentBranches(): Promise<Branch[]> {
  return fetchBranches('/api/v1/admin/sales/deployment/branches');
}
