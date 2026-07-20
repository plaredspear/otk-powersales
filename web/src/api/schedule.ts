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
  employeeId: number | null;
  employeeCode: string;
  employeeName: string;
  branchName: string | null;
  /** 재직상태 (SF formula ValidConditionData__c) — 재직/휴직/퇴직YYYY-MM-DD/퇴직예정YYYY-MM-DD */
  employmentStatus: string | null;
  /** 유효데이터 (SF formula ValidData__c) — 유효/예정/종료/null */
  validData: string | null;
  /** 유효 신호등 (SF formula Valid__c) — GREEN/YELLOW/RED/null */
  valid: 'GREEN' | 'YELLOW' | 'RED' | null;
  accountId: number | null;
  accountCode: string | null;
  accountName: string | null;
  /** 거래처유형 (SF Account.Type) */
  accountType: string | null;
  /** 거래처상태 (SF Account.AccountStatusName__c) */
  accountStatus: string | null;
  typeOfWork3: string | null;
  typeOfWork4: string | null;
  typeOfWork5: string | null;
  startDate: string | null;
  endDate: string | null;
  confirmed: boolean | null;
  costCenterCode: string | null;
  lastMonthRevenue: number | null;
  /** 연결 여사원일정 중 실제 출근(출근보고시각 채워짐)한 건수 — 1 이상이면 수정 불가 */
  attendanceCount: number;
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

/** 유효여부 필터 — 화면 「유효」 신호등 dot 판정(유효/예정/종료)과 1:1 대응 (SF formula ValidData__c). */
export type ScheduleValidData = 'VALID' | 'PLANNED' | 'END';

