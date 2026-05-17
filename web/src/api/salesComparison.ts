import client from './client';
import type { ApiResponse } from './types';

export interface SalesComparisonSummaryRow {
  suitability: string;
  totalCount: number;
  countsByCategory: Record<string, number>;
  accountIdsByCategory: Record<string, number[]>;
}

export interface SalesComparisonSummaryResponse {
  year: number;
  month: number;
  rows: SalesComparisonSummaryRow[];
  total: SalesComparisonSummaryRow;
}

export interface SalesComparisonMiddleItem {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountBranchName: string | null;
  accountCategory: string;
  suitability: string;
  avgClosingAmount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  fixedStandardAmount: number | null;
  bifurcationHalfStandardAmount: number | null;
  totalInputCount: number;
  totalEquivalentWorkingDays: number;
  thisMonthSalesAmount: number;
  ediPos: string | null;
}

export interface SalesComparisonMiddleSubtotal {
  suitability: string;
  accountCount: number;
  avgClosingAmount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  totalInputCount: number;
  totalEquivalentWorkingDays: number;
  thisMonthSalesAmount: number;
}

export interface SalesComparisonMiddleResponse {
  year: number;
  month: number;
  items: SalesComparisonMiddleItem[];
  subtotals: SalesComparisonMiddleSubtotal[];
  total: SalesComparisonMiddleSubtotal;
}

export interface SalesComparisonDetailItem {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountBranchName: string | null;
  accountCategory: string;
  accountCategoryCode: string | null;
  employeeCode: string;
  employeeName: string;
  title: string | null;
  workingCategory1: string;
  workingCategory3: string | null;
  workingCategory4: string | null;
  workingCategory5: string | null;
  suitability: string;
  avgClosingAmount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  fixedStandardAmount: number | null;
  bifurcationHalfStandardAmount: number | null;
  inputCount: number;
  equivalentWorkingDays: number;
  convertedHeadcount: number;
  thisMonthSalesAmount: number;
  ediPos: string | null;
}

export interface SalesComparisonDetailTotal {
  rowCount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  totalInputCount: number;
  totalEquivalentWorkingDays: number;
  totalConvertedHeadcount: number;
  totalThisMonthSalesAmount: number;
}

export interface SalesComparisonDetailResponse {
  year: number;
  month: number;
  items: SalesComparisonDetailItem[];
  total: SalesComparisonDetailTotal;
}

export interface SearchAccountCategoryItem {
  accountCode: string;
  name: string;
}

const BASE = '/api/v1/admin/schedules/sales-comparison';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

export async function fetchSearchCategories(): Promise<SearchAccountCategoryItem[]> {
  const res = await client.get<ApiResponse<SearchAccountCategoryItem[]>>(`${BASE}/categories`);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('거래처유형', res));
  return res.data.data;
}

export async function fetchSummary(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<SalesComparisonSummaryResponse> {
  const res = await client.get<ApiResponse<SalesComparisonSummaryResponse>>(`${BASE}/summary`, {
    params: { year, month, costCenterCodes: costCenterCodes.join(',') },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('집계', res));
  return res.data.data;
}

export async function fetchMiddle(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
): Promise<SalesComparisonMiddleResponse> {
  const res = await client.get<ApiResponse<SalesComparisonMiddleResponse>>(`${BASE}/middle`, {
    params: {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('중간집계', res));
  return res.data.data;
}

export async function fetchDetail(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
  workingCategory1?: string,
  workingCategory5?: string,
): Promise<SalesComparisonDetailResponse> {
  const res = await client.get<ApiResponse<SalesComparisonDetailResponse>>(`${BASE}/detail`, {
    params: {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
      ...(workingCategory1 ? { workingCategory1 } : {}),
      ...(workingCategory5 ? { workingCategory5 } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('상세', res));
  return res.data.data;
}

async function downloadBlob(path: string, params: Record<string, unknown>, fallbackName: string): Promise<void> {
  const res = await client.get(path, { params, responseType: 'blob' });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = fallbackName;
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

export async function exportSummary(year: number, month: number, costCenterCodes: string[]): Promise<void> {
  await downloadBlob(
    `${BASE}/summary/export`,
    { year, month, costCenterCodes: costCenterCodes.join(',') },
    `${year}년${month}월_월평균_매출대비_여사원배치_현황_집계.xlsx`,
  );
}

export async function exportMiddle(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
): Promise<void> {
  await downloadBlob(
    `${BASE}/middle/export`,
    {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
    },
    `${year}년${month}월_월평균_매출대비_여사원배치_현황_중간집계.xlsx`,
  );
}

export async function exportDetail(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
  workingCategory1?: string,
  workingCategory5?: string,
): Promise<void> {
  await downloadBlob(
    `${BASE}/detail/export`,
    {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
      ...(workingCategory1 ? { workingCategory1 } : {}),
      ...(workingCategory5 ? { workingCategory5 } : {}),
    },
    `${year}년${month}월_월평균_매출대비_여사원배치_현황_상세.xlsx`,
  );
}
