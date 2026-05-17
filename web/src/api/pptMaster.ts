import client from './client';
import type { ApiResponse } from './types';


export interface PPTMaster {
  id: number;
  employeeId: number;
  employeeCode: string;
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
  employeeId: number;
  accountId: number;
  teamType: string;
  startDate: string;
  endDate?: string | null;
  isConfirmed: boolean;
}

export interface PPTMasterSearchParams {
  page?: number;
  size?: number;
  employeeName?: string;
  employeeCode?: string;
  teamType?: string;
  branchCode?: string;
  validOnly?: boolean;
}

export interface PPTMasterBulkItem {
  employeeCode: string;
  accountCode: string;
  teamType: string;
  startDate: string;
  endDate?: string | null;
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


// --- API functions ---

export async function getPPTMasters(params: PPTMasterSearchParams): Promise<PPTMasterListData> {
  const res = await client.get<ApiResponse<PPTMasterListData>>('/api/v1/admin/ppt-masters', {
    params,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function getPPTMaster(id: number): Promise<PPTMaster> {
  const res = await client.get<ApiResponse<PPTMaster>>(`/api/v1/admin/ppt-masters/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createPPTMaster(data: PPTMasterFormData): Promise<PPTMaster> {
  const res = await client.post<ApiResponse<PPTMaster>>('/api/v1/admin/ppt-masters', data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updatePPTMaster(id: number, data: PPTMasterFormData): Promise<PPTMaster> {
  const res = await client.put<ApiResponse<PPTMaster>>(`/api/v1/admin/ppt-masters/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 수정에 실패했습니다');
  }
  return res.data.data;
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
  const res = await client.post<ApiResponse<BulkValidationResult>>(
    '/api/v1/admin/ppt-masters/bulk',
    { items },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일괄 검증에 실패했습니다');
  }
  return res.data.data;
}

export async function confirmPPTMasterBulk(
  items: PPTMasterBulkItem[],
): Promise<BulkConfirmResult> {
  const res = await client.post<ApiResponse<BulkConfirmResult>>(
    '/api/v1/admin/ppt-masters/bulk/confirm',
    { items },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일괄 등록에 실패했습니다');
  }
  return res.data.data;
}


// --- PPT 이력 조회 ---

export interface PPTHistory {
  id: number;
  employeeId: number;
  employeeName: string | null;
  employeeCode: string | null;
  orgName: string | null;
  status: string | null;
  oldValue: string | null;
  newValue: string;
  changedAt: string;
}

export interface PPTHistoryListData {
  content: PPTHistory[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PPTHistorySearchParams {
  page?: number;
  size?: number;
  employeeName?: string;
  employeeCode?: string;
  teamType?: string;
  changedAtFrom?: string;
  changedAtTo?: string;
}

export async function getPPTHistories(
  params: PPTHistorySearchParams,
): Promise<PPTHistoryListData> {
  const res = await client.get<ApiResponse<PPTHistoryListData>>(
    '/api/v1/admin/ppt-histories',
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 이력 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function getPPTMasterHistory(
  masterId: number,
  params: { page?: number; size?: number },
): Promise<PPTHistoryListData> {
  const res = await client.get<ApiResponse<PPTHistoryListData>>(
    `/api/v1/admin/ppt-masters/${masterId}/history`,
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 마스터 이력 조회에 실패했습니다');
  }
  return res.data.data;
}
