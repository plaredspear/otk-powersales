import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `MobilePromotionListItem` */
export interface PromotionListItem {
  id: number;
  promotionNumber: string;
  promotionType: string | null;
  accountName: string | null;
  startDate: string;
  endDate: string;
  standLocation: string | null;
  isClosed: boolean;
  myScheduleDate: string | null;
}

/** backend `MobilePromotionListResponse` */
export interface PromotionListData {
  content: PromotionListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PromotionEmployeeItem {
  [key: string]: unknown;
}

/** backend `MobilePromotionDetailResponse` */
export interface PromotionDetail {
  id: number;
  promotionNumber: string;
  promotionType: string | null;
  accountId: number;
  accountName: string | null;
  startDate: string;
  endDate: string;
  primaryProductName: string | null;
  otherProduct: string | null;
  message: string | null;
  standLocation: string | null;
  productType: string | null;
  isClosed: boolean;
  remark: string | null;
  employees: PromotionEmployeeItem[];
}

export interface PromotionListParams {
  startDate?: string;
  endDate?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export async function fetchPromotions(params: PromotionListParams = {}): Promise<PromotionListData> {
  const res = await client.get<ApiResponse<PromotionListData>>('/api/v1/mobile/promotions', {
    params: { page: 0, size: 20, ...params },
  });
  return unwrap(res, '행사 매출 목록 조회에 실패했습니다');
}

export async function fetchPromotionDetail(id: number): Promise<PromotionDetail> {
  const res = await client.get<ApiResponse<PromotionDetail>>(`/api/v1/mobile/promotions/${id}`);
  return unwrap(res, '행사 상세 조회에 실패했습니다');
}
