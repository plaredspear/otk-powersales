import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';


export interface MonthlyIntegrationScheduleItem {
  branchName: string;
  accountBranchName: string | null;
  accountCode: string;
  accountName: string;
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
): Promise<MonthlyIntegrationScheduleResponse> {
  const trimmedKeyword = keyword?.trim();
  const trimmedAccountKeyword = accountKeyword?.trim();
  const res = await client.get<ApiResponse<MonthlyIntegrationScheduleResponse>>(
    '/api/v1/admin/schedules/monthly-integration',
    {
      params: {
        year,
        month,
        costCenterCodes: costCenterCodes.join(','),
        ...(trimmedKeyword ? { keyword: trimmedKeyword } : {}),
        ...(trimmedAccountKeyword ? { accountKeyword: trimmedAccountKeyword } : {}),
      },
    },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '통합일정 조회에 실패했습니다');
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
): Promise<void> {
  const trimmedKeyword = keyword?.trim();
  const trimmedAccountKeyword = accountKeyword?.trim();
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
