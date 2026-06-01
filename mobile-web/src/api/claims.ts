import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `ClaimListItemResponse` */
export interface ClaimListItem {
  claimId: number;
  accountName: string | null;
  productName: string | null;
  productCode: string | null;
  categoryValue: string | null;
  categoryLabel: string | null;
  subcategoryValue: string | null;
  subcategoryLabel: string | null;
  defectQuantity: number | null;
  status: string;
  statusLabel: string;
  createdAt: string;
}

export interface ClaimPhoto {
  photoId: number;
  url: string;
  originalFileName: string | null;
}

/** backend `ClaimDetailResponse` */
export interface ClaimDetail {
  claimId: number;
  accountName: string | null;
  productName: string | null;
  productCode: string | null;
  dateType: string | null;
  dateTypeLabel: string | null;
  date: string | null;
  categoryValue: string | null;
  categoryLabel: string | null;
  subcategoryValue: string | null;
  subcategoryLabel: string | null;
  defectDescription: string | null;
  defectQuantity: number | null;
  purchaseAmount: number | null;
  purchaseMethodName: string | null;
  requestTypeName: string | null;
  status: string;
  statusLabel: string;
  createdAt: string;
  photos: ClaimPhoto[];
}

export interface ClaimListParams {
  startDate?: string;
  endDate?: string;
}

export async function fetchClaims(params: ClaimListParams = {}): Promise<ClaimListItem[]> {
  const res = await client.get<ApiResponse<ClaimListItem[]>>('/api/v1/mobile/claims', { params });
  return unwrap(res, '클레임 목록 조회에 실패했습니다');
}

export async function fetchClaimDetail(claimId: number): Promise<ClaimDetail> {
  const res = await client.get<ApiResponse<ClaimDetail>>(`/api/v1/mobile/claims/${claimId}`);
  return unwrap(res, '클레임 상세 조회에 실패했습니다');
}
