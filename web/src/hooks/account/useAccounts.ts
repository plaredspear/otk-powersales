import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchAccounts, type FetchAccountsParams } from '@/api/account';

export function useAccounts(params: FetchAccountsParams) {
  return useQuery({
    queryKey: ['admin', 'accounts', params.keyword, params.abcType, params.branchCode, params.accountStatusName, params.page, params.size],
    queryFn: () => fetchAccounts(params),
    // 재조회(페이지 이동/필터 변경) 중 이전 결과를 유지해 빈 화면 깜빡임 방지.
    placeholderData: keepPreviousData,
  });
}
