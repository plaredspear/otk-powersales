import client from '../client';
import type { ApiResponse } from '../types';

/**
 * SF UserRole (조직 계층) admin 조회 API.
 *
 * SF "설정 > 사용자 > 역할" 페이지 동등.
 */

export interface UserRoleNode {
  userRoleId: number;
  name: string;
  developerName: string | null;
  rollupDescription: string | null;
  parentUserRoleId: number | null;
  parentName: string | null;
  children: UserRoleNode[];
}

export async function getUserRoleTree(): Promise<UserRoleNode[]> {
  const res = await client.get<ApiResponse<UserRoleNode[]>>('/api/v1/admin/user-roles/tree');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '역할 트리 조회에 실패했습니다');
  }
  return res.data.data;
}
