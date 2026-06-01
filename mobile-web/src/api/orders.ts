import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `OrderRequestSummaryResponse` */
export interface OrderRequestSummary {
  id: number;
  orderRequestNumber: string;
  clientId: number;
  clientName: string;
  orderDate: string;
  deliveryDate: string;
  totalAmount: number;
  orderRequestStatus: string;
  isClosed: boolean;
}

/** backend `OrderRequestListResponse` */
export interface OrderRequestListData {
  items: OrderRequestSummary[];
  total: number;
  truncated: boolean;
  fetchedAt: string;
}

export interface OrderedItem {
  productCode: string;
  productName: string | null;
  totalQuantityBoxes: number;
  totalQuantityPieces: number;
  isCancelled: boolean;
}

/** backend `OrderRequestDetailResponse` (요약 필드) */
export interface OrderRequestDetail {
  id: number;
  orderRequestNumber: string;
  clientId: number;
  clientName: string;
  clientDeadlineTime: string | null;
  orderDate: string;
  deliveryDate: string;
  totalAmount: number;
  totalApprovedAmount: number;
  orderRequestStatus: string;
  isClosed: boolean;
  orderedItemCount: number;
  orderedItems: OrderedItem[];
}

export interface ClientOrderItem {
  productCode: string | null;
  productName: string | null;
  deliveredQuantity: string;
  deliveryStatus: string;
}

/** backend `ClientOrderDetailResponse` */
export interface ClientOrderDetail {
  sapOrderNumber: string;
  sapAccountCode: string | null;
  sapAccountName: string | null;
  clientDeadlineTime: string | null;
  orderDate: string | null;
  deliveryDate: string | null;
  totalApprovedAmount: number | null;
  orderedItemCount: number;
  orderedItems: ClientOrderItem[];
}

export interface OrderListParams {
  clientId?: number;
  status?: string;
  fromDate?: string;
  toDate?: string;
}

export async function fetchMyOrderRequests(params: OrderListParams = {}): Promise<OrderRequestListData> {
  const res = await client.get<ApiResponse<OrderRequestListData>>('/api/v1/mobile/me/order-requests', {
    params,
  });
  return unwrap(res, '주문 목록 조회에 실패했습니다');
}

export async function fetchOrderRequestDetail(orderRequestId: number): Promise<OrderRequestDetail> {
  const res = await client.get<ApiResponse<OrderRequestDetail>>(
    `/api/v1/mobile/me/order-requests/${orderRequestId}`
  );
  return unwrap(res, '주문 상세 조회에 실패했습니다');
}

export async function cancelOrderRequest(orderRequestId: number): Promise<void> {
  const res = await client.post<ApiResponse<unknown>>(
    `/api/v1/mobile/me/order-requests/${orderRequestId}/cancel`
  );
  if (!res.data.success) throw new Error(res.data.error?.message || '주문 취소에 실패했습니다');
}

export async function fetchClientOrderDetail(sapOrderNumber: string): Promise<ClientOrderDetail> {
  const res = await client.get<ApiResponse<ClientOrderDetail>>(
    `/api/v1/mobile/client-orders/${encodeURIComponent(sapOrderNumber)}`
  );
  return unwrap(res, '거래처 주문 상세 조회에 실패했습니다');
}

// --- 주문 등록 (backend OrderRequestCreateRequest) ---
export interface OrderCreateLine {
  lineNumber: number;
  productCode: string;
  quantity: number;
  unit: string;
  quantityPieces: number;
  quantityBoxes: number;
}

export interface OrderCreateRequest {
  accountId: number;
  deliveryDate: string; // YYYY-MM-DD
  totalAmount: number;
  lines: OrderCreateLine[];
}

export interface OrderCreateResult {
  orderRequestId: number;
  orderRequestNumber: string;
  status: string;
  totalAmount: number;
}

export async function createOrderRequest(request: OrderCreateRequest): Promise<OrderCreateResult> {
  const res = await client.post<ApiResponse<OrderCreateResult>>(
    '/api/v1/mobile/order-requests',
    request
  );
  return unwrap(res, '주문 등록에 실패했습니다');
}
