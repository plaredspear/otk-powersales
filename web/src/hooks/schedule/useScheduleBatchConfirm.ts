import { useMutation, useQueryClient } from '@tanstack/react-query';
import { batchConfirmSchedules, batchUnconfirmSchedules } from '@/api/schedule';

export function useScheduleBatchConfirm() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => batchConfirmSchedules(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'schedule', 'list'] });
    },
  });
}

export function useScheduleBatchUnconfirm() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => batchUnconfirmSchedules(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'schedule', 'list'] });
    },
  });
}
