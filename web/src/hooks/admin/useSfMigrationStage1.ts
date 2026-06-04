import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getStage1CopyProgress,
  getStage1Defaults,
  listStage1Targets,
  startStage1Copy,
  startStage1CopyAll,
  Stage1AlreadyRunningError,
  type Stage1CopyAllRequest,
  type Stage1CopyProgress,
  type Stage1CopyRequest,
  type Stage1Defaults,
  type Stage1Target,
} from '@/api/admin/sfMigrationStage1';

const KEY_BASE = ['admin', 'sf-migration-stage1'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'progress'] as const;
const TARGETS_KEY = [...KEY_BASE, 'targets'] as const;
const DEFAULTS_KEY = [...KEY_BASE, 'defaults'] as const;

export function useStage1CopyProgress(options?: { enabled?: boolean }) {
  return useQuery<Stage1CopyProgress>({
    queryKey: PROGRESS_KEY,
    queryFn: getStage1CopyProgress,
    // RUNNING 이면 1초 — processedRows 실시간 갱신.
    // 그 외 (IDLE/COMPLETED/FAILED) 는 5초 — 사용자가 페이지에 머무는 동안 backend 가
    // begin() 한 직후 RUNNING 전환을 자동 감지하도록 가벼운 polling 유지.
    refetchInterval: (query) =>
      query.state.data?.status === 'RUNNING' ? 1000 : 5000,
    refetchIntervalInBackground: false,
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
    onError: (err) => {
      if (err instanceof Stage1AlreadyRunningError) {
        queryClient.setQueryData(PROGRESS_KEY, err.progress);
      }
    },
  });
}

export function useStartStage1CopyAll() {
  const queryClient = useQueryClient();
  return useMutation<Stage1CopyProgress, Error, Stage1CopyAllRequest>({
    mutationFn: startStage1CopyAll,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
    },
    onError: (err) => {
      if (err instanceof Stage1AlreadyRunningError) {
        queryClient.setQueryData(PROGRESS_KEY, err.progress);
      }
    },
  });
}

export function useStage1Targets() {
  return useQuery<Stage1Target[]>({
    queryKey: TARGETS_KEY,
    queryFn: listStage1Targets,
  });
}

export function useStage1Defaults() {
  return useQuery<Stage1Defaults>({
    queryKey: DEFAULTS_KEY,
    queryFn: getStage1Defaults,
    staleTime: Infinity, // 환경 설정값 — 세션 중 불변.
  });
}
