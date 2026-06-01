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
