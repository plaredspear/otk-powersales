import { useQuery } from '@tanstack/react-query';
import { fetchErpOrderDetail } from '@/api/erpOrder';

/**
 * ERP주문 상세 조회 hook (`GET /api/v1/admin/erp-orders/{id}`).
 *
 * `id` 가 유효한 양수일 때만 조회 (라우트 param 파싱 실패 시 비활성).
 */
export function useErpOrderDetail(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'erp-orders', 'detail', id],
    queryFn: () => fetchErpOrderDetail(id as number),
    enabled: typeof id === 'number' && Number.isFinite(id) && id > 0,
  });
}
