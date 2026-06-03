import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createSchedule,
  updateSchedule,
  fetchScheduleDetail,
  type ScheduleCreateRequest,
  type ScheduleUpdateRequest,
} from '@/api/schedule';

/** 단건 편집 모달 상세 조회 — enabled=true 일 때만 fetch (모달 open + 편집 모드). */
export function useScheduleDetail(id: number | null, enabled: boolean) {
  return useQuery({
    queryKey: ['admin', 'schedule', 'detail', id],
    queryFn: () => fetchScheduleDetail(id!),
    enabled: enabled && id != null,
    staleTime: 0,
  });
}

export function useScheduleCreate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ScheduleCreateRequest) => createSchedule(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'schedule', 'list'] });
    },
  });
}

export function useScheduleUpdate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ScheduleUpdateRequest }) => updateSchedule(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'schedule', 'list'] });
    },
  });
}
