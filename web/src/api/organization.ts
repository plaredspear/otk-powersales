import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

export interface Organization {
  id: number;
  cc_cd2: string | null;
  org_cd2: string | null;
  org_nm2: string | null;
  cc_cd3: string | null;
  org_cd3: string | null;
  org_nm3: string | null;
  cc_cd4: string | null;
  org_cd4: string | null;
  org_nm4: string | null;
  cc_cd5: string | null;
  org_cd5: string | null;
  org_nm5: string | null;
  created_at: string;
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
