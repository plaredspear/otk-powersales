import client from './client';
import { unwrap, type ApiResponse } from './types';

/**
 * 비밀번호 검증/변경 — backend `/api/v1/mobile/auth/*` (인증 필요 → client 사용).
 * 로그인/리프레시(auth.ts)는 인터셉터 순환을 피해 raw axios 를 쓰지만, 이 두 호출은
 * 인증된 요청이므로 토큰 주입이 필요해 client 를 쓴다.
 */

/** 자발 변경 1단계 — 현재 비밀번호 확인. */
export async function verifyPassword(currentPassword: string): Promise<void> {
  const res = await client.post<ApiResponse<unknown>>('/api/v1/mobile/auth/verify-password', {
    currentPassword,
  });
  if (!res.data.success) {
    throw new Error(res.data.error?.message || res.data.message || '비밀번호 확인에 실패했습니다');
  }
}

export interface ChangePasswordResult {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * 비밀번호 변경 (강제/자발 통합 — backend Spec #584).
 * - 강제(passwordChangeRequired): currentPassword 생략
 * - 자발: currentPassword 필수
 * 응답으로 새 토큰 페어를 받아 호출자가 갱신한다.
 */
export async function changePassword(params: {
  newPassword: string;
  currentPassword?: string;
}): Promise<ChangePasswordResult> {
  const res = await client.post<ApiResponse<ChangePasswordResult>>(
    '/api/v1/mobile/auth/change-password',
    params
  );
  return unwrap(res, '비밀번호 변경에 실패했습니다');
}
