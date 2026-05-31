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
  flags: ProfileFlagsSummary;
  /** SF 객체권한 (편집 화면 현재값). PermissionSetDetail 과 동일 행 형식. */
  objectPermissions: ObjectPermissionRow[];
  /** @PermissionResource 가상 자원 권한. */
  customPermissions: CustomPermissionRow[];
  /** Web admin 에서 Profile 권한이 수정되었는지 (dirty). */
  isLocallyModified: boolean;
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
  /** Spec #837 — SF 출처 PS 여부 (sfid IS NOT NULL). 목록 "출처" 컬럼 / 삭제 차단 판정용. */
  sfOrigin: boolean;
  /** Spec #837 — dirty 플래그. 신규 시스템에서 수정된 SF 출처 PS 표시. */
  isLocallyModified: boolean;
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

/** Spec #808 — @PermissionResource 가상 자원의 CRUD 비트. */
export interface CustomPermissionRow {
  resource: string;
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
  flags: PermissionSetFlagsSummary | null;
  objectPermissions: ObjectPermissionRow[];
  customPermissions: CustomPermissionRow[];
  assignedUsers: PaginatedPermissionSetUserList;
  /** Spec #837 — SF 출처 PS 여부. 삭제 버튼 비활성화 / dirty 배지 표시 판정. */
  sfOrigin: boolean;
  /** Spec #837 — dirty 플래그. */
  isLocallyModified: boolean;
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

/**
 * "페이지별 필요 권한" 가이드 페이지가 사용. PermissionSet 일람 + 각 PS 의 시스템 권한 flag
 * + entity 객체권한 매트릭스 한 번에 반환.
 */
export interface PermissionSetMatrixEntry {
  permissionSetId: number;
  name: string;
  label: string | null;
  viewAllData: boolean;
  modifyAllData: boolean;
  objectPermissions: ObjectPermissionRow[];
}

export interface PermissionSetMatrix {
  permissionSets: PermissionSetMatrixEntry[];
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

export async function fetchPermissionSetMatrix(): Promise<PermissionSetMatrix> {
  const res = await client.get<ApiResponse<PermissionSetMatrix>>(
    '/api/v1/admin/permissions/permission-sets/matrix',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'PermissionSet 매트릭스 조회에 실패했습니다');
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

/**
 * Spec #837 — PermissionSet 자체 관리 (CRUD + 권한 비트 + 변경 이력).
 */

export interface PermissionSetCreateRequest {
  name: string;
  label?: string | null;
  description?: string | null;
}

export interface PermissionSetUpdateMetaRequest {
  label?: string | null;
  description?: string | null;
}

/**
 * Spec #837 — 권한 비트 전체 교체 형식 (부분 patch 아님).
 *
 * UI 매트릭스 에디터의 현재 비트 set 그대로 송신. 키는 `available-resources` endpoint 의
 * sfObjects.sfApiName / customResources 만 허용 (backend service 가 검증).
 */
export interface PermissionSetUpdateFlagsRequest {
  viewAllData: boolean;
  modifyAllData: boolean;
  objectPermissions: Record<string, Record<string, boolean>>;
  customPermissions: Record<string, Record<string, boolean>>;
}

/**
 * Spec #837 — PS Mutation 응답 형식.
 *
 * 형식 차이 (#837 결정 C): `objectPermissions`/`customPermissions` 는 매트릭스 저장-전송
 * round-trip 을 위해 `Record<string, Record<string, boolean>>` (DB jsonb 본문 그대로). Inspection
 * (`PermissionSetDetail.objectPermissions`) 의 `ObjectPermissionRow[]` 와 다름 — 매트릭스 컴포넌트가
 * 어댑터로 상호 변환.
 */
export interface PermissionSetMutationResponse {
  permissionSetId: number;
  name: string;
  label: string | null;
  description: string | null;
  sfOrigin: boolean;
  permissionSetFlagsId: number | null;
  viewAllData: boolean;
  modifyAllData: boolean;
  objectPermissions: Record<string, Record<string, boolean>>;
  customPermissions: Record<string, Record<string, boolean>>;
  isLocallyModified: boolean;
}

export interface PermissionSetChangeLogEntry {
  changeLogId: number;
  permissionSetId: number | null;
  eventType: 'CREATE' | 'UPDATE_META' | 'UPDATE_FLAGS' | 'DELETE';
  beforeSnapshot: string | null;
  afterSnapshot: string | null;
  changedById: number;
  changedByName: string | null;
  changedAt: string;
  changeReason: string | null;
}

export interface PaginatedPermissionSetChangeLogList {
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  content: PermissionSetChangeLogEntry[];
}

export interface SfObjectResource {
  sfApiName: string;
  entity: string;
}

export interface AvailablePermissionResources {
  sfObjects: SfObjectResource[];
  customResources: string[];
}

/** PS 생성 — Mutation 응답 반환. UI 가 즉시 편집 페이지로 redirect 가능. */
export async function createPermissionSet(req: PermissionSetCreateRequest): Promise<PermissionSetMutationResponse> {
  const res = await client.post<ApiResponse<PermissionSetMutationResponse>>(
    '/api/v1/admin/permissions/permission-sets',
    req,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'PermissionSet 생성에 실패했습니다');
  }
  return res.data.data;
}

/** PS 메타 (label/description) 수정. name 은 수정 불가 — backend 가 ignore. */
export async function updatePermissionSetMeta(
  permissionSetId: number,
  req: PermissionSetUpdateMetaRequest,
): Promise<PermissionSetMutationResponse> {
  const res = await client.put<ApiResponse<PermissionSetMutationResponse>>(
    `/api/v1/admin/permissions/permission-sets/${permissionSetId}`,
    req,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'PermissionSet 메타 수정에 실패했습니다');
  }
  return res.data.data;
}

/** PS 권한 비트 전체 교체. SF 출처 PS 면 응답의 isLocallyModified=true 로 반영. */
export async function updatePermissionSetFlags(
  permissionSetId: number,
  req: PermissionSetUpdateFlagsRequest,
): Promise<PermissionSetMutationResponse> {
  const res = await client.put<ApiResponse<PermissionSetMutationResponse>>(
    `/api/v1/admin/permissions/permission-sets/${permissionSetId}/flags`,
    req,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'PermissionSet 권한 비트 수정에 실패했습니다');
  }
  return res.data.data;
}

