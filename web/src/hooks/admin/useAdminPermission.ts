import { useQuery } from '@tanstack/react-query';
import {
  fetchPermissionMatrix,
  fetchPermissionSet,
  fetchPermissionSets,
  fetchProfile,
  fetchProfiles,
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
