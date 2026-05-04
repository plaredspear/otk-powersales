import client from './client';
import type { ApiResponse } from './types';


export interface Organization {
  id: number;
  ccCd2: string | null;
  orgCd2: string | null;
  orgNm2: string | null;
  ccCd3: string | null;
  orgCd3: string | null;
  orgNm3: string | null;
  ccCd4: string | null;
  orgCd4: string | null;
  orgNm4: string | null;
  ccCd5: string | null;
  orgCd5: string | null;
  orgNm5: string | null;
  createdAt: string;
}

export interface FetchOrganizationsParams {
  keyword?: string;
  level?: string;
}

// --- API functions ---

export async function fetchOrganizations(params: FetchOrganizationsParams): Promise<Organization[]> {
  const res = await client.get<ApiResponse<Organization[]>>('/api/v1/admin/organizations', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '조직마스터 조회에 실패했습니다');
  }
  return res.data.data;
}