/** PS 삭제. SF 출처 PS 는 409 SF_ORIGIN_DELETE_BLOCKED 던짐 — UI 가 사전 비활성화. */
export async function deletePermissionSet(permissionSetId: number): Promise<void> {
  await client.delete(`/api/v1/admin/permissions/permission-sets/${permissionSetId}`);
}

/** 매트릭스 편집 UI 가 사용할 자원 카탈로그 (SF 객체 + 가상 자원). */
export async function fetchAvailablePermissionResources(): Promise<AvailablePermissionResources> {
  const res = await client.get<ApiResponse<AvailablePermissionResources>>(
    '/api/v1/admin/permissions/permission-sets/available-resources',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '자원 카탈로그 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 특정 PS 의 변경 이력 페이지네이션 조회. 시간순 desc 정렬은 backend 가 책임. */
export async function fetchPermissionSetChangeLog(
  permissionSetId: number,
  page = 0,
  size = 20,
): Promise<PaginatedPermissionSetChangeLogList> {
  const res = await client.get<ApiResponse<PaginatedPermissionSetChangeLogList>>(
    `/api/v1/admin/permissions/permission-sets/${permissionSetId}/change-log`,
    { params: { page, size } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '변경 이력 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * Profile 권한 비트 전체 교체 요청 — PUT /profiles/{id}/flags.
 *
 * SF 정합 — 직책별 Profile 에 객체권한을 박으면 발령으로 해당 직책이 된 사원에게 화면 권한 자동 전파.
 * PermissionSet 과 달리 system 비트 5종 전부 (viewAllUsers/manageUsers/apiEnabled 포함) 를 다룬다.
 */
export interface ProfileUpdateFlagsRequest {
  viewAllData: boolean;
  modifyAllData: boolean;
  viewAllUsers: boolean;
  manageUsers: boolean;
  apiEnabled: boolean;
  objectPermissions: Record<string, Record<string, boolean>>;
  customPermissions: Record<string, Record<string, boolean>>;
}

export interface ProfileFlagsMutationResponse {
  profileId: number;
  name: string;
  viewAllData: boolean;
  modifyAllData: boolean;
  viewAllUsers: boolean;
  manageUsers: boolean;
  apiEnabled: boolean;
  objectPermissions: Record<string, Record<string, boolean>>;
  customPermissions: Record<string, Record<string, boolean>>;
  isLocallyModified: boolean;
}

/** Profile 권한 비트 전체 교체. 편집 시 backend 가 isLocallyModified=true 로 set. */
export async function updateProfileFlags(
  profileId: number,
  req: ProfileUpdateFlagsRequest,
): Promise<ProfileFlagsMutationResponse> {
  const res = await client.put<ApiResponse<ProfileFlagsMutationResponse>>(
    `/api/v1/admin/permissions/profiles/${profileId}/flags`,
    req,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Profile 권한 비트 수정에 실패했습니다');
  }
  return res.data.data;
}
