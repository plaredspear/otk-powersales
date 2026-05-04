import axios, { AxiosError } from 'axios';
import type { UserRole } from '@/constants/userRole';
import type { ApiResponse } from './types';

interface LoginRequest {
  employeeCode: string;
  password: string;
}

interface AdminUserInfo {
  id: number;
  employeeCode: string;
  name: string;
  orgName: string | null;
  role: UserRole | null;
  roleLabel: string | null;
  costCenterCode: string | null;
  permissions: string[];
}

interface AdminTokenInfo {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

interface LoginData {
  user: AdminUserInfo;
  token: AdminTokenInfo;
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
        if (status === 401) throw new Error('사번 또는 비밀번호가 올바르지 않습니다');
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
