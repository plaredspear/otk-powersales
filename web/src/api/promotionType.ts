import client from './client';
import type { ApiResponse } from './types';


export interface PromotionType {
  id: number;
  name: string;
  displayOrder: number;
  isActive: boolean;
}

export interface PromotionTypeRequest {
  name: string;
  displayOrder: number;
}


export async function fetchPromotionTypes(): Promise<PromotionType[]> {
  const res = await client.get<ApiResponse<PromotionType[]>>('/api/v1/admin/promotion-types');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사유형 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createPromotionType(data: PromotionTypeRequest): Promise<PromotionType> {
  const res = await client.post<ApiResponse<PromotionType>>(
    '/api/v1/admin/promotion-types',
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사유형 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updatePromotionType(
  id: number,
  data: PromotionTypeRequest,
): Promise<PromotionType> {
  const res = await client.put<ApiResponse<PromotionType>>(
    `/api/v1/admin/promotion-types/${id}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사유형 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deletePromotionType(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/promotion-types/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '행사유형 비활성화에 실패했습니다');
  }
}
