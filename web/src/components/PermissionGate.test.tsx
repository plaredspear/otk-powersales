import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from 'antd';
import PermissionGate from './PermissionGate';
import { useAuthStore } from '@/stores/authStore';

function setPermissions(permissions: string[], profileName: string | null = '5.영업사원') {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      profileName,
      isSalesSupport: false,
      costCenterCode: null,
      permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

describe('PermissionGate', () => {
  beforeEach(() => {
    setPermissions([]);
  });

  describe('mode=hide (기본)', () => {
    it('권한 보유 시 children 을 렌더한다', () => {
      setPermissions(['account:E']);
      render(
        <PermissionGate entity="account" operation="EDIT">
          <Button>등록</Button>
        </PermissionGate>,
      );
      expect(screen.getByText('등록')).toBeInTheDocument();
    });

    it('권한 미보유 시 아무것도 렌더하지 않는다', () => {
      render(
        <PermissionGate entity="account" operation="EDIT">
          <Button>등록</Button>
        </PermissionGate>,
      );
      expect(screen.queryByText('등록')).not.toBeInTheDocument();
    });

    it('권한 미보유 시 fallback 을 렌더한다', () => {
      render(
        <PermissionGate entity="account" operation="EDIT" fallback={<span>권한없음</span>}>
          <Button>등록</Button>
        </PermissionGate>,
      );
      expect(screen.getByText('권한없음')).toBeInTheDocument();
    });
  });

  describe('mode=disable', () => {
    it('권한 보유 시 children 을 그대로(활성) 렌더한다', () => {
      setPermissions(['account:D']);
      render(
        <PermissionGate entity="account" operation="DELETE" mode="disable">
          <Button>삭제</Button>
        </PermissionGate>,
      );
      expect(screen.getByRole('button', { name: '삭제' })).not.toBeDisabled();
    });

    it('권한 미보유 시 children 을 disabled 로 렌더한다', () => {
      render(
        <PermissionGate entity="account" operation="DELETE" mode="disable">
          <Button>삭제</Button>
        </PermissionGate>,
      );
      expect(screen.getByRole('button', { name: '삭제' })).toBeDisabled();
    });
  });

  describe('mode=readonly', () => {
    it('권한 미보유 시 자식 antd 컴포넌트를 비활성화한다', () => {
      render(
        <PermissionGate entity="account" operation="EDIT" mode="readonly">
          <Button>수정</Button>
        </PermissionGate>,
      );
      expect(screen.getByRole('button', { name: '수정' })).toBeDisabled();
    });
  });

  describe('시스템 관리자', () => {
    it('권한 set 이 비어도 시스템 관리자는 통과한다', () => {
      setPermissions([], '시스템 관리자');
      render(
        <PermissionGate entity="account" operation="EDIT">
          <Button>등록</Button>
        </PermissionGate>,
      );
      expect(screen.getByText('등록')).toBeInTheDocument();
    });
  });

  describe('systemPermission', () => {
    it('시스템 권한으로 게이팅한다', () => {
      setPermissions(['SYSTEM:MANAGE_USERS']);
      render(
        <PermissionGate systemPermission="MANAGE_USERS">
          <Button>계정 관리</Button>
        </PermissionGate>,
      );
      expect(screen.getByText('계정 관리')).toBeInTheDocument();
    });

    it('시스템 권한 미보유 시 숨긴다', () => {
      render(
        <PermissionGate systemPermission="MANAGE_USERS">
          <Button>계정 관리</Button>
        </PermissionGate>,
      );
      expect(screen.queryByText('계정 관리')).not.toBeInTheDocument();
    });
  });

  describe('가드 미지정', () => {
    it('entity/systemPermission 모두 미지정 시 무조건 통과한다', () => {
      render(
        <PermissionGate>
          <Button>항상표시</Button>
        </PermissionGate>,
      );
      expect(screen.getByText('항상표시')).toBeInTheDocument();
    });
  });
});
