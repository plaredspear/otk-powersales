import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchOrganizations, type FetchOrganizationsParams } from '@/api/organization';

export function useOrganizations(params: FetchOrganizationsParams) {
  return useQuery({
    queryKey: ['admin', 'organizations', params.keyword, params.level],
    queryFn: () => fetchOrganizations(params),
    // 조회 조건 전환 중 직전 데이터 유지 — 테이블이 빈 상태로 깜빡이지 않게.
    placeholderData: keepPreviousData,
  });
}
