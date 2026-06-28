import client from './client';
import type { ApiResponse } from './types';

/**
 * POS매출 조회 — 거래처 1곳 + 기간(시작/종료일 YYYY-MM-DD) 제품별 실적.
 *
 * Backend `GET /api/v1/admin/sales/pos` (`monthly_sales_history` READ 권한 필요).
 * 레거시 `promotion/month/posmain.jsp` (POS DB `live_pos_sales_dh`) 대응.
 */
export interface PosSalesProduct {
  productCode: string;
  productName: string;
  barcode: string | null;
  amount: number;
  quantity: number;
}

export interface PosSalesResponse {
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

/** POS매출 제품별 명세 엑셀 다운로드 경로 (GET, 조회와 동일 customerId/startDate/endDate 파라미터). */
export const POS_SALES_EXPORT_PATH = '/api/v1/admin/sales/pos/export';

/** POS매출 조회 화면 지점 셀렉터 옵션 (권한별 지점 화이트리스트). */
export interface PosSalesBranch {
  branchCode: string;
  branchName: string;
}

/**
 * POS매출 조회 화면의 지점 셀렉터 옵션 조회.
 *
 * Backend `GET /api/v1/admin/sales/pos/branches` (`monthly_sales_history` READ 권한 필요).
 * 전문행사조/여사원 일정과 동일하게 WomenScheduleBranchResolver 권한별 화이트리스트를 산출한다.
 */
export async function getPosSalesBranches(): Promise<PosSalesBranch[]> {
  const res = await client.get<ApiResponse<PosSalesBranch[]>>('/api/v1/admin/sales/pos/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 거래처 1곳 + 기간(시작/종료일 YYYY-MM-DD) + 선택 바코드 목록의 제품별 POS매출 조회.
 *
 * `barcodes` 가 비면 거래처 전체 제품 집계, 1건 이상이면 해당 바코드 제품만 집계 (mobile 정합).
 * 쉼표 구분 문자열로 전송한다 (`PosSalesRangeRequest.barcodes`).
 */
export async function fetchPosSales(
  customerId: number,
  startDate: string,
  endDate: string,
  barcodes?: string[],
): Promise<PosSalesResponse> {
  const res = await client.get<ApiResponse<PosSalesResponse>>('/api/v1/admin/sales/pos', {
    params: {
      customerId,
      startDate,
      endDate,
      ...(barcodes && barcodes.length > 0 ? { barcodes: barcodes.join(',') } : {}),
    },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || 'POS매출 조회에 실패했습니다');
  }
  return res.data.data;
}

/** POS매출 매출 조회 제품 검색 결과 항목 (제품명/제품코드/바코드 — Backend ProductDto). */
export interface PosSalesProductSearchItem {
  productCode: string | null;
  productName: string | null;
  barcode: string | null;
}

/**
 * POS매출 매출 조회 제품 검색 — 제품명/제품코드/바코드 통합 검색.
 *
 * Backend `GET /api/v1/admin/sales/pos/products` (`monthly_sales_history` READ 권한 필요).
 * 모바일 POS매출 제품 검색과 동일하게 바코드 포함 제품 목록을 반환한다.
 *
 * @param type 'text'(기본, 제품명/제품코드/바코드 통합) 또는 'barcode'(바코드 정확 조회).
 */
export async function searchPosSalesProducts(
  query: string,
  type: 'text' | 'barcode' = 'text',
  size = 30,
): Promise<PosSalesProductSearchItem[]> {
  const res = await client.get<ApiResponse<{ content: PosSalesProductSearchItem[] }>>(
    '/api/v1/admin/sales/pos/products',
    { params: { query, type, page: 0, size } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '제품 검색에 실패했습니다');
  }
  return res.data.data.content ?? [];
}
