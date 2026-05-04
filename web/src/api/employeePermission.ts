import client from './client';
import type { UserRole } from '@/constants/userRole';
import type { ApiResponse } from './types';

export interface UserPermissionInfo {
  permission: string;
  grantedByName: string;
}

export interface EmployeePermissionDetail {
  employeeId: number;
  employeeCode: string;
  name: string;
  role: UserRole | null;
  roleLabel: string | null;
  rolePermissions: string[];
  userPermissions: UserPermissionInfo[];
  effectivePermissions: string[];
}

export interface UpdateUserPermissionsRequest {
  permissions: string[];
}

export interface UpdateAuthorityRequest {
  role: UserRole;
}

export interface UpdateAuthorityResponse {
  employeeId: number;
  employeeCode: string;
  name: string;
  previousRole: UserRole | null;
  previousRoleLabel: string | null;
  newRole: UserRole;
  newRoleLabel: string;
  effectivePermissions: string[];
}

export async function fetchEmployeePermissions(
  employeeId: number,
): Promise<EmployeePermissionDetail> {
  const res = await client.get<ApiResponse<EmployeePermissionDetail>>(
    `/api/v1/admin/employees/${employeeId}/permissions`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error('사원 권한 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function updateUserPermissions(
  employeeId: number,
  request: UpdateUserPermissionsRequest,
): Promise<EmployeePermissionDetail> {
  const res = await client.put<ApiResponse<EmployeePermissionDetail>>(
    `/api/v1/admin/employees/${employeeId}/permissions`,
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error('사원 권한 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function updateEmployeeAuthority(
  employeeId: number,
  request: UpdateAuthorityRequest,
): Promise<UpdateAuthorityResponse> {
  const res = await client.put<ApiResponse<UpdateAuthorityResponse>>(
    `/api/v1/admin/employees/${employeeId}/authority`,
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error('역할 변경에 실패했습니다');
  }
  return res.data.data;
}
