import client from './client';
import type { ApiResponse } from './types';


export interface ClaimListParams {
  startDate?: string;
  endDate?: string;
  status?: string;
  employeeName?: string;
  storeName?: string;
  page?: number;
  size?: number;
}

export interface ClaimListItem {
  claimId: number;
  employeeName: string;
  employeeCode: string;
  storeName: string | null;
  productName: string | null;
  productCode: string | null;
  categoryName: string | null;
  subcategoryName: string | null;
  defectQuantity: number | null;
  status: string;
  createdAt: string;
}

export interface ClaimListData {
  content: ClaimListItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ClaimDetail {
  claimId: number;
  employeeName: string;
  employeeCode: string;
  storeName: string | null;
  productCode: string | null;
  productName: string | null;
  dateType: string | null;
  date: string | null;
  categoryName: string | null;
  subcategoryName: string | null;
  defectDescription: string | null;
  defectQuantity: number | null;
  purchaseAmount: number | null;
  purchaseMethodName: string | null;
  requestTypeName: string | null;
  status: string;
  createdAt: string;
  photos: ClaimPhoto[];
}

export interface ClaimPhoto {
  photoId: number;
  photoType: string | null;
  url: string;
  originalFileName: string | null;
}


// --- API functions ---

export async function fetchClaims(params: ClaimListParams): Promise<ClaimListData> {
  const res = await client.get<ApiResponse<ClaimListData>>('/api/v1/admin/claims', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchClaimDetail(claimId: number): Promise<ClaimDetail> {
  const res = await client.get<ApiResponse<ClaimDetail>>(`/api/v1/admin/claims/${claimId}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 상세 조회에 실패했습니다');
  }
  return res.data.data;
}
