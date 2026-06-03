import { useQuery } from '@tanstack/react-query';
import { fetchAccountDetail } from '@/api/account';

/**
 * 거래처 상세 조회 hook (`GET /api/v1/admin/accounts/{id}`).
 *
 * `id` 가 유효한 양수일 때만 조회 (라우트 param 파싱 실패 시 비활성).
 */
export function useAccountDetail(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'accounts', 'detail', id],
    queryFn: () => fetchAccountDetail(id as number),
    enabled: typeof id === 'number' && Number.isFinite(id) && id > 0,
  });
}
