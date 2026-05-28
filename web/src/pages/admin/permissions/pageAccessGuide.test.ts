import { describe, it, expect } from 'vitest';
import { buildRows } from './pageAccessGuide';
import type { MenuRoute } from '@/config/menuConfig';
import type {
  PermissionMatrix,
  PermissionSetMatrix,
  PermissionSetSummary,
  ProfileSummary,
} from '@/api/admin/permission';

function makeMenu(): MenuRoute {
  return {
    path: '/',
    children: [
      {
        name: '인사/근무',
        children: [
          { path: '/employee', name: '여사원 현황', entity: 'employee', operation: 'READ' },
        ],
      },
      {
        name: '시스템',
        children: [
          { path: '/admin/permissions/matrix', name: '권한 매트릭스', systemPermission: 'VIEW_ALL_DATA' },
          { path: '/admin/permissions/guide', name: '권한 사용 가이드', allowedProfileNames: ['시스템 관리자'] },
        ],
      },
      {
        name: '알림/교육',
        children: [
          { path: '/notices', name: '공지사항' }, // 권한 메타 미지정 — open
        ],
      },
    ],
  };
}

function makeProfiles(): ProfileSummary[] {
  return [
    {
      profileId: 1,
      name: '시스템 관리자',
      userType: 'Standard',
      description: null,
      viewAllData: true,
      modifyAllData: true,
      viewAllUsers: true,
      manageUsers: true,
      apiEnabled: true,
      assignedUserCount: 1,
    },
    {
      profileId: 2,
      name: 'CEO',
      userType: 'Standard',
      description: null,
      viewAllData: true,
      modifyAllData: false,
      viewAllUsers: true,
      manageUsers: false,
      apiEnabled: true,
      assignedUserCount: 1,
    },
    {
      profileId: 3,
      name: '5.영업사원',
      userType: 'Standard',
      description: null,
      viewAllData: false,
      modifyAllData: false,
      viewAllUsers: false,
      manageUsers: false,
      apiEnabled: false,
      assignedUserCount: 50,
    },
  ];
}

function makePermissionSets(): PermissionSetSummary[] {
  return []; // 본 테스트는 PS Summary 직접 사용 안 함 (matrix 만)
}

function makeProfileMatrix(): PermissionMatrix {
  return {
    profiles: [
      { profileId: 1, name: '시스템 관리자' },
      { profileId: 2, name: 'CEO' },
      { profileId: 3, name: '5.영업사원' },
    ],
    rows: [
      {
        entity: 'employee',
        byProfile: [
          { profileId: 1, canRead: true, canCreate: true, canEdit: true, canDelete: true },
          { profileId: 2, canRead: true, canCreate: false, canEdit: false, canDelete: false },
          { profileId: 3, canRead: false, canCreate: false, canEdit: false, canDelete: false },
        ],
      },
    ],
  };
}

function makePermissionSetMatrix(): PermissionSetMatrix {
  return {
    permissionSets: [
      {
        permissionSetId: 100,
        name: 'EmployeeReadOnly',
        label: '사원 조회 PS',
        viewAllData: false,
        modifyAllData: false,
        objectPermissions: [
          { sfApiName: 'Employee__c', entity: 'employee', canRead: true, canCreate: false, canEdit: false, canDelete: false },
        ],
      },
      {
        permissionSetId: 101,
        name: 'SystemViewAll',
        label: null,
        viewAllData: true,
        modifyAllData: false,
        objectPermissions: [],
      },
    ],
  };
}

