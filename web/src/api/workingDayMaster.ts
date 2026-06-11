import client from './client';
import type { ApiResponse } from './types';

/**
 * 영업일관리마스터 조회 API. (Backend `AdminWorkingDayMasterController`, `/api/v1/admin/working-day-masters`)
 *
 * 운영이 관리하는 영업일 달력(연-월 단위)을 조회한다(조회 전용). 권한: working_day_master READ.
 * `workingDateCheck = 1` 인 날짜가 영업일이며 주말/공휴일은 0.
 */

export interface WorkingDayMasterListItem {
  id: number;
  name: string | null;
  workingDate: string | null;
  workingDateCheck: number | null;
  isWorkingDay: boolean;
  createdByName: string | null;
  lastModifiedByName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface WorkingDayMasterListResponse {
  content: WorkingDayMasterListItem[];
  workingDayCount: number;
  holidayCount: number;
}

export async function fetchWorkingDayMasters(
  year: number,
  month: number,
): Promise<WorkingDayMasterListResponse> {
  const res = await client.get<ApiResponse<WorkingDayMasterListResponse>>(
    '/api/v1/admin/working-day-masters',
    { params: { year, month } },
  );
  if (!res.data.data) {
    throw new Error(res.data.message || '영업일 마스터 조회에 실패했습니다');
  }
  return res.data.data;
}
