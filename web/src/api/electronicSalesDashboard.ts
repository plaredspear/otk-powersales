import client from './client';
import type { ApiResponse } from './types';

/**
 * 월 매출(전산실적) — POS `live_tot_sales_dh` 거래처/제품별 전산매출.
 *
 * 레거시 「월 매출 조회(전산)」(`/sales/abcMain`) 동등. 거래처별 합계 명세 + 제품별 상세.
 * 기간은 일 단위(startDate~endDate, 최대 3개월) — 레거시 daterangepicker 정합.
 */

export interface ElectronicSalesDashboardListItem {
  accountId: number;
  accountName: string | null;
  sapAccountCode: string | null;
  branchCode: string | null;
  branchName: string | null;
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
  startDate: string;
  endDate: string;
  /** 조회 결과 전체(페이징 무관)의 전산매출 금액 합계 — 상단 합계 표시용 */
  totalSalesAmount: number;
  /** 조회 결과 전체(페이징 무관)의 전산매출 수량 합계 */
  totalSalesQuantity: number;
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
  startDate: string;
  endDate: string;
  totalAmount: number;
  totalQuantity: number;
  items: ElectronicSalesProductSales[];
}

/** 조회 조건 옵션 — 유통형태 / 거래처유형 / 제품 중·소분류 (메인 DB distinct). */
export interface ElectronicSalesFilterOptions {
  distributionChannels: string[];
  accountTypes: string[];
  categories: { category2: string; category3s: string[] }[];
}

/** 조회 조건 제품 검색 결과 1건 (바코드 보유 제품 한정). */
export interface ElectronicSalesProductLookupItem {
  productId: number;
  name: string | null;
  productCode: string | null;
  barcode: string;
}

/** 목록/엑셀 공용 필터 — 제품/분류 조건은 backend 가 바코드로 해소해 POS 에 전달. */
export interface ElectronicSalesDashboardListRequest {
  /** 시작일 (YYYY-MM-DD) */
  startDate: string;
  /** 종료일 (YYYY-MM-DD) */
  endDate: string;
  costCenterCodes: string[];
  accountIds?: number[];
  accountGroup?: string;
  customerKeyword?: string;
  /** 유통형태 라벨 (예 "02 슈퍼") */
  distributionChannels?: string[];
  /** 거래처유형 라벨 (ABC유형, 예 "6111 이마트") */
  accountTypes?: string[];
  /** 조회 제품 (다중) */
  productIds?: number[];
  /** 제품 중분류 */
  category2?: string;
  /** 제품 소분류 */
  category3?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const BASE = '/api/v1/admin/sales/electronic';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 공통 필터 파라미터 직렬화 (목록/엑셀 공용, 페이징 제외). */
function buildFilterParams(request: ElectronicSalesDashboardListRequest): Record<string, string> {
  return {
    startDate: request.startDate,
    endDate: request.endDate,
    costCenterCodes: request.costCenterCodes.join(','),
    ...(request.accountIds && request.accountIds.length > 0
      ? { accountIds: request.accountIds.join(',') }
      : {}),
    ...(request.accountGroup ? { accountGroup: request.accountGroup } : {}),
    ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
    ...(request.distributionChannels && request.distributionChannels.length > 0
      ? { distributionChannels: request.distributionChannels.join(',') }
      : {}),
    ...(request.accountTypes && request.accountTypes.length > 0
      ? { accountTypes: request.accountTypes.join(',') }
      : {}),
    ...(request.productIds && request.productIds.length > 0
      ? { productIds: request.productIds.join(',') }
      : {}),
    ...(request.category2 ? { category2: request.category2 } : {}),
    ...(request.category3 ? { category3: request.category3 } : {}),
    ...(request.sort ? { sort: request.sort } : {}),
  };
}

/**
 * 거래처별 전산매출 명세 — 페이징 + 정렬 + 필터 (+ 전체 합계).
 */
export async function fetchList(
  request: ElectronicSalesDashboardListRequest,
): Promise<ElectronicSalesDashboardListResponse> {
  const res = await client.get<ApiResponse<ElectronicSalesDashboardListResponse>>(`${BASE}/list`, {
    params: {
      ...buildFilterParams(request),
      ...(request.page !== undefined ? { page: request.page } : {}),
      ...(request.size !== undefined ? { size: request.size } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('전산실적 명세', res));
  return res.data.data;
}

/** 거래처별 전산매출 명세 엑셀 export 파라미터 (페이징 제외). */
export function exportListParams(request: ElectronicSalesDashboardListRequest): Record<string, string> {
  return buildFilterParams(request);
}

/** 거래처별 전산매출 명세 엑셀 다운로드 경로. */
export const EXPORT_LIST_PATH = `${BASE}/list/export`;

/** 상세 조회의 제품/분류 필터 (목록과 동일 조건 반영 — 행 합계와 정합). */
export interface ElectronicSalesDetailFilter {
  productIds?: number[];
  category2?: string;
  category3?: string;
}

/**
 * 단건 거래처 상세 — 제품별 전산매출 명세 (기간 + 목록과 동일한 제품/분류 필터).
 */
export async function fetchDetail(
  customerId: number,
  startDate: string,
  endDate: string,
  filter: ElectronicSalesDetailFilter = {},
): Promise<ElectronicSalesDashboardDetail> {
  const res = await client.get<ApiResponse<ElectronicSalesDashboardDetail>>(
    `${BASE}/detail/${customerId}`,
    {
      params: {
        startDate,
        endDate,
        ...(filter.productIds && filter.productIds.length > 0
          ? { productIds: filter.productIds.join(',') }
          : {}),
        ...(filter.category2 ? { category2: filter.category2 } : {}),
        ...(filter.category3 ? { category3: filter.category3 } : {}),
      },
    },
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('전산실적 상세', res));
  return res.data.data;
}

/**
 * 조회 조건 옵션 조회 — 유통형태 / 거래처유형 / 제품 중·소분류.
 */
export async function fetchFilterOptions(): Promise<ElectronicSalesFilterOptions> {
  const res = await client.get<ApiResponse<ElectronicSalesFilterOptions>>(`${BASE}/filter-options`);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('조회 조건 옵션', res));
  return res.data.data;
}

/**
 * 조회 조건 제품 검색 — 제품명/제품코드/바코드 부분일치 (최대 50건).
 */
export async function fetchProductLookup(keyword: string): Promise<ElectronicSalesProductLookupItem[]> {
  const res = await client.get<ApiResponse<ElectronicSalesProductLookupItem[]>>(
    `${BASE}/product-lookup`,
    { params: { keyword } },
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('제품 검색', res));
  return res.data.data;
}
