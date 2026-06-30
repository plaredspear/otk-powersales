import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getOroraDailyChunks,
  getOroraMonthlyChunks,
  getScheduledJobCatalog,
  getScheduledJobRuns,
  getScheduledJobSummary,
  triggerOroraDailyMaterialize,
  triggerOroraDailyMaterializeChunk,
  triggerOroraMonthlyMaterialize,
  triggerOroraMonthlyMaterializeChunk,
  triggerPptMaster,
  setScheduledJobRuntimeEnabled,
  type PptMasterTriggerAction,
  type RegisteredScheduledJob,
  type ScheduledJobRunsQuery,
} from '@/api/admin/scheduledJob';

const KEY_BASE = ['admin', 'scheduled-jobs'] as const;

export function useScheduledJobRuns(query: ScheduledJobRunsQuery) {
  return useQuery({
    queryKey: [...KEY_BASE, 'runs', query],
    queryFn: () => getScheduledJobRuns(query),
    placeholderData: (previous) => previous,
  });
}

export function useScheduledJobCatalog() {
  return useQuery({
    queryKey: [...KEY_BASE, 'catalog'],
    queryFn: getScheduledJobCatalog,
    staleTime: Infinity,
  });
}

/**
 * 스케줄 잡 런타임 활성/비활성 변경 mutation.
 * 성공 시 서버가 돌려준 최신 카탈로그로 캐시를 직접 갱신한다 (catalog 는 staleTime: Infinity).
 */
export function useSetScheduledJobRuntimeEnabled() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ jobName, enabled }: { jobName: string; enabled: boolean }) =>
      setScheduledJobRuntimeEnabled(jobName, enabled),
    onSuccess: (catalog: RegisteredScheduledJob[]) => {
      queryClient.setQueryData([...KEY_BASE, 'catalog'], catalog);
    },
  });
}

export function useScheduledJobSummary(windowHours: number = 24) {
  return useQuery({
    queryKey: [...KEY_BASE, 'summary', windowHours],
    queryFn: () => getScheduledJobSummary(windowHours),
    refetchInterval: 60_000,
  });
}

/**
 * ORORA 월매출 수동 적재 트리거. 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerOroraMonthlyMaterialize() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (salesMonth?: string) => triggerOroraMonthlyMaterialize(salesMonth),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'runs'] });
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'summary'] });
    },
  });
}

/**
 * ORORA 월매출 거래처 청크 메타(전체 청크 수 + 각 청크 경계) 조회. 정적 거래처 범위 산출이라
 * 거의 변하지 않으므로 staleTime 을 길게 둔다.
 */
export function useOroraMonthlyChunks() {
  return useQuery({
    queryKey: [...KEY_BASE, 'orora-monthly-chunks'],
    queryFn: getOroraMonthlyChunks,
    staleTime: Infinity,
  });
}

/**
 * ORORA 월매출 거래처 청크 단위 수동 적재 트리거. 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerOroraMonthlyMaterializeChunk() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ chunkIndex, salesMonth }: { chunkIndex: number; salesMonth?: string }) =>
      triggerOroraMonthlyMaterializeChunk(chunkIndex, salesMonth),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'runs'] });
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'summary'] });
    },
  });
}

/**
 * ORORA 일매출 수동 적재 트리거. 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerOroraDailyMaterialize() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (salesMonth?: string) => triggerOroraDailyMaterialize(salesMonth),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'runs'] });
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'summary'] });
    },
  });
}

/**
 * ORORA 일매출 거래처 청크 메타(전체 청크 수 + 각 청크 경계) 조회. 정적 거래처 범위 산출이라
 * 거의 변하지 않으므로 staleTime 을 길게 둔다.
 */
export function useOroraDailyChunks() {
  return useQuery({
    queryKey: [...KEY_BASE, 'orora-daily-chunks'],
    queryFn: getOroraDailyChunks,
    staleTime: Infinity,
  });
}

/**
 * ORORA 일매출 거래처 청크 단위 수동 적재 트리거. 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerOroraDailyMaterializeChunk() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ chunkIndex, salesMonth }: { chunkIndex: number; salesMonth?: string }) =>
      triggerOroraDailyMaterializeChunk(chunkIndex, salesMonth),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'runs'] });
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'summary'] });
    },
  });
}

/**
 * 전문행사조(PPT) 마스터 배치 수동 실행 트리거. 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerPptMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (action: PptMasterTriggerAction) => triggerPptMaster(action),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'runs'] });
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'summary'] });
    },
  });
}
