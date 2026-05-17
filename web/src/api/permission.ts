import client from './client';
import type { ApiResponse } from './types';

export interface PermissionDetail {
  code: string;
  description: string;
  menus: string[];
}

export interface RolePermissions {
  role: string;
  roleLabel: string;
  permissions: string[];
}

export interface CurrentUserPermission {
  role: string;
  roleLabel: string;
  permissions: string[];
  canManagePermissions: boolean;
}

export interface PermissionMatrixData {
  permissions: PermissionDetail[];
  roles: RolePermissions[];
  currentUser: CurrentUserPermission;
}

export interface UpdateRolePermissionsRequest {
  permissions: string[];
}

export async function updateRolePermissions(
  role: string,
  request: UpdateRolePermissionsRequest,
): Promise<{ role: string; permissions: string[] }> {
  const res = await client.put<
    ApiResponse<{ role: string; permissions: string[] }>
  >(`/api/v1/admin/permissions/roles/${encodeURIComponent(role)}`, request);
  if (!res.data.success || !res.data.data) {
    throw new Error('역할 권한 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPermissionMatrix(): Promise<PermissionMatrixData> {
  const res = await client.get<ApiResponse<PermissionMatrixData>>(
    '/api/v1/admin/permissions/matrix',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error('권한 매트릭스 조회에 실패했습니다');
  }
  return res.data.data;
}
