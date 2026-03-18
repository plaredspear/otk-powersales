import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createAlternativeHoliday,
  approveAlternativeHoliday,
  rejectAlternativeHoliday,
} from '@/api/alternativeHoliday';
import type {
  CreateAlternativeHolidayPayload,
  ApproveAlternativeHolidayPayload,
  RejectAlternativeHolidayPayload,
} from '@/api/alternativeHoliday';

export function useCreateAlternativeHoliday() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAlternativeHolidayPayload) => createAlternativeHoliday(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'alternative-holidays'] });
    },
  });
}

export function useApproveAlternativeHoliday() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ApproveAlternativeHolidayPayload }) =>
      approveAlternativeHoliday(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'alternative-holidays'] });
    },
  });
}

export function useRejectAlternativeHoliday() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RejectAlternativeHolidayPayload }) =>
      rejectAlternativeHoliday(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'alternative-holidays'] });
    },
  });
}
