import { useQuery } from '@tanstack/react-query';
import { fetchUserDetail } from '@/api/user';

/**
 * web admin User 상세 Query 훅.
 *
 * id <= 0 (URL param 미파싱) 일 때는 fetch 하지 않는다.
 */
export function useUserDetail(id: number) {
  return useQuery({
    queryKey: ['admin', 'users', id],
    queryFn: () => fetchUserDetail(id),
    enabled: id > 0,
  });
}
