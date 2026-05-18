import client from './client';
import type { ApiResponse } from './types';

/**
 * 월매출 대시보드 — 상단 KPI + 월별 추이 응답.
 */
export interface MonthlyTrendPoint {
  salesYear: number;
  salesMonth: number;
  targetAmount: number;
  achievedAmount: number;
  lastYearAchievedAmount: number | null;
}

export interface MonthlySalesDashboardSummary {
  salesYear: number;
  salesMonth: number;
  totalTargetAmount: number;
  totalAchievedAmount: number;
  overallAchievementRate: number;
  referenceAchievementRate: number;
  totalLastYearAchievedAmount: number | null;
  lastYearComparisonRatio: number | null;
  monthlyTrend: MonthlyTrendPoint[];
}

export interface MonthlySalesDashboardListItem {
  accountId: number;
  accountSfid: string | null;
  accountName: string | null;
  sapAccountCode: string | null;
  branchCode: string | null;
  branchName: string | null;
  salesYear: number;
  salesMonth: number;
  targetAmount: number | null;
  totalAchievedAmount: number | null;
  achievementRate: number | null;
  ambientAchievedAmount: number | null;
  noodleAchievedAmount: number | null;
  frozenRefrigeratedAchievedAmount: number | null;
  oilFatAchievedAmount: number | null;
  lastYearAchievedAmount: number | null;
  lastYearComparisonRatio: number | null;
  isConfirmed: boolean;
}

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MonthlySalesDashboardListResponse {
  items: MonthlySalesDashboardListItem[];
  pageInfo: PageInfo;
}

export interface MonthlySalesCategorySales {
  category: string;
  targetAmount: number;
  achievedAmount: number;
  achievementRate: number;
}

export interface MonthlySalesYearComparison {
  currentYear: number;
  previousYear: number;
}

export interface MonthlySalesMonthlyAverage {
  currentYearAverage: number;
  previousYearAverage: number;
  startMonth: number;
  endMonth: number;
}

export interface MonthlySalesDashboardDetail {
  customerId: number;
  customerName: string | null;
  salesYear: number;
  salesMonth: number;
  targetAmount: number;
  achievedAmount: number;
  achievementRate: number;
  referenceAchievementRate: number;
  categorySales: MonthlySalesCategorySales[];
  yearComparison: MonthlySalesYearComparison;
  monthlyAverage: MonthlySalesMonthlyAverage;
}

export interface MonthlySalesDashboardListRequest {
  year: number;
  month: number;
  costCenterCodes: string[];
  accountIds?: number[];
  accountGroup?: string;
  customerKeyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const BASE = '/api/v1/admin/sales/monthly';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 상단 KPI + 월별 추이 조회.
 */
export async function fetchSummary(
  year: number,
  month: number,
  costCenterCodes: string[],
  customerKeyword?: string,
  accountGroup?: string,
): Promise<MonthlySalesDashboardSummary> {
  const res = await client.get<ApiResponse<MonthlySalesDashboardSummary>>(`${BASE}/summary`, {
    params: {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(customerKeyword ? { customerKeyword } : {}),
      ...(accountGroup ? { accountGroup } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월매출 요약', res));
  return res.data.data;
}

/**
 * 거래처별 명세 — 페이징 + 정렬 + 필터.
 */
export async function fetchList(
  request: MonthlySalesDashboardListRequest,
): Promise<MonthlySalesDashboardListResponse> {
  const res = await client.get<ApiResponse<MonthlySalesDashboardListResponse>>(`${BASE}/list`, {
    params: {
      year: request.year,
      month: request.month,
      costCenterCodes: request.costCenterCodes.join(','),
      ...(request.accountIds && request.accountIds.length > 0
        ? { accountIds: request.accountIds.join(',') }
        : {}),
      ...(request.accountGroup ? { accountGroup: request.accountGroup } : {}),
      ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
      ...(request.page !== undefined ? { page: request.page } : {}),
      ...(request.size !== undefined ? { size: request.size } : {}),
      ...(request.sort ? { sort: request.sort } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월매출 명세', res));
  return res.data.data;
}

/**
 * 거래처별 명세 엑셀 다운로드 — blob → anchor click.
 */
export async function exportList(request: MonthlySalesDashboardListRequest): Promise<void> {
  const res = await client.get(`${BASE}/list/export`, {
    params: {
      year: request.year,
      month: request.month,
      costCenterCodes: request.costCenterCodes.join(','),
      ...(request.accountIds && request.accountIds.length > 0
        ? { accountIds: request.accountIds.join(',') }
        : {}),
      ...(request.accountGroup ? { accountGroup: request.accountGroup } : {}),
      ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
      ...(request.sort ? { sort: request.sort } : {}),
    },
    responseType: 'blob',
  });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  const monthStr = String(request.month).padStart(2, '0');
  let filename = `monthly-sales-${request.year}-${monthStr}.xlsx`;
  if (contentDisposition) {
    const utfMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
    if (utfMatch) {
      filename = decodeURIComponent(utfMatch[1]);
    } else {
      const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
      if (match) filename = decodeURIComponent(match[1]);
    }
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

/**
 * 단건 거래처 상세 (모바일 동등 6 영역).
 */
export async function fetchDetail(
  customerId: number,
  year: number,
  month: number,
): Promise<MonthlySalesDashboardDetail> {
  const res = await client.get<ApiResponse<MonthlySalesDashboardDetail>>(
    `${BASE}/detail/${customerId}`,
    { params: { year, month } },
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월매출 상세', res));
  return res.data.data;
}
