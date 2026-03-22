import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: {
    code: string;
    message: string;
  };
}

interface PPTMasterRaw {
  id: number;
  employee_id: number;
  employee_number: string;
  employee_name: string;
  account_id: number;
  account_code: string | null;
  account_name: string | null;
  team_type: string;
  start_date: string;
  end_date: string | null;
  is_confirmed: boolean;
  branch_code: string | null;
  branch_name: string | null;
  created_at: string;
  updated_at: string;
}

interface PPTMasterListRaw {
  content: PPTMasterRaw[];
  total_elements: number;
  total_pages: number;
  number: number;
  size: number;
}

interface BulkValidationResultRaw {
  total_count: number;
  success_count: number;
  error_count: number;
  is_all_valid: boolean;
  results: BulkValidationRowRaw[];
}

interface BulkValidationRowRaw {
  row: number;
  valid: boolean;
  error_message: string | null;
}

interface BulkConfirmResultRaw {
  created_count: number;
}

// --- Frontend interfaces (camelCase) ---

export interface PPTMaster {
  id: number;
  employeeId: number;
  employeeNumber: string;
  employeeName: string;
  accountId: number;
  accountCode: string | null;
  accountName: string | null;
  teamType: string;
  startDate: string;
  endDate: string | null;
  isConfirmed: boolean;
  branchCode: string | null;
  branchName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PPTMasterListData {
  content: PPTMaster[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PPTMasterFormData {
  employee_id: number;
  account_id: number;
  team_type: string;
  start_date: string;
  end_date?: string | null;
  is_confirmed: boolean;
}

export interface PPTMasterSearchParams {
  page?: number;
  size?: number;
  employee_name?: string;
  employee_number?: string;
  team_type?: string;
  branch_code?: string;
  valid_only?: boolean;
}

export interface PPTMasterBulkItem {
  employee_number: string;
  account_code: string;
  team_type: string;
  start_date: string;
  end_date?: string | null;
}

export interface BulkValidationResult {
  totalCount: number;
  successCount: number;
  errorCount: number;
  isAllValid: boolean;
  results: BulkValidationRow[];
}

export interface BulkValidationRow {
  row: number;
  valid: boolean;
  errorMessage: string | null;
}

export interface BulkConfirmResult {
  createdCount: number;
}

// --- Mappers ---

function mapPPTMaster(raw: PPTMasterRaw): PPTMaster {
  return {
    id: raw.id,
    employeeId: raw.employee_id,
    employeeNumber: raw.employee_number,
    employeeName: raw.employee_name,
    accountId: raw.account_id,
    accountCode: raw.account_code,
    accountName: raw.account_name,
    teamType: raw.team_type,
    startDate: raw.start_date,
    endDate: raw.end_date,
    isConfirmed: raw.is_confirmed,
    branchCode: raw.branch_code,
    branchName: raw.branch_name,
    createdAt: raw.created_at,
    updatedAt: raw.updated_at,
  };
}

// --- API functions ---

export async function getPPTMasters(params: PPTMasterSearchParams): Promise<PPTMasterListData> {
  const res = await client.get<ApiResponse<PPTMasterListRaw>>('/api/v1/admin/ppt-masters', {
    params,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 목록 조회에 실패했습니다');
  }
  const raw = res.data.data;
  return {
    content: raw.content.map(mapPPTMaster),
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
    number: raw.number,
    size: raw.size,
  };
}

export async function getPPTMaster(id: number): Promise<PPTMaster> {
  const res = await client.get<ApiResponse<PPTMasterRaw>>(`/api/v1/admin/ppt-masters/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 조회에 실패했습니다');
  }
  return mapPPTMaster(res.data.data);
}

export async function createPPTMaster(data: PPTMasterFormData): Promise<PPTMaster> {
  const res = await client.post<ApiResponse<PPTMasterRaw>>('/api/v1/admin/ppt-masters', data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 등록에 실패했습니다');
  }
  return mapPPTMaster(res.data.data);
}

export async function updatePPTMaster(id: number, data: PPTMasterFormData): Promise<PPTMaster> {
  const res = await client.put<ApiResponse<PPTMasterRaw>>(`/api/v1/admin/ppt-masters/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 수정에 실패했습니다');
  }
  return mapPPTMaster(res.data.data);
}

export async function deletePPTMaster(id: number): Promise<void> {
  await client.delete(`/api/v1/admin/ppt-masters/${id}`);
}

export async function downloadPPTMasterTemplate(): Promise<Blob> {
  const res = await client.get('/api/v1/admin/ppt-masters/excel-template', {
    responseType: 'blob',
  });
  return res.data as Blob;
}

export async function validatePPTMasterBulk(
  items: PPTMasterBulkItem[],
): Promise<BulkValidationResult> {
  const res = await client.post<ApiResponse<BulkValidationResultRaw>>(
    '/api/v1/admin/ppt-masters/bulk',
    { items },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일괄 검증에 실패했습니다');
  }
  const raw = res.data.data;
  return {
    totalCount: raw.total_count,
    successCount: raw.success_count,
    errorCount: raw.error_count,
    isAllValid: raw.is_all_valid,
    results: raw.results.map((r) => ({
      row: r.row,
      valid: r.valid,
      errorMessage: r.error_message,
    })),
  };
}

export async function confirmPPTMasterBulk(
  items: PPTMasterBulkItem[],
): Promise<BulkConfirmResult> {
  const res = await client.post<ApiResponse<BulkConfirmResultRaw>>(
    '/api/v1/admin/ppt-masters/bulk/confirm',
    { items },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일괄 등록에 실패했습니다');
  }
  return { createdCount: res.data.data.created_count };
}
