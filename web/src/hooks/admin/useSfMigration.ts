import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getFkResolvableTables,
  getFkResolveProgress,
  runNaturalKeyFkResolve,
  runNoticeRtaPlaceholder,
  runPicklistColumn,
  runUploadFilePolymorphicParent,
  runUserRoleHierarchyRecalc,
  startFkResolve,
  type FkResolveProgress,
  type NaturalKeyFkResponse,
  type NoticeRtaPlaceholderResponse,
  type PicklistColumn,
  type PicklistResponse,
  type UploadFileParentResponse,
  type UserRoleHierarchyRecalcResponse,
} from '@/api/admin/sfMigration';

const KEY_BASE = ['admin', 'sf-migration'] as const;
const PROGRESS_KEY = [...KEY_BASE, 'fk-progress'] as const;
const TABLES_KEY = [...KEY_BASE, 'fk-tables'] as const;

/**
 * 실행 시작 직후 ~ 백그라운드 스레드가 progress.begin() 으로 status 를 RUNNING 으로 올리기 전까지의
 * 짧은 윈도우 동안에도 polling 을 유지하기 위한 grace 시간 (ms). 백엔드는 비동기 실행을 예약하고
 * 즉시 (아직 IDLE/이전상태일 수 있는) progress 를 반환하므로, 이 윈도우 안에서는 status 가 아직
 * RUNNING 이 아니어도 refetch 를 이어가 첫 RUNNING 상태를 놓치지 않는다.
 */
const POLL_GRACE_MS = 5000;

// 마지막 시작 요청 시각 (모듈 스코프) — refetchInterval grace 윈도우 판정용.
let recentStartAt = 0;

export function useFkResolveProgress(options?: { enabled?: boolean }) {
  return useQuery<FkResolveProgress>({
    queryKey: PROGRESS_KEY,
    queryFn: getFkResolveProgress,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data && data.status === 'RUNNING') return 1000;
      const recentlyKicked = recentStartAt > 0 && Date.now() - recentStartAt < POLL_GRACE_MS;
      return recentlyKicked ? 1000 : false;
    },
    enabled: options?.enabled ?? true,
  });
}

export function useFkResolvableTables() {
  return useQuery<string[]>({
    queryKey: TABLES_KEY,
    queryFn: getFkResolvableTables,
  });
}

/**
 * FK Resolve 실행 mutation. `mutate(tableName?)` — 미지정 시 전체, 지정 시 해당 테이블 1개.
 */
export function useStartFkResolve() {
  const queryClient = useQueryClient();
  return useMutation<FkResolveProgress, Error, string | undefined>({
    mutationFn: startFkResolve,
    onSuccess: (data) => {
      queryClient.setQueryData(PROGRESS_KEY, data);
      // grace 윈도우를 열어 polling 을 강제로 켜고, 즉시 한 번 더 조회해 RUNNING 전이를 따라잡는다.
      recentStartAt = Date.now();
      void queryClient.invalidateQueries({ queryKey: PROGRESS_KEY });
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

export function useRunUploadFilePolymorphicParent() {
  return useMutation<UploadFileParentResponse>({
    mutationFn: runUploadFilePolymorphicParent,
  });
}

export function useRunUserRoleHierarchyRecalc() {
  return useMutation<UserRoleHierarchyRecalcResponse>({
    mutationFn: runUserRoleHierarchyRecalc,
  });
}

/**
 * 공지 본문 rtaImage <img> → placeholder 치환 Mutation 훅.
 *
 * `mutate(dryRun)` 의 boolean 인자로 dry-run(true) / apply(false) 를 분기한다. 1회성 cut-over 도구라
 * 무효화할 캐시 화면이 없어 onSuccess invalidate 없이 결과를 mutation data 로 직접 표시한다.
 */
export function useRunNoticeRtaPlaceholder() {
  return useMutation<NoticeRtaPlaceholderResponse, Error, boolean>({
    mutationFn: runNoticeRtaPlaceholder,
  });
}
