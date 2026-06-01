import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { message } from 'antd';
import { refreshToken } from './auth';
import queryClient from '@/lib/queryClient';

/**
 * 모바일앱용 web axios 클라이언트.
 *
 * web/(admin) `api/client.ts` 의 JWT 자동주입 + 401 refresh rotation 패턴을 재사용하되,
 * 토큰 키를 mobile 전용(`mw_*`) 으로 분리해 같은 브라우저/WebView 에서 admin web 과
 * 세션이 충돌하지 않도록 한다. (audience: mobile)
 */
export const TOKEN_KEYS = {
  access: 'mw_accessToken',
  refresh: 'mw_refreshToken',
  user: 'mw_user',
} as const;

const client = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEYS.access);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach((prom) => {
    if (token) prom.resolve(token);
    else prom.reject(error);
  });
  failedQueue = [];
};

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;

    if (status === 403) {
      const data = error.response?.data as { message?: string } | undefined;
      message.warning(data?.message ?? '요청한 데이터에 접근할 권한이 없습니다.');
      return Promise.reject(error);
    }

    if (status && status >= 500 && status <= 504) {
      message.error('서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
      return Promise.reject(error);
    }

    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      message.error('요청 시간이 초과되었습니다.');
      return Promise.reject(error);
    }

    if (!error.response) {
      message.error('네트워크 연결을 확인해 주세요.');
      return Promise.reject(error);
    }

    if (status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return client(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    const storedRefreshToken = localStorage.getItem(TOKEN_KEYS.refresh);
    if (!storedRefreshToken) {
      isRefreshing = false;
      processQueue(error, null);
      handleLogout();
      return Promise.reject(error);
    }

    try {
      const data = await refreshToken(storedRefreshToken);
      localStorage.setItem(TOKEN_KEYS.access, data.accessToken);
      localStorage.setItem(TOKEN_KEYS.refresh, data.refreshToken);
      processQueue(null, data.accessToken);
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      return client(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      handleLogout();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

function handleLogout() {
  queryClient.clear();
  localStorage.removeItem(TOKEN_KEYS.access);
  localStorage.removeItem(TOKEN_KEYS.refresh);
  localStorage.removeItem(TOKEN_KEYS.user);
  window.location.href = '/login';
}

export default client;
