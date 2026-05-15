import { useQuery } from '@tanstack/react-query';
import { fetchUsers, type UserListParams } from '@/api/user';

/**
 * web admin User 목록 Query 훅.
 *
 * queryKey: `['admin', 'users', keyword, isActive, page, size]`.
 * 필터 조합별로 캐시 분리되며, 변경 mutation 후 prefix `['admin', 'users']` invalidate 로 일괄 갱신된다.
 */
export function useUsers(params: UserListParams) {
  return useQuery({
    queryKey: ['admin', 'users', params.keyword, params.isActive, params.page, params.size],
    queryFn: () => fetchUsers(params),
  });
}
