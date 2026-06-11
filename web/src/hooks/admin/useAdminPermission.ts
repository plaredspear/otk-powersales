import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createAssignment,
  createAssignmentBatch,
  fetchPermissionMatrix,
  fetchPermissionSet,
  fetchPermissionSetMatrix,
  fetchPermissionSets,
  fetchProfile,
  fetchProfiles,
  revokeAssignment,
  type FetchProfileParams,
} from '@/api/admin/permission';

const KEY_BASE = ['admin', 'permissions'] as const;

export function useProfiles() {
  return useQuery({
    queryKey: [...KEY_BASE, 'profiles'],
    queryFn: fetchProfiles,
  });
}

export function useProfile(profileId: number | undefined, params: FetchProfileParams = {}) {
  return useQuery({
    queryKey: [...KEY_BASE, 'profiles', profileId, params],
    queryFn: () => fetchProfile(profileId!, params),
    enabled: !!profileId,
    placeholderData: (previous) => previous,
  });
}

export function usePermissionSets() {
  return useQuery({
    queryKey: [...KEY_BASE, 'permission-sets'],
    queryFn: fetchPermissionSets,
  });
}

export function usePermissionSet(permissionSetId: number | undefined, params: FetchProfileParams = {}) {
  return useQuery({
    queryKey: [...KEY_BASE, 'permission-sets', permissionSetId, params],
    queryFn: () => fetchPermissionSet(permissionSetId!, params),
    enabled: !!permissionSetId,
    placeholderData: (previous) => previous,
  });
}

export function usePermissionMatrix() {
  return useQuery({
    queryKey: [...KEY_BASE, 'matrix'],
    queryFn: fetchPermissionMatrix,
    staleTime: 5 * 60 * 1000,
  });
}

export function usePermissionSetMatrix() {
  return useQuery({
    queryKey: [...KEY_BASE, 'permission-set-matrix'],
    queryFn: fetchPermissionSetMatrix,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Spec #804 — 부여/회수 mutation. 성공 시 관련 query invalidate.
 */
function useInvalidatePermissions() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: KEY_BASE });
  };
}

export function useCreateAssignment() {
  const invalidate = useInvalidatePermissions();
  return useMutation({
    mutationFn: createAssignment,
    onSuccess: () => invalidate(),
  });
}

export function useRevokeAssignment() {
  const invalidate = useInvalidatePermissions();
  return useMutation({
    mutationFn: revokeAssignment,
    onSuccess: () => invalidate(),
  });
}

export function useCreateAssignmentBatch() {
  const invalidate = useInvalidatePermissions();
  return useMutation({
    mutationFn: createAssignmentBatch,
    onSuccess: () => invalidate(),
  });
}
