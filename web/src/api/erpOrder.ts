import client from './client';
import type { ApiResponse } from './types';

/**
 * 기준정보 > ERP주문 조회 API 클라이언트.
 *
 * SAP 인바운드가 적재한 `erp_order` / `erp_order_product` 를 web admin 에 조회 노출하는 read-only API.
 * 백엔드 `AdminErpOrderController` (`/api/v1/admin/erp-orders`, `erp_order` READ 권한) 와 1:1.
 */

export interface FetchErpOrdersParams {
  /** 주문번호 정확일치 (부분 일치 아님 — 대용량 테이블 LIKE 스캔 회피) */
  sapOrderNumber?: string;
  deliveryDateFrom?: string;
  deliveryDateTo?: string;
  page?: number;
  size?: number;
}

export interface ErpOrder {
  id: number;
  sapOrderNumber: string;
  sapAccountCode: string | null;
  sapAccountName: string | null;
  deliveryRequestDate: string | null;
  orderDate: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  /** 백엔드 BigDecimal — Jackson 이 JSON 숫자로 직렬화하므로 number 로 도착 (문자열 방어 포함) */
  orderSalesAmount: number | string | null;
  orderChannelNm: string | null;
  orderTypeNm: string | null;
}

export interface ErpOrderListData {
  content: ErpOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErpOrderProduct {
  id: number;
  lineNumber: string | null;
  productCode: string | null;
  productName: string | null;
  /** 백엔드 BigDecimal — JSON 숫자로 도착 */
  orderQuantity: number | string | null;
  unit: string | null;
  confirmQuantity: number | string | null;
  confirmUnit: string | null;
  shippingQuantity: number | string | null;
  orderSalesLineAmount: number | string | null;
  lineItemStatus: string | null;
  deliveryStatus: string | null;
  plantNm: string | null;
}

export interface ErpOrderDetail {
  id: number;
  sapOrderNumber: string;
  refSapOrderNumber: string | null;
  sapAccountCode: string | null;
  sapAccountName: string | null;
  accountId: number | null;
  accountName: string | null;
  deliveryRequestDate: string | null;
  orderDate: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  /** 백엔드 BigDecimal — JSON 숫자로 도착 */
  orderSalesAmount: number | string | null;
  orderChannel: string | null;
  orderChannelNm: string | null;
  orderType: string | null;
  orderTypeNm: string | null;
  isDeleted: boolean | null;
  products: ErpOrderProduct[];
}

/**
 * ERP주문 목록 조회 (`GET /api/v1/admin/erp-orders`, `erp_order` READ 권한 필요).
 */
export async function fetchErpOrders(params: FetchErpOrdersParams): Promise<ErpOrderListData> {
  const res = await client.get<ApiResponse<ErpOrderListData>>('/api/v1/admin/erp-orders', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ERP주문 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * ERP주문 상세 조회 (`GET /api/v1/admin/erp-orders/{id}`, `erp_order` READ 권한 필요).
 *
 * 미존재 시 404 (`ERP_ORDER_NOT_FOUND`).
 */
export async function fetchErpOrderDetail(id: number): Promise<ErpOrderDetail> {
  const res = await client.get<ApiResponse<ErpOrderDetail>>(`/api/v1/admin/erp-orders/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ERP주문 상세 조회에 실패했습니다');
  }
  return res.data.data;
}
