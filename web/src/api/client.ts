import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { notification } from 'antd';
import { refreshToken } from './auth';
import queryClient from '@/lib/queryClient';

const client = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// Request interceptor: attach access token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: auto-refresh on 401
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach((prom) => {
    if (token) {
      prom.resolve(token);
    } else {
      prom.reject(error);
    }
  });
  failedQueue = [];
};

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;

    // 403: 권한 없음 — notification 표시 후 에러 전파 (로그아웃 안 함)
    if (status === 403) {
      notification.error({
        message: '접근 권한 없음',
        description: '해당 기능에 대한 접근 권한이 없습니다. 관리자에게 문의하세요.',
      });
      return Promise.reject(error);
    }

    // 500번대: 서버 오류
    if (status && status >= 500 && status <= 504) {
      notification.error({
        key: 'api-server-error',
        message: '서버 오류',
        description: '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.',
        duration: 5,
      });
      return Promise.reject(error);
    }

    // 타임아웃
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      notification.error({
        key: 'api-timeout-error',
        message: '요청 시간 초과',
        description: '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.',
        duration: 5,
      });
      return Promise.reject(error);
    }

    // 네트워크 에러 (응답 없음)
    if (!error.response) {
      notification.error({
        key: 'api-network-error',
        message: '네트워크 오류',
        description: '네트워크 연결을 확인해 주세요.',
        duration: 5,
      });
      return Promise.reject(error);
    }

    // 401 이외의 에러는 그대로 전파
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

    const storedRefreshToken = localStorage.getItem('refreshToken');
    if (!storedRefreshToken) {
      isRefreshing = false;
      processQueue(error, null);
      handleLogout();
      return Promise.reject(error);
    }

    try {
      const data = await refreshToken(storedRefreshToken);
      localStorage.setItem('accessToken', data.access_token);
      localStorage.setItem('refreshToken', data.refresh_token);
      processQueue(null, data.access_token);
      originalRequest.headers.Authorization = `Bearer ${data.access_token}`;
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
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  window.location.href = '/login';
}

export default client;
