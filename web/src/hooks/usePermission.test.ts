import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { usePermission, SYSTEM_ADMIN_PROFILE_NAME } from './usePermission';
import { useAuthStore } from '@/stores/authStore';

function setUser(opts: { profileName: string | null; permissions: string[] }) {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      profileName: opts.profileName,
      costCenterCode: null,
      permissions: opts.permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

describe('usePermission', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
  });

  it('일반 사용자는 permissions 배열에 포함된 키만 통과한다', () => {
    setUser({ profileName: '5.영업사원', permissions: ['employee:R'] });
    const { result } = renderHook(() => usePermission());

    expect(result.current.hasEntityPermission('employee', 'READ')).toBe(true);
    expect(result.current.hasEntityPermission('employee', 'EDIT')).toBe(false);
    expect(result.current.hasSystemPermission('MANAGE_USERS')).toBe(false);
  });

  it('시스템 관리자는 permissions 배열에 키가 없어도 모든 entity 권한을 통과한다', () => {
    setUser({ profileName: SYSTEM_ADMIN_PROFILE_NAME, permissions: [] });
    const { result } = renderHook(() => usePermission());

    expect(result.current.hasEntityPermission('employee', 'EDIT')).toBe(true);
    expect(result.current.hasEntityPermission('account', 'DELETE')).toBe(true);
  });

  it('시스템 관리자는 SYSTEM:MANAGE_USERS 미주입 상태에서도 system 권한을 통과한다', () => {
    // 회귀 방지 — SF 표준 System Administrator 는 profile_flags 미적재로
    // SfPermissionResolver 가 SYSTEM:MANAGE_USERS 를 못 주입할 수 있다.
    setUser({ profileName: SYSTEM_ADMIN_PROFILE_NAME, permissions: [] });
    const { result } = renderHook(() => usePermission());

    expect(result.current.hasSystemPermission('MANAGE_USERS')).toBe(true);
    expect(result.current.hasSystemPermission('VIEW_ALL_USERS')).toBe(true);
  });

  it('hasAnyEntityPermission 도 시스템 관리자 예외를 따른다', () => {
    setUser({ profileName: SYSTEM_ADMIN_PROFILE_NAME, permissions: [] });
    const { result } = renderHook(() => usePermission());

    expect(result.current.hasAnyEntityPermission('employee', ['EDIT', 'DELETE'])).toBe(true);
  });
});
