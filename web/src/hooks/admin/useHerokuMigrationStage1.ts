import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getHerokuStage1CopyProgress,
  getHerokuStage1Defaults,
  listHerokuStage1Targets,
  startHerokuStage1Copy,
  startHerokuStage1CopyAll,
  HerokuStage1AlreadyRunningError,
  type HerokuStage1CopyAllRequest,
  type HerokuStage1CopyProgress,
  type HerokuStage1CopyRequest,
  type HerokuStage1Defaults,
  type HerokuStage1Target,
} from '@/api/admin/herokuMigrationStage1';

const KEY_BASE = ['admin', 'heroku-migration-stage1'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'progress'] as const;
const TARGETS_KEY = [...KEY_BASE, 'targets'] as const;
const DEFAULTS_KEY = [...KEY_BASE, 'defaults'] as const;

/**
 * Heroku Stage 1 적재 진행 상태 Query 훅.
 *
 * RUNNING 이면 1초 polling (processedRows 실시간 갱신), 그 외엔 5초 — backend 의 begin()
 * 직후 RUNNING 전환을 가벼운 polling 으로 자동 감지.
 */
export function useHerokuStage1CopyProgress(options?: { enabled?: boolean }) {
  return useQuery<HerokuStage1CopyProgress>({
    queryKey: PROGRESS_KEY,
    queryFn: getHerokuStage1CopyProgress,
    refetchInterval: (query) =>
      query.state.data?.status === 'RUNNING' ? 1000 : 5000,
    refetchIntervalInBackground: false,
    enabled: options?.enabled ?? true,
  });
}

/**
 * Heroku Stage 1 단일 target 적재 Mutation 훅.
 *
 * 성공/409(이미 실행 중) 응답의 progress 를 캐시에 직접 반영해 즉시 진행 상태 표시.
 */
export function useStartHerokuStage1Copy() {
  const queryClient = useQueryClient();
  return useMutation<HerokuStage1CopyProgress, Error, HerokuStage1CopyRequest>({
    mutationFn: startHerokuStage1Copy,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
    },
    onError: (err) => {
      if (err instanceof HerokuStage1AlreadyRunningError) {
        queryClient.setQueryData(PROGRESS_KEY, err.progress);
      }
    },
  });
}

/**
 * Heroku Stage 1 전체 entity 일괄 적재 Mutation 훅.
 *
 * 성공/409 응답의 progress 를 캐시에 직접 반영.
 */
export function useStartHerokuStage1CopyAll() {
  const queryClient = useQueryClient();
  return useMutation<HerokuStage1CopyProgress, Error, HerokuStage1CopyAllRequest>({
    mutationFn: startHerokuStage1CopyAll,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
    },
    onError: (err) => {
      if (err instanceof HerokuStage1AlreadyRunningError) {
        queryClient.setQueryData(PROGRESS_KEY, err.progress);
      }
    },
  });
}

/**
 * Heroku Stage 1 적재 대상 target 목록 Query 훅.
 */
export function useHerokuStage1Targets() {
  return useQuery<HerokuStage1Target[]>({
    queryKey: TARGETS_KEY,
    queryFn: listHerokuStage1Targets,
  });
}

/**
 * Heroku Stage 1 기본값 (S3 bucket / prefix) Query 훅.
 *
 * 환경 설정값 — 세션 중 불변이라 staleTime Infinity.
 */
export function useHerokuStage1Defaults() {
  return useQuery<HerokuStage1Defaults>({
    queryKey: DEFAULTS_KEY,
    queryFn: getHerokuStage1Defaults,
    staleTime: Infinity,
  });
}
