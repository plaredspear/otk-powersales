import client from './client';
import type { AdminUserInfo } from './auth';
import type { ApiResponse } from './types';

/**
 * 대행 로그인 API (Spec #851).
 *
 * 시스템 관리자(`SYSTEM:MANAGE_USERS`)가 다른 Web 사용자 계정을 대행한다.
 * start/stop 응답으로 받은 토큰 쌍으로 인증을 교체한다 (authStore.applyAuth).
 */

export interface ImpersonationStartRequest {
  targetUserId: number;
  reason?: string;
}

export interface ImpersonationMeta {
  impersonatedByUserId: number;
  impersonatedByName: string | null;
  targetUserId: number;
  targetName: string | null;
  startedAt: string;
}

export interface ImpersonationStartData {
  accessToken: string;
  refreshToken: string;
  /** access 토큰 만료(초). 현재 소비처 없음 — backend 응답 계약 정합 목적으로 유지. */
  expiresIn: number;
  impersonation: ImpersonationMeta;
  user: AdminUserInfo;
}

export interface ImpersonationStopData {
  accessToken: string;
  refreshToken: string;
  /** access 토큰 만료(초). 현재 소비처 없음 — backend 응답 계약 정합 목적으로 유지. */
  expiresIn: number;
  user: AdminUserInfo;
}

export async function startImpersonation(
  request: ImpersonationStartRequest,
): Promise<ImpersonationStartData> {
  const res = await client.post<ApiResponse<ImpersonationStartData>>(
    '/api/v1/admin/impersonation/start',
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message ?? '대행 로그인에 실패했습니다');
  }
  return res.data.data;
}

export async function stopImpersonation(): Promise<ImpersonationStopData> {
  const res = await client.post<ApiResponse<ImpersonationStopData>>(
    '/api/v1/admin/impersonation/stop',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message ?? '대행 종료에 실패했습니다');
  }
  return res.data.data;
}
