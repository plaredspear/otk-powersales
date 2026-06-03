import client from './client';
import type { ApiResponse } from './types';

export interface SalesProgressRateMasterListParams {
  keyword?: string;
  targetYear?: string;
  targetMonth?: string;
  page: number;
  size: number;
}

export interface SalesProgressRateMasterListItem {
  id: number;
  name: string | null;
  targetYear: string | null;
  targetMonth: string | null;
  accountName: string | null;
  accountBranchName: string | null;
  accountCode: string | null;
  accountType: string | null;
  rtTargetAmount: number | null;
  rmTargetAmount: number | null;
  frTargetAmount: number | null;
  foTargetAmount: number | null;
  targetSum: number;
  currentMonthSalesAmount: number | null;
  previousMonthSalesAmount: number | null;
  progressRate: number | null;
}

export interface SalesProgressRateMasterListData {
  content: SalesProgressRateMasterListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SalesProgressRateMasterDetail {
  id: number;
  name: string | null;
  targetYear: string | null;
  targetMonth: string | null;
  accountId: number | null;
  accountName: string | null;
  accountBranchName: string | null;
  accountCode: string | null;
  accountType: string | null;
  rtTargetAmount: number | null;
  rmTargetAmount: number | null;
  frTargetAmount: number | null;
  foTargetAmount: number | null;
  targetSum: number;
  targetSumAmount: number | null;
  currentMonthSalesAmount: number | null;
  previousMonthSalesAmount: number | null;
  progressRate: number | null;
  businessRate: number | null;
  externalKey: string | null;
  accountBranchView: string | null;
  createdByName: string | null;
  lastModifiedByName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export async function fetchSalesProgressRateMasters(
  params: SalesProgressRateMasterListParams,
): Promise<SalesProgressRateMasterListData> {
  const queryParams: Record<string, string | number> = {
    page: params.page,
    size: params.size,
  };
  if (params.keyword) queryParams.keyword = params.keyword;
  if (params.targetYear) queryParams.targetYear = params.targetYear;
  if (params.targetMonth) queryParams.targetMonth = params.targetMonth;

  const res = await client.get<ApiResponse<SalesProgressRateMasterListData>>(
    '/api/v1/admin/sales-progress-rate-masters',
    { params: queryParams },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처목표등록마스터 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchSalesProgressRateMaster(
  id: number,
): Promise<SalesProgressRateMasterDetail> {
  const res = await client.get<ApiResponse<SalesProgressRateMasterDetail>>(
    `/api/v1/admin/sales-progress-rate-masters/${id}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처목표등록마스터 조회에 실패했습니다');
  }
  return res.data.data;
}
