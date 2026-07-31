import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 지점 스코프 방식 관리 API (개발자 도구 > 대시보드 > 지점 스코프 방식).
 *
 * 백엔드 `GET/POST /api/v1/admin/tools/branch-scope-mode` 호출 — 투입현황 대시보드의 지점
 * 판정/확장 방식을 통합 리졸버(UNIFIED)와 전환 이전 동작(LEGACY) 사이에서 런타임 전환한다.
 * 신/구 수치 비교를 위한 **한시적** 스위치이며, 검증이 끝나면 토글과 함께 제거한다.
 * 상태는 Redis 에 지속 저장되며 기본값은 UNIFIED. 권한: 시스템 관리자 전용 (백엔드 가드).
 */

export type BranchScopeMode = 'UNIFIED' | 'LEGACY';

export interface BranchScopeModeResponse {
  mode: BranchScopeMode;
}

export async function fetchBranchScopeMode(): Promise<BranchScopeModeResponse> {
  const res = await client.get<ApiResponse<BranchScopeModeResponse>>(
    '/api/v1/admin/tools/branch-scope-mode',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 스코프 방식 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function updateBranchScopeMode(mode: BranchScopeMode): Promise<BranchScopeModeResponse> {
  const res = await client.post<ApiResponse<BranchScopeModeResponse>>(
    '/api/v1/admin/tools/branch-scope-mode',
    { mode },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 스코프 방식 변경에 실패했습니다');
  }
  return res.data.data;
}
