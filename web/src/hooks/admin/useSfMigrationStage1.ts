import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getStage1CopyProgress,
  listStage1Targets,
  startStage1Copy,
  type Stage1CopyProgress,
  type Stage1CopyRequest,
} from '@/api/admin/sfMigrationStage1';

const KEY_BASE = ['admin', 'sf-migration-stage1'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'progress'] as const;
const TARGETS_KEY = [...KEY_BASE, 'targets'] as const;

export function useStage1CopyProgress(options?: { enabled?: boolean }) {
  return useQuery<Stage1CopyProgress>({
    queryKey: PROGRESS_KEY,
    queryFn: getStage1CopyProgress,
    refetchInterval: (query) => {
      const data = query.state.data;
      return data && data.status === 'RUNNING' ? 1000 : false;
    },
    enabled: options?.enabled ?? true,
  });
}

export function useStartStage1Copy() {
  const queryClient = useQueryClient();
  return useMutation<Stage1CopyProgress, Error, Stage1CopyRequest>({
    mutationFn: startStage1Copy,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
    },
  });
}

export function useStage1Targets() {
  return useQuery<string[]>({
    queryKey: TARGETS_KEY,
    queryFn: listStage1Targets,
  });
}
