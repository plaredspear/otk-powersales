import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  getOroraDailyChunks,
  getOroraMonthlyChunks,
  getScheduledJobCatalog,
  getScheduledJobDailyStatus,
  getScheduledJobRuns,
  getScheduledJobSummary,
  triggerMonthlyReaggregate,
  triggerMonthlyReaggregateChunk,
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

/** 실행현황/요약 위젯의 자동 갱신 주기 (ms). */
const POLL_INTERVAL_MS = 60_000;

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
    refetchInterval: POLL_INTERVAL_MS,
  });
}

/**
 * 대시보드 일별 스케줄 실행현황 조회. `date`(`YYYY-MM-DD`) 미지정 시 서버 오늘 날짜.
 *
 * 자동 갱신은 **오늘 조회 시에만** 켠다 — 과거/미래 날짜는 윈도우가 이미 확정돼 값이 변하지 않으므로
 * 폴링이 무의미하다. 백그라운드 탭에서는 갱신을 멈춰 불필요한 반복 호출을 줄인다.
 */
export function useScheduledJobDailyStatus(date?: string) {
  const isToday = !date || date === dayjs().format('YYYY-MM-DD');
  return useQuery({
    queryKey: [...KEY_BASE, 'daily-status', date ?? 'today'],
    queryFn: () => getScheduledJobDailyStatus(date),
    refetchInterval: isToday ? POLL_INTERVAL_MS : false,
    refetchIntervalInBackground: false,
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
 * 월별 합계 재집계(전체 범위) 트리거. ORORA 조회 없이 daily 로 monthly 재계산.
 * 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerMonthlyReaggregate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (salesMonth: string) => triggerMonthlyReaggregate(salesMonth),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'runs'] });
      queryClient.invalidateQueries({ queryKey: [...KEY_BASE, 'summary'] });
    },
  });
}

/**
 * 월별 합계 재집계 거래처 청크 단위 트리거. 성공 시 실행 이력/요약 쿼리를 무효화하여 갱신한다.
 */
export function useTriggerMonthlyReaggregateChunk() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ chunkIndex, salesMonth }: { chunkIndex: number; salesMonth: string }) =>
      triggerMonthlyReaggregateChunk(chunkIndex, salesMonth),
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
