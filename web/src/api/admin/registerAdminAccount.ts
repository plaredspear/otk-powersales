import client from '@/api/client';
import type { ApiResponse } from '../types';
import type {
  AdminAccountRegisterRequest,
  AdminAccountRegisterResponse,
} from './types';

/**
 * 시스템 관리자 수동 등록 (Spec #579).
 *
 * 백엔드 `POST /api/v1/admin/employees` 호출. 성공 시 응답의 `data` 필드를 반환한다.
 * 에러는 axios가 throw 하며, 호출 측에서 `error.response.data.error.code` 로 분기 처리한다.
 */
export async function registerAdminAccount(
  payload: AdminAccountRegisterRequest,
): Promise<AdminAccountRegisterResponse> {
  const res = await client.post<ApiResponse<AdminAccountRegisterResponse>>(
    '/api/v1/admin/employees',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error('관리자 계정 등록에 실패했습니다');
  }
  return res.data.data;
}