export interface ScheduleListParams {
  page?: number;
  size?: number;
  employeeCode?: string;
  accountName?: string;
  accountType?: string;
  /** 거래처상태 (SF Account.AccountStatusName__c: 거래/폐업/출고중지) — 정확 일치 필터. */
  accountStatus?: string;
  confirmed?: boolean;
  typeOfWork3?: string;
  periodStart?: string;
  periodEnd?: string;
  preset?: SchedulePreset;
  /** 유효여부(유효/예정/종료) — 화면 「유효」 신호등 dot 판정 기준. */
  validData?: ScheduleValidData;
  /** 지점 스코프 — 스케줄 소속 지점(costCenterCode) 필터. 전사 권한자만 선택 가능(그 외는 본인 지점 자동 스코프). */
  branchCode?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ScheduleBatchConfirmResult {
  updatedCount: number;
}

export interface ScheduleBatchUnconfirmFailure {
  id: number;
  errorCode: string;
  message: string;
}

/** 확정 해제 결과 — partial success (관리자 등급 / 사업소 scope / 출근 안전망 차단 건은 failures 기록). */
export interface ScheduleBatchUnconfirmResult {
  updatedCount: number;
  failedCount: number;
  failures: ScheduleBatchUnconfirmFailure[];
}

export interface ScheduleBatchDeleteFailure {
  id: number;
  errorCode: string;
  message: string;
}

export interface ScheduleBatchDeleteResult {
  deletedCount: number;
  failedCount: number;
  failures: ScheduleBatchDeleteFailure[];
}

export interface ScheduleCreateRequest {
  employeeCode: string;
  accountCode: string;
  typeOfWork1: string;
  typeOfWork3: string;
  typeOfWork4: string;
  typeOfWork5: string;
  startDate: string;
  endDate?: string | null;
}

export type ScheduleUpdateRequest = ScheduleCreateRequest;

/**
 * 단건 편집 모달 상세 — SF 「진열사원 스케줄 마스터」 레이아웃 정합.
 * 편집 필드 + readonly 계산 정보 (SF 「저장 시 이 필드가 계산됨」) 를 함께 반환.
 */
export interface ScheduleDetail {
  id: number;
  name: string | null;
  confirmed: boolean | null;
  // 편집 가능 필드
  employeeCode: string;
  employeeName: string;
  accountCode: string | null;
  accountName: string | null;
  typeOfWork1: string | null;
  typeOfWork3: string | null;
  typeOfWork4: string | null;
  typeOfWork5: string | null;
  startDate: string | null;
  endDate: string | null;
  // readonly 계산 정보
  branchName: string | null;
  title: string | null;
  employmentStatus: string | null;
  accountStatus: string | null;
  accountType: string | null;
  valid: string | null;
  validData: string | null;
  costCenterCode: string | null;
  lastMonthRevenue: number | null;
}

export interface ScheduleCreateResult {
  id: number;
  employeeCode: string;
  employeeName: string;
  accountCode: string | null;
  accountName: string | null;
  typeOfWork3: string | null;
  typeOfWork4: string | null;
  typeOfWork5: string | null;
  startDate: string | null;
  endDate: string | null;
  costCenterCode: string | null;
  lastMonthRevenue: number | null;
}


// --- API functions ---

/** 진열스케줄 양식 다운로드 경로. */
export const SCHEDULE_TEMPLATE_PATH = '/api/v1/admin/display-work-schedule/template';

/** 선택 진열스케줄 엑셀 다운로드 경로 (POST, body `{ ids }`). */
export const SCHEDULE_EXPORT_PATH = '/api/v1/admin/display-work-schedule/export';

/** 검색결과 전체 진열스케줄 엑셀 다운로드 경로 (GET, 목록과 동일 필터 파라미터). */
export const SCHEDULE_EXPORT_ALL_PATH = '/api/v1/admin/display-work-schedule/export-all';

/**
 * 검색결과 엑셀 다운로드 쿼리 파라미터 빌더 (page/size 제외, 목록과 동일 검색 조건).
 * 실제 다운로드는 공통 `downloadExcel`/`useExcelDownload` 가 수행한다.
 */
export function scheduleExportParams(
  params: Omit<ScheduleListParams, 'page' | 'size'>,
): Record<string, string> {
  const queryParams: Record<string, string> = {};
  if (params.employeeCode) queryParams.employeeCode = params.employeeCode;
  if (params.accountName) queryParams.accountName = params.accountName;
  if (params.accountType) queryParams.accountType = params.accountType;
  if (params.accountStatus) queryParams.accountStatus = params.accountStatus;
  if (params.confirmed != null) queryParams.confirmed = String(params.confirmed);
  if (params.typeOfWork3) queryParams.typeOfWork3 = params.typeOfWork3;
  if (params.periodStart) queryParams.periodStart = params.periodStart;
  if (params.periodEnd) queryParams.periodEnd = params.periodEnd;
  if (params.preset) queryParams.preset = params.preset;
  if (params.validData) queryParams.validData = params.validData;
  if (params.branchCode) queryParams.branchCode = params.branchCode;
  if (params.sortBy) queryParams.sortBy = params.sortBy;
  if (params.sortDir) queryParams.sortDir = params.sortDir;
  return queryParams;
}

// --- 목록 조회 조건 로드(meta) — "권한 기반 조건 로드" 표준 패턴 (행사마스터 정합) ---

export type ScheduleFilterType = 'TEXT' | 'SELECT' | 'DATE';

export interface ScheduleFilterOption {
  value: string;
  label: string;
}

export interface ScheduleFilterMeta {
  key: string;
  type: ScheduleFilterType;
  options?: ScheduleFilterOption[] | null;
}

export interface ScheduleListDefaults {
  pageSize: number;
  sort: string;
}

export interface ScheduleListMeta {
  filters: ScheduleFilterMeta[];
  defaults: ScheduleListDefaults;
}

/**
 * 진열스케줄마스터 목록 화면 조회 조건 로드.
 *
 * 지점 셀렉터(권한 의존) + 근무유형3 + 확정상태 + 텍스트/날짜 필터 + 기본값을 한 번에 반환한다.
 * 기존 `/branches` 단독 호출 + web 하드코딩(근무유형3/확정상태 options)을 대체한다.
 * 지점(branchCode) 옵션 길이로 프론트가 단일/다중을 판별한다(단일이면 Tag, 다중이면 Select).
 */
export async function fetchScheduleListMeta(): Promise<ScheduleListMeta> {
  const res = await client.get<ApiResponse<ScheduleListMeta>>(
    '/api/v1/admin/display-work-schedule/meta',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '진열스케줄 조회 조건 로드에 실패했습니다');
  }
  return res.data.data;
}

