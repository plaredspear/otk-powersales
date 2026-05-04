import { create } from 'zustand';
import { login as loginApi } from '@/api/auth';
import type { UserRole } from '@/constants/userRole';

export interface AuthUser {
  id: number;
  employeeCode: string;
  name: string;
  orgName: string | null;
  role: UserRole | null;
  roleLabel: string | null;
  costCenterCode: string | null;
  permissions: string[];
}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (employeeCode: string, password: string) => Promise<void>;
  logout: () => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,

  login: async (employeeCode: string, password: string) => {
    const data = await loginApi({ employeeCode: employeeCode, password });

    const user: AuthUser = {
      id: data.user.id,
      employeeCode: data.user.employeeCode,
      name: data.user.name,
      orgName: data.user.orgName,
      role: data.user.role,
      roleLabel: data.user.roleLabel,
      costCenterCode: data.user.costCenterCode,
      permissions: data.user.permissions ?? [],
    };

    localStorage.setItem('accessToken', data.token.accessToken);
    localStorage.setItem('refreshToken', data.token.refreshToken);
    localStorage.setItem('user', JSON.stringify(user));

    set({
      user,
      accessToken: data.token.accessToken,
      isAuthenticated: true,
    });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    set({ user: null, accessToken: null, isAuthenticated: false });
  },

  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    set({ accessToken });
  },

  initialize: () => {
    const accessToken = localStorage.getItem('accessToken');
    const userStr = localStorage.getItem('user');

    if (accessToken && userStr) {
      try {
        const user = JSON.parse(userStr) as AuthUser;
        // Spec #573: 신규 스키마 검증 — `role` 필드 미존재 시 구 캐시로 간주하고 재로그인 유도
        if (typeof user.role === 'undefined') {
          throw new Error('legacy auth cache');
        }
        set({ user, accessToken, isAuthenticated: true });
      } catch {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
      }
    }
  },
}));