describe('buildRows', () => {
  it('entity 요구 — Profile + PS 둘 다 매칭', () => {
    const rows = buildRows({
      menu: makeMenu(),
      profiles: makeProfiles(),
      permissionSets: makePermissionSets(),
      profileMatrix: makeProfileMatrix(),
      permissionSetMatrix: makePermissionSetMatrix(),
    });
    const employee = rows.find((r) => r.path === '/employee')!;
    expect(employee.requirementKind).toBe('entity');
    expect(employee.requirementLabel).toBe('사원 (employee, READ/조회)');
    expect(employee.satisfyingProfiles).toEqual([
      { profileId: 1, name: '시스템 관리자' },
      { profileId: 2, name: 'CEO' },
    ]);
    expect(employee.satisfyingPermissionSets).toEqual([
      { permissionSetId: 100, name: 'EmployeeReadOnly', label: '사원 조회 PS' },
    ]);
  });

  it('system 요구 (VIEW_ALL_DATA) — Profile + PS 매칭 (viewAllData flag)', () => {
    const rows = buildRows({
      menu: makeMenu(),
      profiles: makeProfiles(),
      permissionSets: makePermissionSets(),
      profileMatrix: makeProfileMatrix(),
      permissionSetMatrix: makePermissionSetMatrix(),
    });
    const matrix = rows.find((r) => r.path === '/admin/permissions/matrix')!;
    expect(matrix.requirementKind).toBe('system');
    expect(matrix.requirementLabel).toBe('전체 데이터 조회 (VIEW_ALL_DATA)');
    expect(matrix.satisfyingProfiles.map((p) => p.name).sort()).toEqual(['CEO', '시스템 관리자']);
    expect(matrix.satisfyingPermissionSets).toEqual([
      { permissionSetId: 101, name: 'SystemViewAll', label: null },
    ]);
  });

  it('profileName 요구 — Profile 만 매칭, PS 는 빈 배열', () => {
    const rows = buildRows({
      menu: makeMenu(),
      profiles: makeProfiles(),
      permissionSets: makePermissionSets(),
      profileMatrix: makeProfileMatrix(),
      permissionSetMatrix: makePermissionSetMatrix(),
    });
    const guide = rows.find((r) => r.path === '/admin/permissions/guide')!;
    expect(guide.requirementKind).toBe('profileName');
    expect(guide.requirementLabel).toBe('특정 Profile 만 접근: [시스템 관리자]');
    expect(guide.satisfyingProfiles).toEqual([{ profileId: 1, name: '시스템 관리자' }]);
    expect(guide.satisfyingPermissionSets).toEqual([]);
  });

  it('open 요구 — Profile / PS 둘 다 빈 배열', () => {
    const rows = buildRows({
      menu: makeMenu(),
      profiles: makeProfiles(),
      permissionSets: makePermissionSets(),
      profileMatrix: makeProfileMatrix(),
      permissionSetMatrix: makePermissionSetMatrix(),
    });
    const notices = rows.find((r) => r.path === '/notices')!;
    expect(notices.requirementKind).toBe('open');
    expect(notices.requirementLabel).toBe('(로그인 사용자 전체)');
    expect(notices.satisfyingProfiles).toEqual([]);
    expect(notices.satisfyingPermissionSets).toEqual([]);
  });

  it('entity 요구이지만 profileMatrix.rows 에 entity 부재 — 빈 매칭', () => {
    const rows = buildRows({
      menu: {
        path: '/',
        children: [
          {
            name: '카테고리',
            children: [
              { path: '/x', name: 'X', entity: 'unknown_entity', operation: 'READ' },
            ],
          },
        ],
      },
      profiles: makeProfiles(),
      permissionSets: makePermissionSets(),
      profileMatrix: makeProfileMatrix(),
      permissionSetMatrix: makePermissionSetMatrix(),
    });
    const row = rows[0];
    expect(row.satisfyingProfiles).toEqual([]);
    expect(row.satisfyingPermissionSets).toEqual([]);
    // 라벨 fallback: unknown_entity 가 그대로 노출
    expect(row.requirementLabel).toBe('unknown_entity (unknown_entity, READ/조회)');
  });

  it('system 요구 중 PS 가 지원 못하는 권한 (MANAGE_USERS) — Profile 만 매칭, PS 는 빈 배열', () => {
    const rows = buildRows({
      menu: {
        path: '/',
        children: [
          {
            name: '카테고리',
            children: [
              { path: '/x', name: 'X', systemPermission: 'MANAGE_USERS' },
            ],
          },
        ],
      },
      profiles: makeProfiles(),
      permissionSets: makePermissionSets(),
      profileMatrix: makeProfileMatrix(),
      permissionSetMatrix: makePermissionSetMatrix(),
    });
    const row = rows[0];
    expect(row.satisfyingProfiles).toEqual([{ profileId: 1, name: '시스템 관리자' }]);
    expect(row.satisfyingPermissionSets).toEqual([]);
  });
});
