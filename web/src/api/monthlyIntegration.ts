import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';


export interface MonthlyIntegrationScheduleItem {
  /** MFEIS row PK — 상세 조회 진입 키 */
  id: number | null;
  branchName: string;
  accountBranchName: string | null;
  accountCode: string;
  accountName: string;
  /** 유통형태 — 거래처상태코드 + 거래처유형명 조합 (예: "02 슈퍼") */
  distributionChannelLabel: string | null;
  /** 거래처유형 — ABC유형코드 + ABC유형 조합 (예: "6111 이마트") */
  abcTypeLabel: string | null;
  employeeCode: string;
  title: string | null;
  employeeName: string;
  workingCategory1: string;
  workingCategory3: string | null;
  workingCategory4: string | null;
  workingCategory5: string | null;
  totalInputCount: number;
  equivalentWorkingDays: number;
  convertedHeadcount: number;
  avgClosingAmount: number;
}

export interface MonthlyIntegrationScheduleResponse {
  year: number;
  month: number;
  items: MonthlyIntegrationScheduleItem[];
  totalCount: number;
}

/** 집계 근거 여사원일정 1건 — equivalentContribution = 1/dailyScheduleCount */
export interface MonthlyIntegrationSourceScheduleItem {
  scheduleId: number;
  workingDate: string;
  accountCode: string | null;
  accountName: string | null;
  workingCategory1: string | null;
  workingCategory3: string | null;
  workingCategory4: string | null;
  workingCategory5: string | null;
  /** 출근보고 일시 (ISO) */
  attendanceReportedAt: string | null;
  /** 그날 사원의 출근 일정 수 N (거래처 무관) */
  dailyScheduleCount: number;
  /** 환산근무일수 기여분 1/N */
  equivalentContribution: number;
}

export interface MonthlyIntegrationDetailResponse {
  id: number;
  year: number;
  month: number;
  branchName: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  accountCode: string | null;
  accountName: string | null;
  workingCategory1: string | null;
  workingCategory3: string | null;
  workingCategory4: string | null;
  workingCategory5: string | null;
  workingDaysMonth: number;
  totalInputCount: number;
  equivalentWorkingDays: number;
  convertedHeadcount: number;
  schedules: MonthlyIntegrationSourceScheduleItem[];
}

export interface CategoryScheduleItem {
  branchName: string;
  currentMonthTotal: number;
  previousMonthTotal: number;
  totalChange: number;
  displayFixed: number;
  displayAlternate: number;
  displayPatrol: number;
  currentMonthDisplayTotal: number;
  previousMonthDisplayTotal: number;
  displayChange: number;
  eventAmbient: number;
  eventFrozenChilled: number;
  currentMonthEventTotal: number;
  previousMonthEventTotal: number;
  eventChange: number;
}

export interface CategoryScheduleResponse {
  year: number;
  month: number;
  items: CategoryScheduleItem[];
}


// --- API functions ---

export async function fetchMonthlyIntegrationSchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
  keyword?: string,
  accountKeyword?: string,
  distributionKeyword?: string,
  accountTypeKeyword?: string,
): Promise<MonthlyIntegrationScheduleResponse> {
  const trimmedKeyword = keyword?.trim();
  const trimmedAccountKeyword = accountKeyword?.trim();
  const trimmedDistribution = distributionKeyword?.trim();
  const trimmedAccountType = accountTypeKeyword?.trim();
  const res = await client.get<ApiResponse<MonthlyIntegrationScheduleResponse>>(
    '/api/v1/admin/schedules/monthly-integration',
    {
      params: {
        year,
        month,
        costCenterCodes: costCenterCodes.join(','),
        ...(trimmedKeyword ? { keyword: trimmedKeyword } : {}),
        ...(trimmedAccountKeyword ? { accountKeyword: trimmedAccountKeyword } : {}),
        ...(trimmedDistribution ? { distributionKeyword: trimmedDistribution } : {}),
        ...(trimmedAccountType ? { accountTypeKeyword: trimmedAccountType } : {}),
      },
    },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '통합일정 조회에 실패했습니다');
  }
  return res.data.data;
}

/** MFEIS row 상세 — 집계 근거가 된 여사원일정 목록. */
export async function fetchMonthlyIntegrationDetail(
  id: number,
): Promise<MonthlyIntegrationDetailResponse> {
  const res = await client.get<ApiResponse<MonthlyIntegrationDetailResponse>>(
    `/api/v1/admin/schedules/monthly-integration/${id}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '통합일정 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 여사원 통합일정 엑셀 다운로드. */
export async function fetchMonthlyIntegrationExport(
  year: number,
  month: number,
  costCenterCodes: string[],
  keyword?: string,
  accountKeyword?: string,
  distributionKeyword?: string,
  accountTypeKeyword?: string,
): Promise<void> {
  const trimmedKeyword = keyword?.trim();
  const trimmedAccountKeyword = accountKeyword?.trim();
  const trimmedDistribution = distributionKeyword?.trim();
  const trimmedAccountType = accountTypeKeyword?.trim();
  await downloadExcel(
    '/api/v1/admin/schedules/monthly-integration/export',
    `${year}년${month}월_여사원_통합일정.xlsx`,
    {
      params: {
        year,
        month,
        costCenterCodes: costCenterCodes.join(','),
        ...(trimmedKeyword ? { keyword: trimmedKeyword } : {}),
        ...(trimmedAccountKeyword ? { accountKeyword: trimmedAccountKeyword } : {}),
        ...(trimmedDistribution ? { distributionKeyword: trimmedDistribution } : {}),
        ...(trimmedAccountType ? { accountTypeKeyword: trimmedAccountType } : {}),
      },
    },
  );
}

export async function fetchCategorySchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<CategoryScheduleResponse> {
  const res = await client.get<ApiResponse<CategoryScheduleResponse>>(
    '/api/v1/admin/schedules/monthly-integration/category',
    { params: { year, month, costCenterCodes: costCenterCodes.join(',') } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '근무형태별 여사원인원현황 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 근무형태별 여사원인원현황 엑셀 다운로드. */
export async function fetchCategoryExport(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  await downloadExcel(
    '/api/v1/admin/schedules/monthly-integration/category/export',
    `${year}년${month}월_근무형태별_인원현황.xlsx`,
    { params: { year, month, costCenterCodes: costCenterCodes.join(',') } },
  );
}
