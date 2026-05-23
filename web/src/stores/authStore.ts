import { create } from 'zustand';
import { login as loginApi } from '@/api/auth';
import type { AppAuthority } from '@/constants/userRole';

/**
 * Web Admin 인증 사용자 (Spec #760).
 *
 * Backend `WebUserSummary` (userId/username/employeeCode) 응답을 web 의 기존 소비 코드
 * 컨벤션(id/employeeCode) 에 맞춰 매핑한 형태. token 은 별도 localStorage 키로 분리 관리.
 */
export interface AuthUser {
  id: number;
  employeeCode: string;
  username: string;
  name: string;
  orgName: string | null;
  /** SF DKRetail__AppAuthority__c picklist value 또는 null. picklist value 자체가 한글 label. */
  role: AppAuthority | null;
  /** SF Profile.Name (시스템 관리자 / 5.영업사원 / 4.지점장 등). 라우터 가드 입력. */
  profileName?: string | null;
  costCenterCode: string | null;
  permissions: string[];
}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,

  login: async (username: string, password: string) => {
    const data = await loginApi({ username, password });

    const user: AuthUser = {
      id: data.user.userId,
      employeeCode: data.user.employeeCode,
      username: data.user.username,
      name: data.user.name ?? '',
      orgName: data.user.orgName,
      role: data.user.role,
      profileName: data.user.profileName,
      costCenterCode: data.user.costCenterCode,
      permissions: data.user.permissions ?? [],
    };

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(user));

    set({
      user,
      accessToken: data.accessToken,
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
