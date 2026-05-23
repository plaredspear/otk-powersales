import client from '../client';
import type { ApiResponse } from '../types';

/**
 * Spec #803 — 권한 관리 admin 조회 API client.
 */

export interface ProfileSummary {
  profileId: number;
  name: string;
  userType: string | null;
  description: string | null;
  viewAllData: boolean;
  modifyAllData: boolean;
  viewAllUsers: boolean;
  manageUsers: boolean;
  apiEnabled: boolean;
  assignedUserCount: number;
}

export interface ProfileFlagsSummary {
  viewAllData: boolean;
  modifyAllData: boolean;
  viewAllUsers: boolean;
  manageUsers: boolean;
  apiEnabled: boolean;
}

export interface AssignedUserSummary {
  userId: number;
  username: string;
  employeeCode: string | null;
  employeeName: string | null;
}

export interface PaginatedUserList {
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  content: AssignedUserSummary[];
}

export interface ProfileDetail {
  profileId: number;
  name: string;
  userType: string | null;
  description: string | null;
  sfid: string | null;
  flags: ProfileFlagsSummary;
  assignedUsers: PaginatedUserList;
}

export interface PermissionSetSummary {
  permissionSetId: number;
  name: string;
  label: string | null;
  description: string | null;
  permissionSetFlagsId: number | null;
  viewAllData: boolean;
  modifyAllData: boolean;
  objectPermissionCount: number;
  assignedUserCount: number;
}

export interface PermissionSetFlagsSummary {
  permissionSetFlagsId: number;
  viewAllData: boolean;
  modifyAllData: boolean;
}

export interface ObjectPermissionRow {
  sfApiName: string;
  entity: string | null;
  canRead: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

export interface AssignedPermissionSetUserSummary {
  assignmentId: number;
  userId: number;
  username: string;
  employeeCode: string | null;
  employeeName: string | null;
}

export interface PaginatedPermissionSetUserList {
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  content: AssignedPermissionSetUserSummary[];
}

export interface PermissionSetDetail {
  permissionSetId: number;
  name: string;
  label: string | null;
  description: string | null;
  sfid: string | null;
  flags: PermissionSetFlagsSummary | null;
  objectPermissions: ObjectPermissionRow[];
  assignedUsers: PaginatedPermissionSetUserList;
}

export interface PermissionMatrixProfile {
  profileId: number;
  name: string;
}

export interface EntityProfilePermission {
  profileId: number;
  canRead: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

export interface EntityProfileRow {
  entity: string;
  byProfile: EntityProfilePermission[];
}

export interface PermissionMatrix {
  profiles: PermissionMatrixProfile[];
  rows: EntityProfileRow[];
}

export interface FetchProfileParams {
  userPage?: number;
  userSize?: number;
  userKeyword?: string;
}

export async function fetchProfiles(): Promise<ProfileSummary[]> {
  const res = await client.get<ApiResponse<ProfileSummary[]>>('/api/v1/admin/permissions/profiles');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Profile 일람 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchProfile(profileId: number, params: FetchProfileParams = {}): Promise<ProfileDetail> {
  const res = await client.get<ApiResponse<ProfileDetail>>(
    `/api/v1/admin/permissions/profiles/${profileId}`,
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Profile 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPermissionSets(): Promise<PermissionSetSummary[]> {
  const res = await client.get<ApiResponse<PermissionSetSummary[]>>('/api/v1/admin/permissions/permission-sets');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'PermissionSet 일람 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPermissionSet(
  permissionSetId: number,
  params: FetchProfileParams = {},
): Promise<PermissionSetDetail> {
  const res = await client.get<ApiResponse<PermissionSetDetail>>(
    `/api/v1/admin/permissions/permission-sets/${permissionSetId}`,
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'PermissionSet 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPermissionMatrix(): Promise<PermissionMatrix> {
  const res = await client.get<ApiResponse<PermissionMatrix>>('/api/v1/admin/permissions/matrix');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '권한 매트릭스 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * Spec #804 — Assignment 부여/회수.
 */

export interface AssignmentCreateRequest {
  userId: number;
  permissionSetFlagsId: number;
}

export interface AssignmentResponse {
  assignmentId: number;
  userId: number;
  permissionSetFlagsId: number;
  isActive: boolean;
  assignedAt: string | null;
  createdById: number | null;
}

export interface AssignmentBatchRequest {
  userId?: number;
  permissionSetFlagsId?: number;
  userIds?: number[];
  permissionSetFlagsIds?: number[];
}

export interface AssignmentBatchItem {
  userId: number;
  permissionSetFlagsId: number;
  assignmentId?: number;
  reason?: string;
}

export interface AssignmentBatchResult {
  succeeded: AssignmentBatchItem[];
  skipped: AssignmentBatchItem[];
  failed: AssignmentBatchItem[];
}

export async function createAssignment(req: AssignmentCreateRequest): Promise<AssignmentResponse> {
  const res = await client.post<ApiResponse<AssignmentResponse>>('/api/v1/admin/permissions/assignments', req);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '부여에 실패했습니다');
  }
  return res.data.data;
}

export async function revokeAssignment(assignmentId: number): Promise<void> {
  await client.delete(`/api/v1/admin/permissions/assignments/${assignmentId}`);
}

export async function createAssignmentBatch(req: AssignmentBatchRequest): Promise<AssignmentBatchResult> {
  const res = await client.post<ApiResponse<AssignmentBatchResult>>('/api/v1/admin/permissions/assignments/batch', req);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일괄 부여에 실패했습니다');
  }
  return res.data.data;
}
