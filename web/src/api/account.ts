import client from './client';
import type { ApiResponse } from './types';


export interface FetchAccountsParams {
  keyword?: string;
  abcType?: string;
  branchCode?: string;
  accountStatusName?: string;
  page?: number;
  size?: number;
}

export interface Account {
  id: number;
  externalKey: string | null;
  name: string | null;
  abcType: string | null;
  branchCode: string | null;
  branchName: string | null;
  employeeCode: string | null;
  address1: string | null;
  phone: string | null;
  accountStatusName: string | null;
}

export interface AccountListData {
  content: Account[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}


// --- API function ---

export async function fetchAccounts(params: FetchAccountsParams): Promise<AccountListData> {
  const res = await client.get<ApiResponse<AccountListData>>('/api/v1/admin/accounts', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
