import client from './client';
import type { ApiResponse } from './types';

export type AttendanceTypeCode = 'REGULAR' | 'DISPLAY' | 'EVENT';

export const ATTENDANCE_TYPE_OPTIONS: { value: AttendanceTypeCode; label: string }[] = [
  { value: 'REGULAR', label: '일반' },
  { value: 'DISPLAY', label: '진열' },
  { value: 'EVENT', label: '행사' },
];

export type SecondWorkTypeValue = '상온' | '냉동/냉장';

export interface AttendanceLogListItem {
  id: number;
  name: string | null;
  employeeId: number | null;
  employeeCode: string | null;
  employeeName: string | null;
  employeeJobCode: string | null;
  accountId: number | null;
  accountCode: string | null;
  accountName: string | null;
  attendanceDate: string | null;
  attendanceType: AttendanceTypeCode | null;
  secondWorkType: SecondWorkTypeValue | null;
  secondWorkTypeName: string | null;
  reason: string | null;
  createdAt: string;
}

export interface AttendanceLogDetail extends AttendanceLogListItem {
  sfid: string | null;
  employeeSfid: string | null;
  accountSfid: string | null;
  ownerSfid: string | null;
  ownerUserName: string | null;
  createdBySfid: string | null;
  createdByName: string | null;
  lastModifiedBySfid: string | null;
  lastModifiedByName: string | null;
  isDeleted: boolean | null;
  updatedAt: string;
}

export interface AttendanceLogListData {
  content: AttendanceLogListItem[];
  page?: number;
  size?: number;
  number?: number;
  totalElements: number;
  totalPages: number;
}

export interface FetchAttendanceLogParams {
  employeeId?: number;
  accountId?: number;
  attendanceType?: AttendanceTypeCode | '';
  attendanceDateFrom?: string;
  attendanceDateTo?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

const BASE = '/api/v1/admin/attendance-log';

export async function searchAttendanceLog(
  params: FetchAttendanceLogParams = {},
): Promise<AttendanceLogListData> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  const url = query ? `${BASE}?${query}` : BASE;
  const res = await client.get<ApiResponse<AttendanceLogListData>>(url);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근무 등록현황 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function getAttendanceLog(id: number): Promise<AttendanceLogDetail> {
  const res = await client.get<ApiResponse<AttendanceLogDetail>>(`${BASE}/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근무 등록 상세 조회에 실패했습니다');
  }
  return res.data.data;
}
