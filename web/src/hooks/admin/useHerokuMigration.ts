import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getHerokuFkResolveProgress,
  startHerokuFkResolve,
  type HerokuFkResolveProgress,
} from '@/api/admin/herokuMigration';

const KEY_BASE = ['admin', 'heroku-migration-stage2'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'fk-progress'] as const;

/**
 * Heroku Stage 2 FK Resolve 진행 상태 Query 훅.
 *
 * RUNNING 이면 1초 polling, 그 외엔 5초 — 사용자가 페이지에 머무는 동안 RUNNING 전환을 감지.
 */
export function useHerokuFkResolveProgress(options?: { enabled?: boolean }) {
  return useQuery<HerokuFkResolveProgress>({
    queryKey: PROGRESS_KEY,
    queryFn: getHerokuFkResolveProgress,
    refetchInterval: (query) =>
      query.state.data?.status === 'RUNNING' ? 1000 : 5000,
    enabled: options?.enabled ?? true,
  });
}

/**
 * Heroku Stage 2 FK Resolve 실행 Mutation 훅.
 *
 * 성공 시 시작 progress 를 캐시에 직접 반영해 즉시 진행 상태 표시.
 */
export function useStartHerokuFkResolve() {
  const queryClient = useQueryClient();
  return useMutation<HerokuFkResolveProgress>({
    mutationFn: startHerokuFkResolve,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
    },
  });
}
