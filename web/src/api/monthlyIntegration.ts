import client from './client';
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
): Promise<MonthlyIntegrationScheduleResponse> {
  const res = await client.get<ApiResponse<MonthlyIntegrationScheduleResponse>>(
    '/api/v1/admin/schedules/monthly-integration',
    { params: { year, month, costCenterCodes: costCenterCodes.join(',') } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '통합일정 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchMonthlyIntegrationExport(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  const res = await client.get('/api/v1/admin/schedules/monthly-integration/export', {
    params: { year, month, costCenterCodes: costCenterCodes.join(',') },
    responseType: 'blob',
  });

  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${year}년${month}월_여사원_통합일정.xlsx`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
    if (match) filename = decodeURIComponent(match[1]);
  }

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
    throw new Error(res.data.error?.message || res.data.message || '근무형태별 인원현황 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchCategoryExport(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  const res = await client.get('/api/v1/admin/schedules/monthly-integration/category/export', {
    params: { year, month, costCenterCodes: costCenterCodes.join(',') },
    responseType: 'blob',
  });

  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${year}년${month}월_근무형태별_인원현황.xlsx`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
    if (match) filename = decodeURIComponent(match[1]);
  }

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
