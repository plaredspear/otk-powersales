import client from './client';
import type { ApiResponse } from './types';


export interface PPTMaster {
  id: number;
  /** SF Name (AutoNumber PM{0000000}) — 마이그레이션 레코드만 보유, 신규 등록분은 null */
  name: string | null;
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
  /** SF BranchName__c — 사원 소속 지점명 */
  branchName: string | null;
  /** SF ValidConditionData__c 산출용 raw — 재직상태 계산 */
  employeeStatus: string | null;
  employeeAppLoginActive: boolean | null;
  employeeEndDate: string | null;
  /** SF AccountType__c — 거래처유형 (수퍼/할인점 등) */
  accountType: string | null;
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

/** 전문행사조 화면 지점 셀렉터 옵션 (마스터/이력/확정인원 공용). */
export interface PPTBranch {
  branchCode: string;
  branchName: string;
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

export interface ConfirmByIdsResult {
  confirmedCount: number;
  skippedCount: number;
}


// --- API functions ---

/**
 * 전문행사조 화면 지점 셀렉터 옵션 조회.
 *
 * 여사원 일정/대시보드와 동일하게 권한별 지점 화이트리스트를 반환한다. 마스터/이력/확정인원 3화면 공용.
 */
export async function getPPTBranches(): Promise<PPTBranch[]> {
  const res = await client.get<ApiResponse<PPTBranch[]>>('/api/v1/admin/ppt-masters/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

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

/** 전문행사조 마스터 엑셀 템플릿 다운로드 경로. */
export const PPT_MASTER_TEMPLATE_PATH = '/api/v1/admin/ppt-masters/excel-template';

/** 전문행사조 마스터 엑셀 다운로드 경로. */
export const PPT_MASTER_EXPORT_PATH = '/api/v1/admin/ppt-masters/export';

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

export async function confirmPPTMastersByIds(
  ids: number[],
): Promise<ConfirmByIdsResult> {
  const res = await client.post<ApiResponse<ConfirmByIdsResult>>(
    '/api/v1/admin/ppt-masters/confirm-by-ids',
    { ids },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '선택 일괄 확정에 실패했습니다');
  }
  return res.data.data;
}


// --- PPT 이력 조회 ---

export interface PPTHistory {
  id: number;
  name: string | null;
  employeeId: number;
  employeeName: string | null;
  employeeCode: string | null;
  orgName: string | null;
  oldValue: string | null;
  newValue: string;
  changedAt: string;
  // 이력을 유발한 마스터의 거래처 — masterId 가 없는 이력(만료/삭제 해제·구 데이터)은 null.
  accountCode: string | null;
  accountName: string | null;
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
  branchCode?: string;
  changedAtFrom?: string;
  changedAtTo?: string;
}

/** 전문행사조 이력 엑셀 다운로드 경로 (GET, 목록과 동일 검색 파라미터). */
export const PPT_HISTORY_EXPORT_PATH = '/api/v1/admin/ppt-histories/export';

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
