import { AxiosError } from 'axios';
import client from './client';
import type { ApiResponse } from './types';

/**
 * POS매출 — POS `live_pos_sales_dh` 거래처/제품별 POS 스캔 실적.
 *
 * 레거시 「POS매출 조회」(`/sales/posMain` → `posmain.jsp`) 의 거래처별 확장. 외부 POS DB 부하를
 * 줄이기 위해 2단 조회로 분리한다:
 * - 1단 `/accounts`: 지점/거래처명/유통형태/거래처유형으로 메인 DB 거래처 목록만 조회 (POS 미접촉).
 * - 2단 `/list`: 1단에서 선택한 거래처(accountIds, 최대 20)만 외부 POS DB 로 집계 + 제품/분류 필터.
 *
 * 기간은 일 단위(startDate~endDate, 레거시 daterangepicker `maxSpan` 정합 — 최대 31일).
 * 유통형태/거래처유형/분류 옵션과 제품 검색은 전산실적의 filter-options / product-lookup 을
 * 재사용한다 (동일 권한 가드).
 */

/** 1단 거래처 조회 결과 1행 — POS 집계 없는 순수 거래처 메타. */
export interface PosSalesAccountItem {
  accountId: number;
  accountName: string | null;
  sapAccountCode: string | null;
  /** 유통형태 라벨 (예 "01 대형마트(3대)"). */
  distributionChannel: string | null;
  /** 거래처유형(ABC유형) 라벨 (예 "6111 이마트"). */
  accountType: string | null;
  branchCode: string | null;
  branchName: string | null;
}

export interface PosSalesAccountListResponse {
  /** 조건에 매칭된 전체 거래처 수. */
  totalElements: number;
  items: PosSalesAccountItem[];
}

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

/** 1단 거래처 조회 필터 — 모두 메인 DB Account 해소, 외부 POS DB 미접촉. */
export interface PosSalesAccountListRequest {
  costCenterCodes: string[];
  customerKeyword?: string;
  /** 유통형태 라벨 (예 "02 슈퍼") */
  distributionChannels?: string[];
  /** 거래처유형 라벨 (ABC유형, 예 "6111 이마트") */
  accountTypes?: string[];
}

/** 2단 목록/엑셀 공용 필터 — 선택 거래처(accountIds) + 제품/분류(backend 가 바코드로 해소). */
export interface PosSalesDashboardListRequest {
  /** 시작일 (YYYY-MM-DD) */
  startDate: string;
  /** 종료일 (YYYY-MM-DD) */
  endDate: string;
  /** 1단에서 선택한 거래처 id (최대 20). */
  accountIds: number[];
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

/** 2단에서 선택 가능한 최대 거래처 수 — backend PosSalesAdminQueryService.MAX_SELECTABLE_ACCOUNTS 정합. */
export const MAX_SELECTABLE_ACCOUNTS = 20;

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 1단 — 조건에 맞는 거래처 목록 조회 (외부 POS DB 미접촉, 즉시 응답).
 *
 * 지점/거래처명/유통형태/거래처유형 필터를 메인 DB 에만 적용. 400 응답(거래처 수 상한 등)은
 * backend 안내 메시지를 그대로 Error 로 승격해 화면 Alert 에 노출한다.
 */
export async function fetchPosSalesAccounts(
  request: PosSalesAccountListRequest,
): Promise<PosSalesAccountListResponse> {
  try {
    const res = await client.get<ApiResponse<PosSalesAccountListResponse>>(`${BASE}/accounts`, {
      params: {
        costCenterCodes: request.costCenterCodes.join(','),
        ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
        ...(request.distributionChannels && request.distributionChannels.length > 0
          ? { distributionChannels: request.distributionChannels.join(',') }
          : {}),
        ...(request.accountTypes && request.accountTypes.length > 0
          ? { accountTypes: request.accountTypes.join(',') }
          : {}),
      },
    });
    if (!res.data.success || !res.data.data) throw new Error(failureMessage('거래처', res));
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 400) {
      const errorMessage = (err.response.data as ApiResponse<unknown>)?.error?.message;
      throw new Error(errorMessage || '거래처 조회에 실패했습니다');
    }
    throw err;
  }
}

/** 2단 공통 필터 파라미터 직렬화 (목록/엑셀 공용, 페이징 제외). */
function buildFilterParams(request: PosSalesDashboardListRequest): Record<string, string> {
  return {
    startDate: request.startDate,
    endDate: request.endDate,
    accountIds: request.accountIds.join(','),
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
 *
 * 400 응답(기간 상한/거래처 수 상한 등 서버 검증)은 backend 안내 메시지를 그대로 Error 로
 * 승격해 화면 Alert 에 노출한다 (예: "조회 대상 거래처가 N건입니다 … 조건을 좁혀주세요").
 */
export async function fetchPosSalesList(
  request: PosSalesDashboardListRequest,
): Promise<PosSalesDashboardListResponse> {
  try {
    const res = await client.get<ApiResponse<PosSalesDashboardListResponse>>(`${BASE}/list`, {
      params: {
        ...buildFilterParams(request),
        ...(request.page !== undefined ? { page: request.page } : {}),
        ...(request.size !== undefined ? { size: request.size } : {}),
      },
    });
    if (!res.data.success || !res.data.data) throw new Error(failureMessage('POS매출 명세', res));
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 400) {
      const errorMessage = (err.response.data as ApiResponse<unknown>)?.error?.message;
      throw new Error(errorMessage || 'POS매출 명세 조회에 실패했습니다');
    }
    throw err;
  }
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
