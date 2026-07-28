import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchBranchMappings, type FetchBranchMappingsParams } from '@/api/branchMapping';

export function useBranchMappings(params: FetchBranchMappingsParams) {
  return useQuery({
    queryKey: ['admin', 'branch-mappings', params.keyword],
    queryFn: () => fetchBranchMappings(params),
    // 조회 조건 전환 중 직전 데이터 유지 — 테이블이 빈 상태로 깜빡이지 않게.
    placeholderData: keepPreviousData,
  });
}
