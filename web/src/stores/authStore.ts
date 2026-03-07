import { create } from 'zustand';
import { login as loginApi } from '@/api/auth';

export interface AuthUser {
  id: number;
  employeeId: string;
  name: string;
  orgName: string | null;
  role: string;
}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (employeeId: string, password: string) => Promise<void>;
  logout: () => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,

  login: async (employeeId: string, password: string) => {
    const data = await loginApi({ employee_id: employeeId, password });

    const user: AuthUser = {
      id: data.user.id,
      employeeId: data.user.employee_id,
      name: data.user.name,
      orgName: data.user.org_name,
      role: data.user.role,
    };

    localStorage.setItem('accessToken', data.token.access_token);
    localStorage.setItem('refreshToken', data.token.refresh_token);
    localStorage.setItem('user', JSON.stringify(user));

    set({
      user,
      accessToken: data.token.access_token,
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
        set({ user, accessToken, isAuthenticated: true });
      } catch {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
      }
    }
  },
}));
