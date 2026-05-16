import { useMutation, useQueryClient } from '@tanstack/react-query';
import { batchConfirmSchedules, batchUnconfirmSchedules, batchDeleteSchedules } from '@/api/schedule';

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

export function useScheduleBatchDelete() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => batchDeleteSchedules(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'schedule', 'list'] });
    },
  });
}
