import client from './client';
import type { ApiResponse } from './types';


export interface Organization {
  id: number;
  costCenterLevel2: string | null;
  orgCodeLevel2: string | null;
  orgNameLevel2: string | null;
  costCenterLevel3: string | null;
  orgCodeLevel3: string | null;
  orgNameLevel3: string | null;
  costCenterLevel4: string | null;
  orgCodeLevel4: string | null;
  orgNameLevel4: string | null;
  costCenterLevel5: string | null;
  orgCodeLevel5: string | null;
  orgNameLevel5: string | null;
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
