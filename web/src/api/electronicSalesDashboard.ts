import { AxiosError } from 'axios';
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
  /** 유통형태 — 거래처유형마스터 "{코드} {이름}" 라벨 (예 "06 슈퍼") */
  distributionChannel: string | null;
  /** 거래처유형 — ABC유형코드+ABC유형 조합 라벨 */
  accountType: string | null;
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

/**
 * 유통형태 옵션 1건 — 거래처유형마스터 코드 + `"{코드} {이름}"` 라벨.
 * 조회 요청에는 [code] 를 보내고, 화면에는 [label] 을 표시한다 (목록 컬럼 표기와 동일 규칙).
 */
export interface DistributionChannelOption {
  code: string;
  label: string;
}

/** 조회 조건 옵션 — 유통형태(거래처유형마스터) / 거래처유형(ABC) / 제품 중·소분류. */
export interface ElectronicSalesFilterOptions {
  distributionChannels: DistributionChannelOption[];
  accountTypes: string[];
  categories: { category2: string; category3s: string[] }[];
  /** 유통형태 **코드** → 해당 유통형태에 실제 존재하는 거래처유형 라벨 목록 (종속 필터링용). */
  dependentAccountTypes: Record<string, string[]>;
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
  /** 유통형태 — 거래처유형마스터 코드 (예 "06" = 슈퍼) */
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
 *
 * 400 응답(기간 상한/거래처 수 상한 등 서버 검증)은 backend 안내 메시지를 그대로 Error 로
 * 승격해 화면 Alert 에 노출한다 (예: "조회 대상 거래처가 N건입니다 … 조건을 좁혀주세요").
 */
export async function fetchList(
  request: ElectronicSalesDashboardListRequest,
): Promise<ElectronicSalesDashboardListResponse> {
  try {
    const res = await client.get<ApiResponse<ElectronicSalesDashboardListResponse>>(`${BASE}/list`, {
      params: {
        ...buildFilterParams(request),
        ...(request.page !== undefined ? { page: request.page } : {}),
        ...(request.size !== undefined ? { size: request.size } : {}),
      },
    });
    if (!res.data.success || !res.data.data) throw new Error(failureMessage('전산실적 명세', res));
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 400) {
      const errorMessage = (err.response.data as ApiResponse<unknown>)?.error?.message;
      throw new Error(errorMessage || '전산실적 명세 조회에 실패했습니다');
    }
    throw err;
  }
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

/**
 * 제품 고급 검색 키워드 최소 길이 — backend `@Size(min = 1)` 보다 강한 화면 정책.
 *
 * 1자 키워드는 매칭이 과도해 페이징 전체를 훑게 되므로 화면에서 2자 이상으로 제한한다.
 * 이 값 미만이면 분류/상태 필터가 하나라도 있어야 검색이 실행된다.
 */
export const PRODUCT_ADVANCED_MIN_KEYWORD_LENGTH = 2;

/** 제품 고급 검색 조건 — 키워드 + 대/중/소분류 + 제품상태 조합. */
export interface ElectronicSalesProductAdvancedSearchParams {
  keyword?: string;
  category1?: string;
  category2?: string;
  category3?: string;
  productStatus?: string;
  page?: number;
  size?: number;
}

/** 제품 고급 검색 결과 1건 — 그리드 표시용 상세 컬럼 + 대표 바코드. */
export interface ElectronicSalesProductAdvancedItem {
  id: number;
  productCode: string | null;
  name: string | null;
  /**
   * 대표 바코드 — 드롭다운과 동일한 `제품명 (제품코드 / 바코드)` 라벨을 만들기 위해 함께 내려온다.
   * 검색 대상이 바코드 보유 제품 한정이라 사실상 항상 값이 있다.
   */
  barcode: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  standardUnitPrice: number | null;
  unit: string | null;
  storageCondition: string | null;
  productStatus: string | null;
  launchDate: string | null;
}

export interface ElectronicSalesProductAdvancedSearchResponse {
  content: ElectronicSalesProductAdvancedItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * 제품 고급 검색 — 키워드 + 대/중/소분류 + 제품상태 조합 (페이징).
 *
 * 드롭다운 빠른 검색([fetchProductLookup])이 상위 50건만 반환해 결과가 많은 키워드에서 뒤쪽
 * 제품에 도달할 수 없는 문제를 해소한다. 검색 대상은 드롭다운과 동일한 소비자 바코드 보유 제품 한정.
 */
export async function fetchProductAdvancedSearch(
  params: ElectronicSalesProductAdvancedSearchParams,
): Promise<ElectronicSalesProductAdvancedSearchResponse> {
  const res = await client.get<ApiResponse<ElectronicSalesProductAdvancedSearchResponse>>(
    `${BASE}/product-lookup/advanced`,
    {
      params: {
        ...(params.keyword ? { keyword: params.keyword } : {}),
        ...(params.category1 ? { category1: params.category1 } : {}),
        ...(params.category2 ? { category2: params.category2 } : {}),
        ...(params.category3 ? { category3: params.category3 } : {}),
        ...(params.productStatus ? { productStatus: params.productStatus } : {}),
        ...(params.page !== undefined ? { page: params.page } : {}),
        ...(params.size !== undefined ? { size: params.size } : {}),
      },
    },
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('제품 고급 검색', res));
  return res.data.data;
}

/** 제품 고급 검색 모달의 필터 드롭다운 옵션 — 대/중/소분류 트리 + 제품상태. */
export interface ElectronicSalesCategory2Node {
  category2: string;
  children: string[];
}

export interface ElectronicSalesCategoryTree {
  category1: string;
  children: ElectronicSalesCategory2Node[];
}

export interface ElectronicSalesProductLookupFilterOptions {
  categories: ElectronicSalesCategoryTree[];
  /** 제품상태 화면 표시명 ("판매중"/"단종") — 저장값이 아니며 검색 파라미터도 같은 표시명. */
  productStatuses: string[];
}

/**
 * 제품 고급 검색 모달의 필터 옵션 조회 — 모달 오픈 시에만 호출.
 *
 * 기존 [fetchFilterOptions] 의 categories 는 중/소분류 2단이라 대분류 필터를 채울 수 없어 분리한다.
 */
export async function fetchProductLookupFilterOptions(): Promise<ElectronicSalesProductLookupFilterOptions> {
  const res = await client.get<ApiResponse<ElectronicSalesProductLookupFilterOptions>>(
    `${BASE}/product-lookup/filter-options`,
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('제품 검색 조건 옵션', res));
  return res.data.data;
}