export async function uploadScheduleExcel(file: File): Promise<ScheduleUploadResult> {
  const formData = new FormData();
  formData.append('file', file);

  const res = await client.post<ApiResponse<ScheduleUploadResult>>(
    '/api/v1/admin/display-work-schedule/upload',
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
    '/api/v1/admin/display-work-schedule/upload/confirm',
    { uploadId: uploadId },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '등록에 실패했습니다');
  }

  return res.data.data;
}

export async function fetchScheduleList(params: ScheduleListParams): Promise<ScheduleListResponse> {
  const queryParams: Record<string, string> = {};
  if (params.page != null) queryParams.page = String(params.page);
  if (params.size != null) queryParams.size = String(params.size);
  if (params.employeeCode) queryParams.employeeCode = params.employeeCode;
  if (params.accountName) queryParams.accountName = params.accountName;
  if (params.accountType) queryParams.accountType = params.accountType;
  if (params.accountStatus) queryParams.accountStatus = params.accountStatus;
  if (params.confirmed != null) queryParams.confirmed = String(params.confirmed);
  if (params.typeOfWork3) queryParams.typeOfWork3 = params.typeOfWork3;
  if (params.periodStart) queryParams.periodStart = params.periodStart;
  if (params.periodEnd) queryParams.periodEnd = params.periodEnd;
  if (params.preset) queryParams.preset = params.preset;
  if (params.validData) queryParams.validData = params.validData;
  if (params.branchCode) queryParams.branchCode = params.branchCode;
  if (params.sortBy) queryParams.sortBy = params.sortBy;
  if (params.sortDir) queryParams.sortDir = params.sortDir;

  const res = await client.get<ApiResponse<PageRaw<ScheduleListItem>>>(
    '/api/v1/admin/display-work-schedule/list',
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
    '/api/v1/admin/display-work-schedule/confirm',
    { ids },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '일괄 확정에 실패했습니다');
  }

  return res.data.data;
}

export async function fetchScheduleDetail(id: number): Promise<ScheduleDetail> {
  const res = await client.get<ApiResponse<ScheduleDetail>>(`/api/v1/admin/display-work-schedule/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createSchedule(payload: ScheduleCreateRequest): Promise<ScheduleCreateResult> {
  const res = await client.post<ApiResponse<ScheduleCreateResult>>(
    '/api/v1/admin/display-work-schedule',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '스케줄 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateSchedule(id: number, payload: ScheduleUpdateRequest): Promise<ScheduleCreateResult> {
  const res = await client.put<ApiResponse<ScheduleCreateResult>>(
    `/api/v1/admin/display-work-schedule/${id}`,
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '스케줄 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function batchDeleteSchedules(ids: number[]): Promise<ScheduleBatchDeleteResult> {
  const res = await client.post<ApiResponse<ScheduleBatchDeleteResult>>(
    '/api/v1/admin/display-work-schedule/batch-delete',
    { ids },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '일괄 삭제에 실패했습니다');
  }
  return res.data.data;
}

export async function batchUnconfirmSchedules(ids: number[]): Promise<ScheduleBatchUnconfirmResult> {
  const res = await client.patch<ApiResponse<ScheduleBatchUnconfirmResult>>(
    '/api/v1/admin/display-work-schedule/unconfirm',
    { ids },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '확정 해제에 실패했습니다');
  }

  return res.data.data;
}
