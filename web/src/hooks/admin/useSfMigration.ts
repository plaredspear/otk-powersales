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
