import client from './client';
import type { ApiResponse } from './types';

export interface UserListParams {
  keyword?: string;
  isActive?: boolean;
  profileId?: number;
  page: number;
  size: number;
}

export interface UserSummary {
  id: number;
  username: string;
  employeeCode: string;
  name: string | null;
  email: string | null;
  profileName: string | null;
  branch: string | null;
  department: string | null;
  isActive: boolean;
  lastLoginAt: string | null;
}

export interface UserListData {
  content: UserSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserDetail {
  id: number;
  username: string;
  employeeCode: string;
  name: string | null;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  alias: string | null;
  title: string | null;
  department: string | null;
  division: string | null;
  branch: string | null;
  mobilePhone: string | null;
  phone: string | null;
  hrCode: string | null;
  profileName: string | null;
  isSalesSupport: boolean;
  isActive: boolean;
  passwordChangeRequired: boolean;
  lastLoginAt: string | null;
  createdAt: string | null;
  lastModifiedAt: string | null;
}

export interface UserPasswordResetResult {
  userId: number;
  username: string;
  temporaryPasswordIssued: boolean;
  passwordChangeRequired: boolean;
  resetAt: string;
}

// --- API functions ---

/**
 * web admin User 목록 조회.
 *
 * keyword (username/employeeCode/name 부분일치) + isActive + profileId 필터 + 페이지네이션 파라미터로 admin API 호출.
 */
export async function fetchUsers(params: UserListParams): Promise<UserListData> {
  const res = await client.get<ApiResponse<UserListData>>('/api/v1/admin/users', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사용자 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 사용자 관리 화면 필터용 프로파일 옵션 (id/name). */
export interface UserProfileOption {
  id: number;
  name: string;
}

/**
 * 사용자 관리 필터용 프로파일 옵션 조회.
 *
 * 프로파일 관리 상세 목록(`/permissions/profiles`, `profile` READ 가드) 대신 `user` READ 로 가드된
 * 경량 lookup 을 호출한다. `user` 권한만 가진 관리자도 필터 드롭다운을 채울 수 있다.
 */
export async function fetchUserProfileOptions(): Promise<UserProfileOption[]> {
  const res = await client.get<ApiResponse<UserProfileOption[]>>('/api/v1/admin/users/profile-options');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '프로파일 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * web admin User 상세 조회.
 */
export async function fetchUserDetail(id: number): Promise<UserDetail> {
  const res = await client.get<ApiResponse<UserDetail>>(`/api/v1/admin/users/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사용자 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * web admin User 비밀번호 임시 리셋.
 *
 * 임시 비밀번호 평문(`{사번}@pwrs`) 은 응답에 포함되지 않으며, 화면이 응답의 `employeeCode` 로
 * 동일 규칙을 재조립해 안내 메시지로 노출한다.
 */
export async function resetUserPassword(id: number): Promise<UserPasswordResetResult> {
  const res = await client.post<ApiResponse<UserPasswordResetResult>>(
    `/api/v1/admin/users/${id}/reset-password`
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '비밀번호 초기화에 실패했습니다');
  }
  return res.data.data;
}

/**
 * web admin User 활성/비활성 토글.
 *
 * 자기 자신 비활성화 시도는 backend 에서 400 (`CANNOT_DEACTIVATE_SELF`) 으로 차단된다.
 */
export async function updateUserActiveStatus(id: number, isActive: boolean): Promise<void> {
  const res = await client.put<ApiResponse<unknown>>(
    `/api/v1/admin/users/${id}/active`,
    { isActive }
  );
  if (!res.data.success) {
    throw new Error(res.data.message || '사용자 상태 변경에 실패했습니다');
  }
}
