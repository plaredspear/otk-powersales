import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 사원 단말 / 비밀번호 운영자 리셋 API (Spec #582).
 *
 * 백엔드 `POST /api/v1/admin/{employees|female-employees}/{employeeId}/reset-device|reset-password` 호출.
 * 여사원 현황(`female_employee:EDIT`)에서 진입한 경우 `/female-employees/*` 를, 설정 사원
 * (`MANAGE_USERS`) 에서 진입한 경우 `/employees/*` 를 호출하여 권한 가드와 정합.
 * 응답 wire format은 backend 표준 ApiResponse 의 camelCase data 필드.
 */

function credentialBasePath(isFemale: boolean): string {
  return isFemale ? '/api/v1/admin/female-employees' : '/api/v1/admin/employees';
}

export interface ResetDeviceResponse {
  employeeId: number;
  employeeCode: string;
  name: string;
  previousDeviceBound: boolean;
  resetAt: string;
}

export interface ResetPasswordResponse {
  employeeId: number;
  employeeCode: string;
  name: string;
  temporaryPasswordIssued: boolean;
  passwordChangeRequired: boolean;
  resetAt: string;
}

export async function resetEmployeeDevice(
  employeeId: number,
  isFemale = false,
): Promise<ResetDeviceResponse> {
  const res = await client.post<ApiResponse<ResetDeviceResponse>>(
    `${credentialBasePath(isFemale)}/${employeeId}/reset-device`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '단말 초기화에 실패했습니다');
  }
  return res.data.data;
}

export async function resetEmployeePassword(
  employeeId: number,
  isFemale = false,
): Promise<ResetPasswordResponse> {
  const res = await client.post<ApiResponse<ResetPasswordResponse>>(
    `${credentialBasePath(isFemale)}/${employeeId}/reset-password`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '비밀번호 초기화에 실패했습니다');
  }
  return res.data.data;
}
