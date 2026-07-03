import axios, { AxiosError } from 'axios';
import type { AppAuthority } from '@/constants/userRole';
import type { ApiResponse } from './types';

interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Backend `WebUserSummary` 응답 매핑 (Spec #760).
 *
 * `id` 는 backend `userId` 필드의 web 별칭. 실제 axios 응답 키는 userId 이므로
 * authStore 에서 변환한다.
 */
export interface AdminUserInfo {
  userId: number;
  username: string;
  name: string | null;
  employeeCode: string;
  profileName: string | null;
  isSalesSupport: boolean;
  role: AppAuthority | null;
  orgName: string | null;
  costCenterCode: string | null;
  permissions: string[];
}

/**
 * Backend `WebLoginResponse` (flat 구조 — token + user 형제 필드).
 */
export interface LoginData {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  passwordChangeRequired: boolean;
  user: AdminUserInfo;
}

interface RefreshData {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export async function login(request: LoginRequest): Promise<LoginData> {
  try {
    const res = await axios.post<ApiResponse<LoginData>>('/api/v1/admin/auth/login', request);
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.error?.message || '로그인에 실패했습니다');
    }
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError) {
      if (err.response) {
        const serverMessage = (err.response.data as ApiResponse<unknown>)?.error?.message;
        if (serverMessage) {
          throw new Error(serverMessage);
        }
        const status = err.response.status;
        if (status === 401) throw new Error('아이디 또는 비밀번호가 올바르지 않습니다');
        if (status === 403) throw new Error('웹 관리자 로그인 권한이 없습니다');
        throw new Error('로그인에 실패했습니다');
      }
      throw new Error('로그인 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.');
    }
    throw err;
  }
}

interface ChangePasswordRequest {
  /** 강제 변경(임시 비밀번호 상태)이면 생략, 자발 변경이면 현재 비밀번호. */
  currentPassword?: string;
  newPassword: string;
}

/**
 * 비밀번호 변경 응답 — 변경 성공 시 backend 가 새 토큰 페어를 함께 발급한다.
 * 기존 access token 클레임의 `password_change_required=true` 가 갱신된 새 토큰으로 교체해야
 * 강제 변경을 재로그인 없이 끝낼 수 있다.
 */
export interface ChangePasswordData {
  passwordChangeRequired: boolean;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * Web 비밀번호 변경 (강제 / 자발 통합, backend `POST /api/v1/admin/auth/password`).
 *
 * 임시 비밀번호 상태(passwordChangeRequired=true)에서는 currentPassword 없이 호출한다.
 * `@/api/client` 는 refreshToken 을 import 해 순환 참조가 되므로, 여기서는 access token 을
 * 직접 첨부하여 순수 axios 로 호출한다 (강제 변경 화면 진입 시점엔 token 이 항상 존재).
 */
export async function changePassword(request: ChangePasswordRequest): Promise<ChangePasswordData> {
  try {
    const accessToken = localStorage.getItem('accessToken');
    const res = await axios.post<ApiResponse<ChangePasswordData>>(
      '/api/v1/admin/auth/password',
      request,
      { headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined },
    );
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.error?.message || '비밀번호 변경에 실패했습니다');
    }
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError) {
      const serverMessage = (err.response?.data as ApiResponse<unknown>)?.error?.message;
      throw new Error(serverMessage || '비밀번호 변경에 실패했습니다');
    }
    throw err;
  }
}

export async function refreshToken(token: string): Promise<RefreshData> {
  const res = await axios.post<ApiResponse<RefreshData>>('/api/v1/admin/auth/refresh', {
    refreshToken: token,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error('토큰 갱신에 실패했습니다');
  }
  return res.data.data;
}
