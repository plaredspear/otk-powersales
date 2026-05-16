import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSchedule, updateSchedule, type ScheduleCreateRequest, type ScheduleUpdateRequest } from '@/api/schedule';

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
