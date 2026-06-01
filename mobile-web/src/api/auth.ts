import axios, { AxiosError } from 'axios';
import type { ApiResponse } from './types';

/**
 * 모바일 인증 API — backend `/api/v1/mobile/auth/*`.
 *
 * 계약: `AuthController` + `LoginRequest`/`LoginResponse`/`TokenResponse`.
 * client.ts 의 인터셉터 순환 의존을 피하려고 refresh/login 은 순수 axios 로 호출한다.
 */

export interface LoginRequest {
  /** 8자리 사번 또는 ADMIN-* */
  employeeCode: string;
  password: string;
  deviceId?: string;
}

export interface MobileUserInfo {
  id: number;
  employeeCode: string;
  name: string;
  orgName: string | null;
  /** SF AppAuthority picklist value (여사원/조장/지점장/...) 또는 null */
  role: string | null;
}

export interface LoginResponse {
  user: MobileUserInfo;
  token: {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
  };
  passwordChangeRequired: boolean;
  requiresGpsConsent: boolean;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  try {
    const res = await axios.post<ApiResponse<LoginResponse>>('/api/v1/mobile/auth/login', request);
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.error?.message || '로그인에 실패했습니다');
    }
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError && err.response) {
      const serverMessage = (err.response.data as ApiResponse<unknown>)?.error?.message;
      if (serverMessage) throw new Error(serverMessage);
      if (err.response.status === 401) throw new Error('사번 또는 비밀번호가 올바르지 않습니다');
    }
    throw err instanceof Error ? err : new Error('로그인 중 오류가 발생했습니다');
  }
}

export async function refreshToken(token: string): Promise<TokenResponse> {
  const res = await axios.post<ApiResponse<TokenResponse>>('/api/v1/mobile/auth/refresh', {
    refreshToken: token,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error('토큰 갱신에 실패했습니다');
  }
  return res.data.data;
}
