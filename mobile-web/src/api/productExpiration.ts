import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `ProductExpirationItemResponse` */
export interface ProductExpirationItem {
  seq: number;
  productCode: string;
  productName: string;
  accountCode: string;
  accountName: string;
  expirationDate: string;
  alarmDate: string;
  dDay: number;
  description: string | null;
  isExpired: boolean;
}

export interface ProductExpirationListParams {
  fromDate: string; // YYYY-MM-DD
  toDate: string; // YYYY-MM-DD
  accountCode?: string;
}

export async function fetchProductExpirationList(
  params: ProductExpirationListParams
): Promise<ProductExpirationItem[]> {
  const res = await client.get<ApiResponse<ProductExpirationItem[]>>(
    '/api/v1/mobile/product-expiration',
    { params }
  );
  return unwrap(res, '유통기한 목록 조회에 실패했습니다');
}

/** backend `ProductExpirationCreateRequest` */
export interface ProductExpirationCreateRequest {
  accountCode: string;
  accountName: string;
  productCode: string;
  productName: string;
  expirationDate: string; // YYYY-MM-DD
  alarmDate: string; // YYYY-MM-DD
  description?: string;
}

export interface ProductExpirationUpdateRequest {
  expirationDate: string;
  alarmDate: string;
  description?: string;
}

export async function createProductExpiration(
  request: ProductExpirationCreateRequest
): Promise<ProductExpirationItem> {
  const res = await client.post<ApiResponse<ProductExpirationItem>>(
    '/api/v1/mobile/product-expiration',
    request
  );
  return unwrap(res, '유통기한 등록에 실패했습니다');
}

export async function updateProductExpiration(
  seq: number,
  request: ProductExpirationUpdateRequest
): Promise<ProductExpirationItem> {
  const res = await client.put<ApiResponse<ProductExpirationItem>>(
    `/api/v1/mobile/product-expiration/${seq}`,
    request
  );
  return unwrap(res, '유통기한 수정에 실패했습니다');
}

export async function deleteProductExpiration(seq: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(
    `/api/v1/mobile/product-expiration/${seq}`
  );
  if (!res.data.success) throw new Error(res.data.error?.message || '삭제에 실패했습니다');
}
