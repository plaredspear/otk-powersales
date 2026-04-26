import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: { code: string; message: string };
}

interface ScheduleUploadResultRaw {
  upload_id: string;
  total_rows: number;
  success_rows: number;
  error_rows: number;
  errors: RowErrorRaw[];
  previews: RowPreviewRaw[];
}

interface RowErrorRaw {
  row: number;
  column: string;
  field: string;
  value: string | null;
  message: string;
}

interface RowPreviewRaw {
  row: number;
  employee_code: string;
  employee_name: string;
  account_code: string;
  account_name: string;
  type_of_work3: string;
  type_of_work5: string;
  start_date: string;
  end_date: string | null;
}

interface ScheduleConfirmResultRaw {
  inserted_count: number;
}

interface ScheduleListItemRaw {
  id: number;
  employee_code: string;
  employee_name: string;
  account_code: string | null;
  account_name: string | null;
  type_of_work3: string | null;
  type_of_work5: string | null;
  start_date: string | null;
  end_date: string | null;
  confirmed: boolean | null;
  cost_center_code: string | null;
  last_month_revenue: number | null;
}

interface PageRaw<T> {
  content: T[];
  total_elements: number;
  total_pages: number;
  number: number;
  size: number;
}

interface ScheduleBatchConfirmResultRaw {
  updated_count: number;
}

// --- Frontend interfaces (camelCase) ---

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

export interface ScheduleListParams {
  page?: number;
  size?: number;
  employeeCode?: string;
  accountName?: string;
  confirmed?: boolean;
  typeOfWork3?: string;
  startDateFrom?: string;
  startDateTo?: string;
}

export interface ScheduleBatchConfirmResult {
  updatedCount: number;
}

// --- Mappers ---

function mapScheduleListItem(raw: ScheduleListItemRaw): ScheduleListItem {
  return {
    id: raw.id,
    employeeCode: raw.employee_code,
    employeeName: raw.employee_name,
    accountCode: raw.account_code,
    accountName: raw.account_name,
    typeOfWork3: raw.type_of_work3,
    typeOfWork5: raw.type_of_work5,
    startDate: raw.start_date,
    endDate: raw.end_date,
    confirmed: raw.confirmed,
    costCenterCode: raw.cost_center_code,
    lastMonthRevenue: raw.last_month_revenue,
  };
}

function mapScheduleListResponse(raw: PageRaw<ScheduleListItemRaw>): ScheduleListResponse {
  return {
    content: raw.content.map(mapScheduleListItem),
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
    page: raw.number,
    size: raw.size,
  };
}

function mapUploadResult(raw: ScheduleUploadResultRaw): ScheduleUploadResult {
  return {
    uploadId: raw.upload_id,
    totalRows: raw.total_rows,
    successRows: raw.success_rows,
    errorRows: raw.error_rows,
    errors: raw.errors.map((e) => ({
      row: e.row,
      column: e.column,
      field: e.field,
      value: e.value,
      message: e.message,
    })),
    previews: raw.previews.map((p) => ({
      row: p.row,
      employeeCode: p.employee_code,
      employeeName: p.employee_name,
      accountCode: p.account_code,
      accountName: p.account_name,
      typeOfWork3: p.type_of_work3,
      typeOfWork5: p.type_of_work5,
      startDate: p.start_date,
      endDate: p.end_date,
    })),
  };
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

  const res = await client.post<ApiResponse<ScheduleUploadResultRaw>>(
    '/api/v1/admin/schedule/upload',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '업로드에 실패했습니다');
  }

  return mapUploadResult(res.data.data);
}

export async function confirmScheduleUpload(uploadId: string): Promise<ScheduleConfirmResult> {
  const res = await client.post<ApiResponse<ScheduleConfirmResultRaw>>(
    '/api/v1/admin/schedule/upload/confirm',
    { upload_id: uploadId },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '등록 확정에 실패했습니다');
  }

  return { insertedCount: res.data.data.inserted_count };
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

  const res = await client.get<ApiResponse<PageRaw<ScheduleListItemRaw>>>(
    '/api/v1/admin/schedule/list',
    { params: queryParams },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '목록 조회에 실패했습니다');
  }

  return mapScheduleListResponse(res.data.data);
}

export async function batchConfirmSchedules(ids: number[]): Promise<ScheduleBatchConfirmResult> {
  const res = await client.patch<ApiResponse<ScheduleBatchConfirmResultRaw>>(
    '/api/v1/admin/schedule/confirm',
    { ids },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '일괄 확정에 실패했습니다');
  }

  return { updatedCount: res.data.data.updated_count };
}

export async function batchUnconfirmSchedules(ids: number[]): Promise<ScheduleBatchConfirmResult> {
  const res = await client.patch<ApiResponse<ScheduleBatchConfirmResultRaw>>(
    '/api/v1/admin/schedule/unconfirm',
    { ids },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '확정 해제에 실패했습니다');
  }

  return { updatedCount: res.data.data.updated_count };
}
