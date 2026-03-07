import axios from 'axios';

interface LoginRequest {
  employee_id: string;
  password: string;
}

interface AdminUserInfo {
  id: number;
  employee_id: string;
  name: string;
  org_name: string | null;
  role: string;
  app_authority: string | null;
  cost_center_code: string | null;
}

interface AdminTokenInfo {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

interface LoginData {
  user: AdminUserInfo;
  token: AdminTokenInfo;
}

interface RefreshData {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error?: { code: string; message: string };
  message?: string;
}

export async function login(request: LoginRequest): Promise<LoginData> {
  const res = await axios.post<ApiResponse<LoginData>>('/api/v1/admin/auth/login', request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || '로그인에 실패했습니다');
  }
  return res.data.data;
}

export async function refreshToken(token: string): Promise<RefreshData> {
  const res = await axios.post<ApiResponse<RefreshData>>('/api/v1/admin/auth/refresh', {
    refresh_token: token,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error('토큰 갱신에 실패했습니다');
  }
  return res.data.data;
}
