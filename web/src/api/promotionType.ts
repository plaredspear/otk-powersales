import client from './client';

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface PromotionTypeRaw {
  id: number;
  name: string;
  display_order: number;
  is_active: boolean;
}

export interface PromotionType {
  id: number;
  name: string;
  displayOrder: number;
  isActive: boolean;
}

export interface PromotionTypeRequest {
  name: string;
  display_order: number;
}

function mapPromotionType(raw: PromotionTypeRaw): PromotionType {
  return {
    id: raw.id,
    name: raw.name,
    displayOrder: raw.display_order,
    isActive: raw.is_active,
  };
}

export async function fetchPromotionTypes(): Promise<PromotionType[]> {
  const res = await client.get<ApiResponse<PromotionTypeRaw[]>>('/api/v1/admin/promotion-types');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사유형 목록 조회에 실패했습니다');
  }
  return res.data.data.map(mapPromotionType);
}

export async function createPromotionType(data: PromotionTypeRequest): Promise<PromotionType> {
  const res = await client.post<ApiResponse<PromotionTypeRaw>>(
    '/api/v1/admin/promotion-types',
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사유형 등록에 실패했습니다');
  }
  return mapPromotionType(res.data.data);
}

export async function updatePromotionType(
  id: number,
  data: PromotionTypeRequest,
): Promise<PromotionType> {
  const res = await client.put<ApiResponse<PromotionTypeRaw>>(
    `/api/v1/admin/promotion-types/${id}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사유형 수정에 실패했습니다');
  }
  return mapPromotionType(res.data.data);
}

export async function deletePromotionType(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/promotion-types/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '행사유형 비활성화에 실패했습니다');
  }
}
