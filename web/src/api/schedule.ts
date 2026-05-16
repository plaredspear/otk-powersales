import client from './client';
import type { ApiResponse } from './types';


interface PageRaw<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}


export interface ScheduleUploadResult {
  uploadId: string;
  totalRows: number;
  successRows: number;
  errorRows: number;
  errors: RowError[];
  previews: RowPreview[];
}

export interface RowError {
  row: number;
  column: string;
  field: string;
  value: string | null;
  message: string;
}

export interface RowPreview {
  row: number;
  employeeCode: string;
  employeeName: string;
  accountCode: string;
  accountName: string;
  typeOfWork3: string;
  typeOfWork5: string;
  startDate: string;
  endDate: string | null;
}

export interface ScheduleConfirmResult {
  insertedCount: number;
}

export interface ScheduleListItem {
  id: number;
  employeeCode: string;
  employeeName: string;
  accountCode: string | null;
  accountName: string | null;
  typeOfWork3: string | null;
  typeOfWork5: string | null;
  startDate: string | null;
  endDate: string | null;
  confirmed: boolean | null;
  costCenterCode: string | null;
  lastMonthRevenue: number | null;
}

export interface ScheduleListResponse {
  content: ScheduleListItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export type SchedulePreset =
  | 'INPUT_TODAY'
  | 'ALL'
  | 'VALID'
  | 'VALID_CONFIRMED'
  | 'VALID_NOT_CONFIRMED'
  | 'FIXED_VALID'
  | 'BIFURCATION_VALID'
  | 'PATROL_VALID'
  | 'VALID_CONFIRMED_TEMP'
  | 'END';

export interface ScheduleListParams {
  page?: number;
  size?: number;
  employeeCode?: string;
  accountName?: string;
  confirmed?: boolean;
  typeOfWork3?: string;
  startDateFrom?: string;
  startDateTo?: string;
  preset?: SchedulePreset;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ScheduleBatchConfirmResult {
  updatedCount: number;
}


// --- API functions ---

export async function downloadScheduleTemplate(): Promise<void> {
  const res = await client.get('/api/v1/admin/schedule/template', {
    responseType: 'blob',
  });

  // Content-Disposition 헤더에서 파일명 추출
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = '진열스케줄_양식.xlsx';
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
    if (match) {
      filename = decodeURIComponent(match[1]);
    }
  }

  // Blob 다운로드
  const blob = new Blob([res.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export async function uploadScheduleExcel(file: File): Promise<ScheduleUploadResult> {
  const formData = new FormData();
  formData.append('file', file);

  const res = await client.post<ApiResponse<ScheduleUploadResult>>(
    '/api/v1/admin/schedule/upload',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '업로드에 실패했습니다');
  }

  return res.data.data;
}

export async function confirmScheduleUpload(uploadId: string): Promise<ScheduleConfirmResult> {
  const res = await client.post<ApiResponse<ScheduleConfirmResult>>(
    '/api/v1/admin/schedule/upload/confirm',
    { uploadId: uploadId },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '등록 확정에 실패했습니다');
  }

  return res.data.data;
}

export async function fetchScheduleList(params: ScheduleListParams): Promise<ScheduleListResponse> {
  const queryParams: Record<string, string> = {};
  if (params.page != null) queryParams.page = String(params.page);
  if (params.size != null) queryParams.size = String(params.size);
  if (params.employeeCode) queryParams.employeeCode = params.employeeCode;
  if (params.accountName) queryParams.accountName = params.accountName;
  if (params.confirmed != null) queryParams.confirmed = String(params.confirmed);
  if (params.typeOfWork3) queryParams.typeOfWork3 = params.typeOfWork3;
  if (params.startDateFrom) queryParams.startDateFrom = params.startDateFrom;
  if (params.startDateTo) queryParams.startDateTo = params.startDateTo;
  if (params.preset) queryParams.preset = params.preset;
  if (params.sortBy) queryParams.sortBy = params.sortBy;
  if (params.sortDir) queryParams.sortDir = params.sortDir;

  const res = await client.get<ApiResponse<PageRaw<ScheduleListItem>>>(
    '/api/v1/admin/schedule/list',
    { params: queryParams },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '목록 조회에 실패했습니다');
  }

  const raw = res.data.data;
  return {
    content: raw.content,
    totalElements: raw.totalElements,
    totalPages: raw.totalPages,
    page: raw.number,
    size: raw.size,
  };
}

export async function batchConfirmSchedules(ids: number[]): Promise<ScheduleBatchConfirmResult> {
  const res = await client.patch<ApiResponse<ScheduleBatchConfirmResult>>(
    '/api/v1/admin/schedule/confirm',
    { ids },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '일괄 확정에 실패했습니다');
  }

  return res.data.data;
}

export async function batchUnconfirmSchedules(ids: number[]): Promise<ScheduleBatchConfirmResult> {
  const res = await client.patch<ApiResponse<ScheduleBatchConfirmResult>>(
    '/api/v1/admin/schedule/unconfirm',
    { ids },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '확정 해제에 실패했습니다');
  }

  return res.data.data;
}
