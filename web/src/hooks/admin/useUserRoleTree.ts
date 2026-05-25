import { useQuery } from '@tanstack/react-query';
import { getUserRoleTree, type UserRoleNode } from '@/api/admin/userRole';

const TREE_KEY = ['admin', 'user-roles', 'tree'] as const;

export function useUserRoleTree() {
  return useQuery<UserRoleNode[]>({
    queryKey: TREE_KEY,
    queryFn: getUserRoleTree,
    staleTime: 5 * 60 * 1000,
  });
}
