import client from './client';
import type { ApiResponse } from './types';


export interface PromotionListParams {
  keyword?: string;
  promotionType?: string;
  category?: string;
  startDate?: string;
  endDate?: string;
  page: number;
  size: number;
}

export interface PromotionListItem {
  id: number;
  promotionNumber: string;
  promotionName: string | null;
  promotionType: string | null;
  accountName: string | null;
  startDate: string;
  endDate: string;
  targetAmount: number | null;
  actualAmount: number | null;
  category: string | null;
  productType: string | null;
  branchName: string | null;
  isClosed: boolean;
  costCenterCode: string | null;
  remark: string | null;
  isDeleted: boolean;
  createdAt: string;
}

export interface PromotionListData {
  content: PromotionListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PromotionDetail {
  id: number;
  promotionNumber: string;
  promotionName: string | null;
  promotionType: string | null;
  accountId: number;
  accountName: string | null;
  startDate: string;
  endDate: string;
  primaryProductId: number | null;
  primaryProductName: string | null;
  otherProduct: string | null;
  message: string | null;
  standLocation: string | null;
  targetAmount: number | null;
  actualAmount: number | null;
  costCenterCode: string | null;
  category: string | null;
  productType: string | null;
  category1: string | null;
  branchName: string | null;
  isClosed: boolean;
  remark: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionFormData {
  promotionType: string;
  accountId: number;
  startDate: string;
  endDate: string;
  primaryProductId: number;
  otherProduct?: string | null;
  message?: string | null;
  standLocation: string;
  remark?: string | null;
}

// --- Form-Meta interfaces ---


export interface PromotionFormMeta {
  promotionTypes: { value: string; name: string }[];
  standLocations: { value: string; name: string }[];
}


// --- API functions ---

export async function fetchPromotionFormMeta(): Promise<PromotionFormMeta> {
  const res = await client.get<ApiResponse<PromotionFormMeta>>('/api/v1/admin/promotions/form-meta');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 폼 메타 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPromotions(params: PromotionListParams): Promise<PromotionListData> {
  const queryParams: Record<string, string | number> = {
    page: params.page,
    size: params.size,
  };
  if (params.keyword) queryParams.keyword = params.keyword;
  if (params.promotionType) queryParams.promotionType = params.promotionType;
  if (params.category) queryParams.category = params.category;
  if (params.startDate) queryParams.startDate = params.startDate;
  if (params.endDate) queryParams.endDate = params.endDate;

  const res = await client.get<ApiResponse<PromotionListData>>('/api/v1/admin/promotions', {
    params: queryParams,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPromotion(id: number): Promise<PromotionDetail> {
  const res = await client.get<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createPromotion(data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetail>>('/api/v1/admin/promotions', data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updatePromotion(id: number, data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.put<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deletePromotion(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/promotions/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '행사마스터 삭제에 실패했습니다');
  }
}

/**
 * 행사마스터 복제 (폼 방식 — UC-11).
 *
 * 원본 sourceId 의 검증/CC 자동 채움/대표상품 1건 생성 (T1/T2/T7) 을 재사용. 신규 행사사원 11개 이력성 필드 초기화.
 */
export async function clonePromotion(sourceId: number, data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${sourceId}/clone`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 복제에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사마스터 자식 포함 복제 — 1클릭 (UC-12).
 *
 * 원본 필드 전체 + 행사사원 5필드만 복사 (담당자/근무유형). body 없음.
 */
export async function cloneWithChildren(sourceId: number): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${sourceId}/clone-with-children`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 자식 포함 복제에 실패했습니다');
  }
  return res.data.data;
}
