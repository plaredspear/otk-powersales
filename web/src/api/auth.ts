import axios from 'axios';

interface LoginRequest {
  employee_id: string;
  password: string;
}

interface UserInfo {
  id: number;
  employee_id: string;
  name: string;
  org_name: string | null;
  role: string;
}

interface TokenInfo {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

interface LoginData {
  user: UserInfo;
  token: TokenInfo;
  requires_password_change: boolean;
  requires_gps_consent: boolean;
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
  const res = await axios.post<ApiResponse<LoginData>>('/api/v1/auth/login', request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || '로그인에 실패했습니다');
  }
  return res.data.data;
}

export async function refreshToken(token: string): Promise<RefreshData> {
  const res = await axios.post<ApiResponse<RefreshData>>('/api/v1/auth/refresh', {
    refresh_token: token,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error('토큰 갱신에 실패했습니다');
  }
  return res.data.data;
}
