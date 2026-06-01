import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `MyAccountInfo` */
export interface MyAccount {
  accountId: number;
  accountName: string;
  accountCode: string;
  address: string | null;
  addressDetail: string | null;
  representativeName: string | null;
  phoneNumber: string | null;
}

/** backend `MyAccountListResponse` */
export interface MyAccountListData {
  stores: MyAccount[];
  totalCount: number;
}

export async function fetchMyAccounts(keyword?: string): Promise<MyAccountListData> {
  const res = await client.get<ApiResponse<MyAccountListData>>('/api/v1/mobile/accounts/my', {
    params: keyword ? { keyword } : undefined,
  });
  return unwrap(res, '거래처 목록 조회에 실패했습니다');
}
