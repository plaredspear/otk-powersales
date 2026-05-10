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

// --- 신규 거래처 등록 (Spec #640 P2-W) ---

export interface AdminAccountCreateRequest {
  name: string;
  employeeCode: string;
}

export interface AdminAccountCreateResponseData {
  id: number;
  name: string;
  accountGroup: string;
  employeeCode: string;
  branchCode: string | null;
  branchName: string | null;
}

/**
 * 관리자 웹 신규 거래처 등록 (Spec #640 P1-B 엔드포인트 호출).
 *
 * Backend `POST /api/v1/admin/accounts` (`ACCOUNT_WRITE` 권한 필요).
 * 에러는 axios 가 throw — 호출 측에서 `error.response.data.error.code` 분기.
 */
export async function createAdminAccount(
  payload: AdminAccountCreateRequest,
): Promise<AdminAccountCreateResponseData> {
  const res = await client.post<ApiResponse<AdminAccountCreateResponseData>>(
    '/api/v1/admin/accounts',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 등록에 실패했습니다');
  }
  return res.data.data;
}
