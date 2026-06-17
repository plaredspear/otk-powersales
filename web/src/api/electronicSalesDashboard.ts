import client from './client';
import type { ApiResponse } from './types';

/**
 * 월 매출(전산실적) — POS `live_tot_sales_dh` 거래처/제품별 전산매출.
 *
 * 레거시 「월 매출 조회(전산)」(`/sales/abcMain`) 동등. 거래처별 합계 명세 + 제품별 상세.
 */

export interface ElectronicSalesDashboardListItem {
  accountId: number;
  accountName: string | null;
  sapAccountCode: string | null;
  branchCode: string | null;
  branchName: string | null;
  salesYear: number;
  salesMonth: number;
  salesAmount: number;
  salesQuantity: number;
}

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ElectronicSalesDashboardListResponse {
  items: ElectronicSalesDashboardListItem[];
  pageInfo: PageInfo;
}

export interface ElectronicSalesProductSales {
  productCode: string;
  productName: string;
  amount: number;
  quantity: number;
}

export interface ElectronicSalesDashboardDetail {
  customerId: number;
  customerName: string | null;
  sapAccountCode: string | null;
  salesYear: number;
  salesMonth: number;
  totalAmount: number;
  totalQuantity: number;
  items: ElectronicSalesProductSales[];
}

export interface ElectronicSalesDashboardListRequest {
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

const BASE = '/api/v1/admin/sales/electronic';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 거래처별 전산매출 명세 — 페이징 + 정렬 + 필터.
 */
export async function fetchList(
  request: ElectronicSalesDashboardListRequest,
): Promise<ElectronicSalesDashboardListResponse> {
  const res = await client.get<ApiResponse<ElectronicSalesDashboardListResponse>>(`${BASE}/list`, {
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
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('전산실적 명세', res));
  return res.data.data;
}

/** 거래처별 전산매출 명세 엑셀 export 파라미터 (페이징 제외). */
export function exportListParams(request: ElectronicSalesDashboardListRequest): Record<string, string> {
  return {
    year: String(request.year),
    month: String(request.month),
    costCenterCodes: request.costCenterCodes.join(','),
    ...(request.accountIds && request.accountIds.length > 0
      ? { accountIds: request.accountIds.join(',') }
      : {}),
    ...(request.accountGroup ? { accountGroup: request.accountGroup } : {}),
    ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
    ...(request.sort ? { sort: request.sort } : {}),
  };
}

/** 거래처별 전산매출 명세 엑셀 다운로드 경로. */
export const EXPORT_LIST_PATH = `${BASE}/list/export`;

/**
 * 단건 거래처 상세 — 제품별 전산매출 명세.
 */
export async function fetchDetail(
  customerId: number,
  year: number,
  month: number,
): Promise<ElectronicSalesDashboardDetail> {
  const res = await client.get<ApiResponse<ElectronicSalesDashboardDetail>>(
    `${BASE}/detail/${customerId}`,
    { params: { year, month } },
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('전산실적 상세', res));
  return res.data.data;
}
