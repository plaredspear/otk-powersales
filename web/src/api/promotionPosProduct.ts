import client from './client';
import type { ApiResponse } from './types';

// SF Promotion 상세의 "상세 POS품목" Related List (DKRetail__PromotionProduct__c).
export interface PromotionPosProduct {
  id: number;
  name: string | null;
  productId: number | null;
  productName: string | null;
  productCode: string | null;
  price: number | null;
}

export interface PromotionPosProductRequest {
  productId?: number | null;
  price?: number | null;
}

export async function fetchPromotionPosProducts(
  promotionId: number,
): Promise<PromotionPosProduct[]> {
  const res = await client.get<ApiResponse<PromotionPosProduct[]>>(
    `/api/v1/admin/promotions/${promotionId}/pos-products`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '상세 POS품목 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createPromotionPosProduct(
  promotionId: number,
  data: PromotionPosProductRequest,
): Promise<PromotionPosProduct> {
  const res = await client.post<ApiResponse<PromotionPosProduct>>(
    `/api/v1/admin/promotions/${promotionId}/pos-products`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '상세 POS품목 생성에 실패했습니다');
  }
  return res.data.data;
}

export async function updatePromotionPosProduct(
  posProductId: number,
  data: PromotionPosProductRequest,
): Promise<PromotionPosProduct> {
  const res = await client.put<ApiResponse<PromotionPosProduct>>(
    `/api/v1/admin/promotion-products/${posProductId}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '상세 POS품목 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deletePromotionPosProduct(posProductId: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(
    `/api/v1/admin/promotion-products/${posProductId}`,
  );
  if (!res.data.success) {
    throw new Error(res.data.message || '상세 POS품목 삭제에 실패했습니다');
  }
}
