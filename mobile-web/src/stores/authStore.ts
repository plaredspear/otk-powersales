import { create } from 'zustand';
import { login as loginApi, type MobileUserInfo } from '@/api/auth';
import { TOKEN_KEYS } from '@/api/client';

/**
 * 모바일앱용 web 인증 스토어.
 *
 * web/(admin) `authStore` 패턴을 따르되 토큰 키를 mobile 전용으로 분리(TOKEN_KEYS).
 * 강제 비밀번호 변경/GPS 동의 가드 플래그는 Wave 2/3 에서 라우팅에 연결한다(현재는 보관만).
 */
export interface AuthUser extends MobileUserInfo {}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  passwordChangeRequired: boolean;
  requiresGpsConsent: boolean;
  login: (employeeCode: string, password: string) => Promise<void>;
  logout: () => void;
  /** 비밀번호 변경 성공 후 새 토큰 반영 + passwordChangeRequired 해제. */
  applyTokens: (accessToken: string, refreshToken: string) => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  passwordChangeRequired: false,
  requiresGpsConsent: false,

  login: async (employeeCode, password) => {
    const data = await loginApi({ employeeCode, password });
    localStorage.setItem(TOKEN_KEYS.access, data.token.accessToken);
    localStorage.setItem(TOKEN_KEYS.refresh, data.token.refreshToken);
    localStorage.setItem(TOKEN_KEYS.user, JSON.stringify(data.user));
    set({
      user: data.user,
      accessToken: data.token.accessToken,
      isAuthenticated: true,
      passwordChangeRequired: data.passwordChangeRequired,
      requiresGpsConsent: data.requiresGpsConsent,
    });
  },

  logout: () => {
    localStorage.removeItem(TOKEN_KEYS.access);
    localStorage.removeItem(TOKEN_KEYS.refresh);
    localStorage.removeItem(TOKEN_KEYS.user);
    set({ user: null, accessToken: null, isAuthenticated: false });
  },

  applyTokens: (accessToken, refreshToken) => {
    localStorage.setItem(TOKEN_KEYS.access, accessToken);
    localStorage.setItem(TOKEN_KEYS.refresh, refreshToken);
    set({ accessToken, passwordChangeRequired: false });
  },

  initialize: () => {
    const accessToken = localStorage.getItem(TOKEN_KEYS.access);
    const userStr = localStorage.getItem(TOKEN_KEYS.user);
    if (accessToken && userStr) {
      try {
        const user = JSON.parse(userStr) as AuthUser;
        set({ user, accessToken, isAuthenticated: true });
      } catch {
        localStorage.removeItem(TOKEN_KEYS.access);
        localStorage.removeItem(TOKEN_KEYS.refresh);
        localStorage.removeItem(TOKEN_KEYS.user);
      }
    }
  },
}));
