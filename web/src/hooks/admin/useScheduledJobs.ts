import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getScheduledJobCatalog,
  getScheduledJobRuns,
  getScheduledJobSummary,
  triggerOroraMonthlyMaterialize,
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
