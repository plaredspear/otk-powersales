import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchPromotions, type PromotionListParams } from '@/api/promotion';

export function usePromotions(params: PromotionListParams) {
  return useQuery({
    // params 객체 전체를 key 에 포함 — 개별 나열 시 accountName 등 일부 필터 누락으로 재조회가 안 되는 문제 방지.
    queryKey: ['admin', 'promotions', params],
    queryFn: () => fetchPromotions(params),
    // 페이지/조건 변경 시 직전 데이터를 유지해 테이블 깜빡임(빈 상태 노출)을 방지.
    placeholderData: keepPreviousData,
  });
}
