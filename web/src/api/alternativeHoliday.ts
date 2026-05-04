import client from './client';
import type { ApiResponse } from './types';


export interface AlternativeHolidayItem {
  id: number;
  employeeCode: string;
  employeeName: string;
  orgName: string | null;
  actualWorkDate: string;
  targetAltHolidayDate: string;
  confirmAltHolidayDate: string | null;
  status: string;
  changeReason: string | null;
  createdBy: string;
  createdAt: string;
}

export interface AlternativeHolidayListParams {
  startDate: string;
  endDate: string;
  status?: string;
  employeeCode?: string;
  orgCode?: string;
}

export interface CreateAlternativeHolidayPayload {
  employeeCode: string;
  actualWorkDate: string;
  targetAltHolidayDate: string;
}

export interface ApproveAlternativeHolidayPayload {
  confirmAltHolidayDate?: string | null;
}

export interface RejectAlternativeHolidayPayload {
  changeReason: string;
}


// --- API functions ---

export async function fetchAlternativeHolidays(
  params: AlternativeHolidayListParams,
): Promise<AlternativeHolidayItem[]> {
  const queryParams: Record<string, string> = {
    startDate: params.startDate,
    endDate: params.endDate,
  };
  if (params.status) queryParams.status = params.status;
  if (params.employeeCode) queryParams.employeeCode = params.employeeCode;
  if (params.orgCode) queryParams.orgCode = params.orgCode;

  const res = await client.get<ApiResponse<AlternativeHolidayItem[]>>(
    '/api/v1/admin/alternative-holidays',
    { params: queryParams },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '목록 조회에 실패했습니다');
  }

  return res.data.data;
}

export async function createAlternativeHoliday(
  payload: CreateAlternativeHolidayPayload,
): Promise<{ id: number; status: string }> {
  const res = await client.post<ApiResponse<{ id: number; status: string }>>(
    '/api/v1/admin/alternative-holidays',
    payload,
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '신청에 실패했습니다');
  }

  return { id: res.data.data.id, status: res.data.data.status };
}

export async function approveAlternativeHoliday(
  id: number,
  payload: ApproveAlternativeHolidayPayload,
): Promise<{ id: number; status: string; confirmAltHolidayDate: string | null }> {
  const res = await client.post<ApiResponse<{ id: number; status: string; confirmAltHolidayDate: string | null }>>(
    `/api/v1/admin/alternative-holidays/${id}/approve`,
    payload,
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '승인에 실패했습니다');
  }

  return {
    id: res.data.data.id,
    status: res.data.data.status,
    confirmAltHolidayDate: res.data.data.confirmAltHolidayDate,
  };
}

export async function rejectAlternativeHoliday(
  id: number,
  payload: RejectAlternativeHolidayPayload,
): Promise<{ id: number; status: string }> {
  const res = await client.post<ApiResponse<{ id: number; status: string }>>(
    `/api/v1/admin/alternative-holidays/${id}/reject`,
    payload,
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '반려에 실패했습니다');
  }

  return { id: res.data.data.id, status: res.data.data.status };
}
