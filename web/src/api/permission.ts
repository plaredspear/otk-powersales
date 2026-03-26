import client from './client';

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

export interface PermissionDetail {
  code: string;
  description: string;
  menus: string[];
}

export interface RolePermissions {
  role: string;
  permissions: string[];
}

export interface CurrentUserPermission {
  role: string;
  permissions: string[];
}

export interface PermissionMatrixData {
  permissions: PermissionDetail[];
  roles: RolePermissions[];
  current_user: CurrentUserPermission;
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
