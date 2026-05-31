import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createPermissionSet,
  deletePermissionSet,
  fetchAvailablePermissionResources,
  updatePermissionSetFlags,
  updatePermissionSetMeta,
  updateProfileFlags,
  type PermissionSetCreateRequest,
  type PermissionSetUpdateFlagsRequest,
  type PermissionSetUpdateMetaRequest,
  type ProfileUpdateFlagsRequest,
} from '@/api/admin/permission';

const KEY_BASE = ['admin', 'permissions'] as const;

/**
 * Spec #837 — PS 변경 후 관련 query invalidate. PermissionSetDetailPage / ListPage / Matrix / 권한
 * 메뉴 표시 모두 영향. user 의 effective 권한 변경 가능성도 있어 employee 도 함께 invalidate.
 */
function useInvalidatePermissionSets() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: KEY_BASE });
    queryClient.invalidateQueries({ queryKey: ['employee'] });
  };
}

/** Spec #837 — `available-resources` 카탈로그 조회. 매트릭스 편집 UI 가 사용. 5분 TTL. */
export function useAvailablePermissionResources(enabled = true) {
  return useQuery({
    queryKey: [...KEY_BASE, 'available-resources'],
    queryFn: fetchAvailablePermissionResources,
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

/** Spec #837 — 신규 PS 등록 mutation. 성공 시 query invalidate. */
export function useCreatePermissionSet() {
  const invalidate = useInvalidatePermissionSets();
  return useMutation({
    mutationFn: (req: PermissionSetCreateRequest) => createPermissionSet(req),
    onSuccess: () => invalidate(),
  });
}

/** Spec #837 — PS 메타 수정 mutation. SF 출처 PS 면 backend 가 dirty 플래그 set. */
export function useUpdatePermissionSetMeta(permissionSetId: number) {
  const invalidate = useInvalidatePermissionSets();
  return useMutation({
    mutationFn: (req: PermissionSetUpdateMetaRequest) => updatePermissionSetMeta(permissionSetId, req),
    onSuccess: () => invalidate(),
  });
}

/** Spec #837 — PS 권한 비트 전체 교체 mutation. SF 출처 PS 면 dirty 플래그 set. */
export function useUpdatePermissionSetFlags(permissionSetId: number) {
  const invalidate = useInvalidatePermissionSets();
  return useMutation({
    mutationFn: (req: PermissionSetUpdateFlagsRequest) => updatePermissionSetFlags(permissionSetId, req),
    onSuccess: () => invalidate(),
  });
}

/** Spec #837 — PS 삭제 mutation. SF 출처 PS 는 backend 가 409 던짐 — UI 가 사전 비활성화. */
export function useDeletePermissionSet() {
  const invalidate = useInvalidatePermissionSets();
  return useMutation({
    mutationFn: (permissionSetId: number) => deletePermissionSet(permissionSetId),
    onSuccess: () => invalidate(),
  });
}

/** Profile 권한 비트 전체 교체 mutation. 편집 시 backend 가 dirty 플래그 set + 권한 캐시 무효화. */
export function useUpdateProfileFlags(profileId: number) {
  const invalidate = useInvalidatePermissionSets();
  return useMutation({
    mutationFn: (req: ProfileUpdateFlagsRequest) => updateProfileFlags(profileId, req),
    onSuccess: () => invalidate(),
  });
}
