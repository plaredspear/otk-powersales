import client from './client';
import type { ApiResponse } from './types';


export interface ProductExpiration {
  id: number;
  seq: number;
  productName: string;
  productCode: string;
  accountName: string;
  accountCode: string;
  employeeName: string;
  employeeCode: string;
  expirationDate: string;
  alarmDate: string;
  dDay: number;
  status: 'EXPIRED' | 'IMMINENT' | 'NORMAL';
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductExpirationListData {
  content: ProductExpiration[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductExpirationSummary {
  totalCount: number;
  expiredCount: number;
  imminentCount: number;
  normalCount: number;
}

export interface FetchProductExpirationsParams {
  fromDate?: string;
  toDate?: string;
  employeeKeyword?: string;
  accountKeyword?: string;
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateProductExpirationRequest {
  employeeCode: string;
  accountCode: string;
  productCode: string;
  expirationDate: string;
  alarmDate: string;
  description?: string;
}

export interface UpdateProductExpirationRequest {
  expirationDate: string;
  alarmDate: string;
  description?: string;
}


// --- API functions ---

export async function fetchProductExpirations(params: FetchProductExpirationsParams): Promise<ProductExpirationListData> {
  const res = await client.get<ApiResponse<ProductExpirationListData>>('/api/v1/admin/product-expiration', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchProductExpirationSummary(): Promise<ProductExpirationSummary> {
  const res = await client.get<ApiResponse<ProductExpirationSummary>>('/api/v1/admin/product-expiration/summary');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 요약 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createProductExpiration(payload: CreateProductExpirationRequest): Promise<ProductExpiration> {
  const res = await client.post<ApiResponse<ProductExpiration>>('/api/v1/admin/product-expiration', payload);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateProductExpiration(id: number, payload: UpdateProductExpirationRequest): Promise<ProductExpiration> {
  const res = await client.put<ApiResponse<ProductExpiration>>(`/api/v1/admin/product-expiration/${id}`, payload);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteProductExpiration(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<null>>(`/api/v1/admin/product-expiration/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '유통기한 삭제에 실패했습니다');
  }
}

export async function batchDeleteProductExpirations(ids: number[]): Promise<number> {
  const res = await client.post<ApiResponse<{ deletedCount: number }>>('/api/v1/admin/product-expiration/batch-delete', { ids });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 일괄 삭제에 실패했습니다');
  }
  return res.data.data.deletedCount;
}
