import { create } from 'zustand';
import { login as loginApi, type AdminUserInfo } from '@/api/auth';
import type { AppAuthority } from '@/constants/userRole';
import { IMPERSONATION_STORAGE_KEY } from '@/constants/storageKeys';

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

/**
 * 대행 로그인 상태 (Spec #851). null = 대행 아님.
 *
 * `targetName` 은 대행 중인 대상 사용자, `byName` 은 복귀 대상 관리자.
 */
export interface ImpersonationState {
  byUserId: number;
  byName: string;
  targetUserId: number;
  targetName: string;
  startedAt: string;
}

const IMPERSONATION_KEY = IMPERSONATION_STORAGE_KEY;

function toAuthUser(info: AdminUserInfo): AuthUser {
  return {
    id: info.userId,
    employeeCode: info.employeeCode,
    username: info.username,
    name: info.name ?? '',
    orgName: info.orgName,
    role: info.role,
    profileName: info.profileName,
    costCenterCode: info.costCenterCode,
    permissions: info.permissions ?? [],
  };
}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  /** 대행 중이면 메타, 아니면 null. */
  impersonation: ImpersonationState | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  /**
   * 토큰 + 사용자 전체 교체 (대행 시작/종료 시, Spec #851).
   * `impersonation` 이 주어지면 대행 시작, null 이면 대행 종료(관리자 복귀).
   */
  applyAuth: (
    tokens: { accessToken: string; refreshToken: string },
    userInfo: AdminUserInfo,
    impersonation: ImpersonationState | null,
  ) => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  impersonation: null,

  login: async (username: string, password: string) => {
    const data = await loginApi({ username, password });

    const user = toAuthUser(data.user);

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.removeItem(IMPERSONATION_KEY);

    set({
      user,
      accessToken: data.accessToken,
      isAuthenticated: true,
      impersonation: null,
    });
  },

  applyAuth: (tokens, userInfo, impersonation) => {
    const user = toAuthUser(userInfo);
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    if (impersonation) {
      localStorage.setItem(IMPERSONATION_KEY, JSON.stringify(impersonation));
    } else {
      localStorage.removeItem(IMPERSONATION_KEY);
    }
    set({
      user,
      accessToken: tokens.accessToken,
      isAuthenticated: true,
      impersonation,
    });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem(IMPERSONATION_KEY);
    set({ user: null, accessToken: null, isAuthenticated: false, impersonation: null });
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
        // Spec #851: 대행 상태 복원 (새로고침 후에도 대행 배너 유지)
        let impersonation: ImpersonationState | null = null;
        const impStr = localStorage.getItem(IMPERSONATION_KEY);
        if (impStr) {
          try {
            impersonation = JSON.parse(impStr) as ImpersonationState;
          } catch {
            localStorage.removeItem(IMPERSONATION_KEY);
          }
        }
        set({ user, accessToken, isAuthenticated: true, impersonation });
      } catch {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        localStorage.removeItem(IMPERSONATION_KEY);
      }
    }
  },
}));
