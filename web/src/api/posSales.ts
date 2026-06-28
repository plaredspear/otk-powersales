import client from './client';
import type { ApiResponse } from './types';

/**
 * POS매출 조회 — 거래처 1곳 + 연월 단위 제품별 실적.
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
  yearMonth: string;
  items: PosSalesProduct[];
}

/** POS매출 제품별 명세 엑셀 다운로드 경로 (GET, 조회와 동일 customerId/yearMonth 파라미터). */
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
 * 거래처 1곳 + 연월(YYYYMM) 의 제품별 POS매출 조회.
 */
export async function fetchPosSales(
  customerId: number,
  yearMonth: string,
): Promise<PosSalesResponse> {
  const res = await client.get<ApiResponse<PosSalesResponse>>('/api/v1/admin/sales/pos', {
    params: { customerId, yearMonth },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || 'POS매출 조회에 실패했습니다');
  }
  return res.data.data;
}
