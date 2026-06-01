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
