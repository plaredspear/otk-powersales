import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSchedule, type ScheduleCreateRequest } from '@/api/schedule';

export function useScheduleCreate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ScheduleCreateRequest) => createSchedule(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'schedule', 'list'] });
    },
  });
}
