import { useQuery } from '@tanstack/react-query';
import { fetchPermissionSetChangeLog } from '@/api/admin/permission';

const KEY_BASE = ['admin', 'permissions'] as const;

/**
 * Spec #837 — 특정 PS 의 변경 이력 페이지네이션 조회.
 *
 * 시간순 desc 정렬은 backend 가 책임. PS 삭제 후에는 본 query 로 접근 불가 (FK SET NULL — DELETE
 * 이벤트의 permissionSetId 가 null 로 set 됨).
 */
export function usePermissionSetChangeLog(permissionSetId: number | undefined, page = 0, size = 20) {
  return useQuery({
    queryKey: [...KEY_BASE, 'permission-sets', permissionSetId, 'change-log', page, size],
    queryFn: () => fetchPermissionSetChangeLog(permissionSetId!, page, size),
    enabled: !!permissionSetId,
    placeholderData: (previous) => previous,
  });
}
