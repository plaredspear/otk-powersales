import client from './client';
import type { ApiResponse } from './types';

/**
 * POS매출 — POS `live_pos_sales_dh` 거래처/제품별 POS 스캔 실적.
 *
 * 레거시 「POS매출 조회」(`/sales/posMain` → `posmain.jsp`) 의 거래처별 확장. 거래처별 합계 명세 +
 * 제품별 상세. 기간은 일 단위(startDate~endDate, 레거시 daterangepicker `maxSpan` 정합 — 최대 31일).
 * 전산실적(`electronicSalesDashboard.ts`) 과 동일한 API 구성이며, 유통형태/거래처유형/분류 옵션과
 * 제품 검색은 전산실적의 filter-options / product-lookup 을 재사용한다 (동일 권한 가드).
 */

export interface PosSalesDashboardListItem {
  accountId: number;
  accountName: string | null;
  sapAccountCode: string | null;
  branchCode: string | null;
  branchName: string | null;
  salesAmount: number;
  salesQuantity: number;
}

export interface PosSalesPageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PosSalesDashboardListResponse {
  startDate: string;
  endDate: string;
  /** 조회 결과 전체(페이징 무관)의 POS매출 금액 합계 — 상단 합계 표시용 */
  totalSalesAmount: number;
  /** 조회 결과 전체(페이징 무관)의 POS매출 수량 합계 */
  totalSalesQuantity: number;
  items: PosSalesDashboardListItem[];
  pageInfo: PosSalesPageInfo;
}

export interface PosSalesProduct {
  productCode: string;
  productName: string;
  barcode: string | null;
  amount: number;
  quantity: number;
}

export interface PosSalesDetail {
  customerId: number;
  customerName: string;
  sapAccountCode: string;
  /** 조회 시작일 `YYYY-MM-DD`. */
  startDate: string;
  /** 조회 종료일 `YYYY-MM-DD`. */
  endDate: string;
  /** 합계금액(원) — 서버 산출분. */
  totalAmount: number;
  /** 합계수량(EA) — 서버 산출분. */
  totalQuantity: number;
  items: PosSalesProduct[];
}

/** 목록/엑셀 공용 필터 — 제품/분류 조건은 backend 가 바코드로 해소해 POS 에 전달. */
export interface PosSalesDashboardListRequest {
  /** 시작일 (YYYY-MM-DD) */
  startDate: string;
  /** 종료일 (YYYY-MM-DD) */
  endDate: string;
  costCenterCodes: string[];
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

const BASE = '/api/v1/admin/sales/pos';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 공통 필터 파라미터 직렬화 (목록/엑셀 공용, 페이징 제외). */
function buildFilterParams(request: PosSalesDashboardListRequest): Record<string, string> {
  return {
    startDate: request.startDate,
    endDate: request.endDate,
    costCenterCodes: request.costCenterCodes.join(','),
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
 * 거래처별 POS매출 명세 — 페이징 + 정렬 + 필터 (+ 전체 합계).
 */
export async function fetchPosSalesList(
  request: PosSalesDashboardListRequest,
): Promise<PosSalesDashboardListResponse> {
  const res = await client.get<ApiResponse<PosSalesDashboardListResponse>>(`${BASE}/list`, {
    params: {
      ...buildFilterParams(request),
      ...(request.page !== undefined ? { page: request.page } : {}),
      ...(request.size !== undefined ? { size: request.size } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('POS매출 명세', res));
  return res.data.data;
}

/** 거래처별 POS매출 명세 엑셀 export 파라미터 (페이징 제외). */
export function exportPosSalesListParams(request: PosSalesDashboardListRequest): Record<string, string> {
  return buildFilterParams(request);
}

/** 거래처별 POS매출 명세 엑셀 다운로드 경로. */
export const POS_SALES_EXPORT_LIST_PATH = `${BASE}/list/export`;

/** 상세 조회의 제품/분류 필터 (목록과 동일 조건 반영 — 행 합계와 정합). */
export interface PosSalesDetailFilter {
  productIds?: number[];
  category2?: string;
  category3?: string;
}

/**
 * 단건 거래처 상세 — 제품별 POS매출 명세 (기간 + 목록과 동일한 제품/분류 필터, 바코드 포함).
 */
export async function fetchPosSalesDetail(
  customerId: number,
  startDate: string,
  endDate: string,
  filter: PosSalesDetailFilter = {},
): Promise<PosSalesDetail> {
  const res = await client.get<ApiResponse<PosSalesDetail>>(`${BASE}/detail/${customerId}`, {
    params: {
      startDate,
      endDate,
      ...(filter.productIds && filter.productIds.length > 0
        ? { productIds: filter.productIds.join(',') }
        : {}),
      ...(filter.category2 ? { category2: filter.category2 } : {}),
      ...(filter.category3 ? { category3: filter.category3 } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('POS매출 상세', res));
  return res.data.data;
}
