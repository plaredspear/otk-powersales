import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createHolidayMaster,
  updateHolidayMaster,
  deleteHolidayMaster,
  type HolidayMasterRequest,
} from '@/api/holidayMaster';

export function useCreateHolidayMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: HolidayMasterRequest) => createHolidayMaster(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'holiday-masters'] });
    },
  });
}

export function useUpdateHolidayMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: HolidayMasterRequest }) =>
      updateHolidayMaster(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'holiday-masters'] });
    },
  });
}

export function useDeleteHolidayMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteHolidayMaster(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'holiday-masters'] });
    },
  });
}
