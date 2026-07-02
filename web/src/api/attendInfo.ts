import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';
import type { Branch, TeamMember } from './team-schedule';

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

/**
 * 근무기간 조회 "지점 선택" 옵션 — 권한별 조회 허용 지점 (attend_info READ).
 */
export async function fetchAttendInfoBranches(): Promise<Branch[]> {
  const res = await client.get<ApiResponse<Branch[]>>(`${BASE}/branches`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 근무기간 조회 좌측 여사원 선택 목록.
 *
 * 여사원 일정관리의 form members 와 달리 퇴사/휴직 등 비활성 여사원도 포함 (과거 근무내역 조회).
 * 화면 도메인 권한(attend_info READ)으로 가드되는 전용 엔드포인트.
 *
 * `branchCode` 지정 시 (다중/전사 권한자가 지점 선택) 해당 지점 여사원을 조회 — backend 가 권한 검증.
 */
export async function fetchAttendInfoMembers(branchCode?: string): Promise<TeamMember[]> {
  const res = await client.get<ApiResponse<TeamMember[]>>(`${BASE}/members`, {
    params: branchCode ? { branchCode } : undefined,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '여사원 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 기간별 근무내역(개인) — 여사원별 근무 집계 항목.
 * 근무기간 조회(월별근무내역 목록)와 동일하게 TeamMemberSchedule(출근 등록 기준)을 원천으로 하되,
 * 단일 월이 아닌 기간(시작년월~종료년월) 전체를 여사원별 1행으로 집계.
 */
export interface WorkHistoryPeriodSummaryItem {
  /** 소속지점명 */
  orgName: string | null;
  /** 사번 */
  employeeCode: string | null;
  /** 이름 */
  employeeName: string | null;
  /** 직위 */
  title: string | null;
  /** 총 근무일수 */
  totalWorkingDays: number;
  /** 근무 거래처 수 */
  workingAccountCount: number;
  /** 근무유형별 일수 — 진열 */
  displayDays: number;
  /** 근무유형별 일수 — 행사 */
  eventDays: number;
  /** 구분별 일수 — 근무 */
  workDays: number;
  /** 구분별 일수 — 연차 */
  annualLeaveDays: number;
  /** 구분별 일수 — 대휴 */
  altHolidayDays: number;
  /** 월별 통계 분해 (yyyy-MM 오름차순). 단일 월 조회 시 빈 배열. */
  monthlyBreakdown: WorkHistoryMonthlyStat[];
}

/** 여사원별 월 단위 근무 통계 — 합계 행의 월별 분해. */
export interface WorkHistoryMonthlyStat {
  /** 대상 년월 (yyyy-MM) */
  yearMonth: string;
  totalWorkingDays: number;
  workingAccountCount: number;
  displayDays: number;
  eventDays: number;
  workDays: number;
  annualLeaveDays: number;
  altHolidayDays: number;
}

export interface WorkHistoryPeriodSummaryResponse {
  fromYearMonth: string;
  toYearMonth: string;
  items: WorkHistoryPeriodSummaryItem[];
  totalCount: number;
}

export interface FetchWorkHistoryPeriodSummaryParams {
  /** 시작년월 (yyyy-MM) */
  fromYearMonth: string;
  /** 종료년월 (yyyy-MM) */
  toYearMonth: string;
  /** 조회 지점 코드 (costCenterCode) — 비우면 권한 스코프 전체 */
  costCenterCodes: string[];
  /** 사번/이름 검색어 */
  keyword?: string;
}

/**
 * 기간별 근무내역(개인) 집계 조회.
 * 지점 스코프 내 전체 여사원의 기간 근무 집계를 여사원별 1행으로 반환.
 */
export async function fetchWorkHistoryPeriodSummary(
  params: FetchWorkHistoryPeriodSummaryParams,
): Promise<WorkHistoryPeriodSummaryResponse> {
  const trimmedKeyword = params.keyword?.trim();
  const res = await client.get<ApiResponse<WorkHistoryPeriodSummaryResponse>>(`${BASE}/period-summary`, {
    params: {
      fromYearMonth: params.fromYearMonth,
      toYearMonth: params.toYearMonth,
      costCenterCodes: params.costCenterCodes.join(','),
      ...(trimmedKeyword ? { keyword: trimmedKeyword } : {}),
    },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '기간별 근무내역 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 기간별 근무내역(개인) — 특정 여사원 1명의 거래처별 근무 집계 행.
 * 좌측 패널에서 여사원을 선택하면 선택 기간 내 근무 행을 거래처 단위로 그룹핑해 표시.
 * 거래처 미연결 행(연차/대휴 등)은 accountName=null 1행으로 묶인다.
 */
export interface WorkHistoryAccountStat {
  /** 거래처명. 거래처 미연결 행 묶음이면 null. */
  accountName: string | null;
  /** 거래처 코드 (externalKey). 거래처 미연결이면 null. */
  accountExternalKey: string | null;
  totalWorkingDays: number;
  displayDays: number;
  eventDays: number;
  workDays: number;
  annualLeaveDays: number;
  altHolidayDays: number;
}

export interface WorkHistoryEmployeeAccountResponse {
  fromYearMonth: string;
  toYearMonth: string;
  employeeCode: string;
  employeeName: string | null;
  items: WorkHistoryAccountStat[];
  totalCount: number;
}

export interface FetchWorkHistoryEmployeeAccountParams {
  /** 조회 대상 여사원 사번 */
  employeeCode: string;
  /** 시작년월 (yyyy-MM) */
  fromYearMonth: string;
  /** 종료년월 (yyyy-MM) */
  toYearMonth: string;
}

/**
 * 기간별 근무내역(개인) — 여사원 1명의 거래처별 집계 조회.
 * 지점 스코프 밖 사번을 지정하면 백엔드가 빈 결과를 반환.
 */
export async function fetchWorkHistoryEmployeeAccounts(
  params: FetchWorkHistoryEmployeeAccountParams,
): Promise<WorkHistoryEmployeeAccountResponse> {
  const res = await client.get<ApiResponse<WorkHistoryEmployeeAccountResponse>>(
    `${BASE}/period-summary/accounts`,
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '거래처별 근무내역 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 기간별 근무내역(개인) 엑셀 다운로드 — 조회와 동일 필터/스코프. */
export async function fetchWorkHistoryPeriodSummaryExport(
  params: FetchWorkHistoryPeriodSummaryParams,
): Promise<void> {
  const trimmedKeyword = params.keyword?.trim();
  await downloadExcel(
    `${BASE}/period-summary/export`,
    `기간별근무내역_${params.fromYearMonth}_${params.toYearMonth}.xlsx`,
    {
      params: {
        fromYearMonth: params.fromYearMonth,
        toYearMonth: params.toYearMonth,
        costCenterCodes: params.costCenterCodes.join(','),
        ...(trimmedKeyword ? { keyword: trimmedKeyword } : {}),
      },
    },
  );
}
