import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getHerokuFkResolveProgress,
  getHerokuSfidFkResolvableTables,
  getHerokuSfidFkResolveProgress,
  runHerokuPasswordHash,
  startHerokuFkResolve,
  startHerokuSfidFkResolve,
  type HerokuFkResolveProgress,
  type HerokuPasswordHashResponse,
} from '@/api/admin/herokuMigration';
import type { FkResolveProgress } from '@/api/admin/sfMigration';

const KEY_BASE = ['admin', 'heroku-migration-stage2'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'fk-progress'] as const;
const SFID_PROGRESS_KEY = [...KEY_BASE, 'sfid-fk-progress'] as const;
const SFID_TABLES_KEY = [...KEY_BASE, 'sfid-fk-tables'] as const;

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

/**
 * Heroku sfid FK Resolve 진행 상태 Query 훅 (SF progress 공유 — chunk 단위 진행 포함).
 *
 * RUNNING 이면 1초 polling, 그 외엔 5초.
 */
export function useHerokuSfidFkResolveProgress(options?: { enabled?: boolean }) {
  return useQuery<FkResolveProgress>({
    queryKey: SFID_PROGRESS_KEY,
    queryFn: getHerokuSfidFkResolveProgress,
    refetchInterval: (query) =>
      query.state.data?.status === 'RUNNING' ? 1000 : 5000,
    enabled: options?.enabled ?? true,
  });
}

/**
 * Heroku sfid FK Resolve 대상 테이블 목록 Query 훅 (단일 실행 드롭다운용).
 */
export function useHerokuSfidFkResolvableTables() {
  return useQuery<string[]>({
    queryKey: SFID_TABLES_KEY,
    queryFn: getHerokuSfidFkResolvableTables,
  });
}

/**
 * Heroku sfid FK Resolve 실행 Mutation 훅. `tableName` 미지정 시 전체, 지정 시 1개만 실행.
 */
export function useStartHerokuSfidFkResolve() {
  const queryClient = useQueryClient();
  return useMutation<FkResolveProgress, Error, string | undefined>({
    mutationFn: (tableName) => startHerokuSfidFkResolve(tableName),
    onSuccess: (data) => {
      queryClient.setQueryData(SFID_PROGRESS_KEY, data);
    },
  });
}

/**
 * EmployeeInfo(mobile) 초기 비밀번호 BCrypt 적재 Mutation 훅.
 *
 * SF Stage 2-C (User.password) 와 동일 초기 평문 상수를 공유한다. 1회성 cut-over 도구라 무효화할
 * 캐시 화면이 없어 결과를 mutation data 로 직접 표시한다. 동기 실행 — 사원 수에 비례한 시간 소요.
 */
export function useRunHerokuPasswordHash() {
  return useMutation<HerokuPasswordHashResponse>({
    mutationFn: runHerokuPasswordHash,
  });
}
