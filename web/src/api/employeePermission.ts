import client from './client';
import type { UserRole } from '@/constants/userRole';

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

export interface UserPermissionInfo {
  permission: string;
  granted_by_name: string;
}

export interface EmployeePermissionDetail {
  employee_id: number;
  employee_code: string;
  name: string;
  role: UserRole | null;
  role_label: string | null;
  role_permissions: string[];
  user_permissions: UserPermissionInfo[];
  effective_permissions: string[];
}

export interface UpdateUserPermissionsRequest {
  permissions: string[];
}

export interface UpdateAuthorityRequest {
  role: UserRole;
}

export interface UpdateAuthorityResponse {
  employee_id: number;
  employee_code: string;
  name: string;
  previous_role: UserRole | null;
  previous_role_label: string | null;
  new_role: UserRole;
  new_role_label: string;
  effective_permissions: string[];
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
