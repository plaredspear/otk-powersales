import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getFkResolveProgress,
  runNaturalKeyFkResolve,
  runPicklistColumn,
  startFkResolve,
  type FkResolveProgress,
  type NaturalKeyFkResponse,
  type PicklistColumn,
  type PicklistResponse,
} from '@/api/admin/sfMigration';

const KEY_BASE = ['admin', 'sf-migration'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'fk-progress'] as const;

export function useFkResolveProgress(options?: { enabled?: boolean }) {
  return useQuery<FkResolveProgress>({
    queryKey: PROGRESS_KEY,
    queryFn: getFkResolveProgress,
    refetchInterval: (query) => {
      const data = query.state.data;
      return data && data.status === 'RUNNING' ? 1000 : false;
    },
    enabled: options?.enabled ?? true,
  });
}

export function useStartFkResolve() {
  const queryClient = useQueryClient();
  return useMutation<FkResolveProgress>({
    mutationFn: startFkResolve,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
    },
  });
}

export function useRunPicklistColumn() {
  return useMutation<PicklistResponse, Error, PicklistColumn>({
    mutationFn: runPicklistColumn,
  });
}

export function useRunNaturalKeyFkResolve() {
  return useMutation<NaturalKeyFkResponse>({
    mutationFn: runNaturalKeyFkResolve,
  });
}
