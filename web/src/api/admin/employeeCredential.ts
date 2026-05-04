import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 사원 단말 / 비밀번호 운영자 리셋 API (Spec #582).
 *
 * 백엔드 `POST /api/v1/admin/employees/{employeeId}/reset-device|reset-password` 호출.
 * 응답 wire format은 backend 표준 ApiResponse 의 camelCase data 필드.
 */

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

export async function resetEmployeeDevice(employeeId: number): Promise<ResetDeviceResponse> {
  const res = await client.post<ApiResponse<ResetDeviceResponse>>(
    `/api/v1/admin/employees/${employeeId}/reset-device`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '단말 초기화에 실패했습니다');
  }
  return res.data.data;
}

export async function resetEmployeePassword(employeeId: number): Promise<ResetPasswordResponse> {
  const res = await client.post<ApiResponse<ResetPasswordResponse>>(
    `/api/v1/admin/employees/${employeeId}/reset-password`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '비밀번호 초기화에 실패했습니다');
  }
  return res.data.data;
}
