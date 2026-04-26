import { useQuery } from '@tanstack/react-query';
import { fetchAccounts, type FetchAccountsParams } from '@/api/account';

export function useAccounts(params: FetchAccountsParams) {
  return useQuery({
    queryKey: ['admin', 'accounts', params.keyword, params.abcType, params.branchCode, params.accountStatusName, params.page, params.size],
    queryFn: () => fetchAccounts(params),
  });
}
