import axios, { AxiosError } from 'axios';
import type { UserRole } from '@/constants/userRole';
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
  role: UserRole | null;
  roleLabel: string | null;
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

export async function refreshToken(token: string): Promise<RefreshData> {
  const res = await axios.post<ApiResponse<RefreshData>>('/api/v1/admin/auth/refresh', {
    refreshToken: token,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error('토큰 갱신에 실패했습니다');
  }
  return res.data.data;
}
