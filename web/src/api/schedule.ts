import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: { code: string; message: string };
}

interface ScheduleBranchRaw {
  cost_center_code: string;
  branch_name: string;
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

// --- Frontend interfaces (camelCase) ---

export interface ScheduleBranch {
  costCenterCode: string;
  branchName: string;
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

// --- Mappers ---

function mapBranches(raw: ScheduleBranchRaw[]): ScheduleBranch[] {
  return raw.map((b) => ({
    costCenterCode: b.cost_center_code,
    branchName: b.branch_name,
  }));
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

export async function fetchScheduleBranches(): Promise<ScheduleBranch[]> {
  const res = await client.get<ApiResponse<ScheduleBranchRaw[]>>('/api/v1/admin/schedule/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return mapBranches(res.data.data);
}

export async function downloadScheduleTemplate(costCenterCode: string): Promise<void> {
  const res = await client.get('/api/v1/admin/schedule/template', {
    params: { costCenterCode },
    responseType: 'blob',
  });

  // Content-Disposition 헤더에서 파일명 추출
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `진열스케줄_양식_${costCenterCode}.xlsx`;
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
