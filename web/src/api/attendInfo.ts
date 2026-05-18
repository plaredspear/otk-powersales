import client from './client';
import type { ApiResponse } from './types';

export type AttendInfoStatus = 'N' | 'Y';

export type AttendTypeCode = '10' | '14' | '20' | '90' | '120' | '133';

export const ATTEND_TYPE_OPTIONS: { value: AttendTypeCode; label: string }[] = [
  { value: '10', label: '1년미만연차' },
  { value: '14', label: '연차' },
  { value: '20', label: '연중휴가' },
  { value: '90', label: '경조' },
  { value: '120', label: '생휴' },
  { value: '133', label: '연중&하기&하계' },
];

export interface ScheduleConversionSummary {
  converted_schedule_count: number;
  deleted_schedule_count: number;
  skipped_employee_not_found: number;
  skipped_job_filter: number;
  skipped_attend_type_filter: number;
  skipped_idempotent: number;
}

export interface LinkedSchedulePreview {
  id: number;
  workingDate: string;
  workingType: string;
}

export interface AttendInfoListItem {
  id: number;
  name: string | null;
  employeeCode: string;
  employeeName: string | null;
  employeeJobCode: string | null;
  attendType: string | null;
  attendTypeName: string | null;
  startDate: string;
  endDate: string | null;
  status: AttendInfoStatus | null;
  createdAt: string;
  createdByName: string | null;
}

export interface AttendInfoDetail extends AttendInfoListItem {
  updatedAt: string;
  lastModifiedByName: string | null;
  linkedScheduleCount: number;
  linkedSchedules: LinkedSchedulePreview[];
  conversionSummary: ScheduleConversionSummary | null;
}

export interface AttendInfoListData {
  content: AttendInfoListItem[];
  page?: number;
  size?: number;
  number?: number;
  totalElements: number;
  totalPages: number;
}

export interface FetchAttendInfoParams {
  employeeId?: number;
  employeeCode?: string;
  attendType?: string;
  startDateFrom?: string;
  startDateTo?: string;
  status?: AttendInfoStatus | '';
  keyword?: string;
  page?: number;
  size?: number;
}

export interface CreateAttendInfoRequest {
  employeeCode: string;
  attendType: string;
  startDate: string;
  endDate: string;
  status: AttendInfoStatus;
  reason: string;
}

export interface UpdateAttendInfoRequest {
  attendType?: string;
  startDate?: string;
  endDate?: string;
  status?: AttendInfoStatus;
  reason?: string;
}

export interface DeleteAttendInfoResponse {
  deletedScheduleCount: number;
}

const BASE = '/api/v1/admin/attend-info';

export async function searchAttendInfo(params: FetchAttendInfoParams = {}): Promise<AttendInfoListData> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  const url = query ? `${BASE}?${query}` : BASE;
  const res = await client.get<ApiResponse<AttendInfoListData>>(url);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근태정보 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function getAttendInfo(id: number): Promise<AttendInfoDetail> {
  const res = await client.get<ApiResponse<AttendInfoDetail>>(`${BASE}/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근태정보 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createAttendInfo(data: CreateAttendInfoRequest): Promise<AttendInfoDetail> {
  const res = await client.post<ApiResponse<AttendInfoDetail>>(BASE, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근태정보 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateAttendInfo(
  id: number,
  data: UpdateAttendInfoRequest,
): Promise<AttendInfoDetail> {
  const res = await client.put<ApiResponse<AttendInfoDetail>>(`${BASE}/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근태정보 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteAttendInfo(id: number): Promise<DeleteAttendInfoResponse> {
  const res = await client.delete<ApiResponse<DeleteAttendInfoResponse>>(`${BASE}/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근태정보 삭제에 실패했습니다');
  }
  return res.data.data;
}
