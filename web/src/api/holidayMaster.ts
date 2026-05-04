import client from './client';
import type { ApiResponse } from './types';


export interface HolidayMaster {
  id: number;
  holidayDate: string;
  name: string;
  type: string;
}

export interface HolidayMasterRequest {
  holidayDate: string;
  name: string;
  type: string;
}


export async function fetchHolidayMasters(year: number): Promise<HolidayMaster[]> {
  const res = await client.get<ApiResponse<HolidayMaster[]>>(
    `/api/v1/admin/holiday-masters?year=${year}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공휴일 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createHolidayMaster(data: HolidayMasterRequest): Promise<HolidayMaster> {
  const res = await client.post<ApiResponse<HolidayMaster>>(
    '/api/v1/admin/holiday-masters',
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공휴일 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateHolidayMaster(
  id: number,
  data: HolidayMasterRequest,
): Promise<HolidayMaster> {
  const res = await client.put<ApiResponse<HolidayMaster>>(
    `/api/v1/admin/holiday-masters/${id}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공휴일 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteHolidayMaster(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/holiday-masters/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '공휴일 삭제에 실패했습니다');
  }
}
