import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchAccounts, type FetchAccountsParams } from '@/api/account';

export function useAccounts(params: FetchAccountsParams) {
  return useQuery({
    // 필터가 늘어나는 목록 훅이므로 params 객체 전체를 key 로 사용 — 파라미터 추가 시 key 누락(stale)을 원천 차단.
    queryKey: ['admin', 'accounts', params],
    queryFn: () => fetchAccounts(params),
    // 재조회(페이지 이동/필터 변경) 중 이전 결과를 유지해 빈 화면 깜빡임 방지.
    placeholderData: keepPreviousData,
  });
}
