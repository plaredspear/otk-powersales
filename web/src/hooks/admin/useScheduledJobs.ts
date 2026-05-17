import { useQuery } from '@tanstack/react-query';
import {
  getScheduledJobCatalog,
  getScheduledJobRuns,
  getScheduledJobSummary,
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
