import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  updateTeamSchedule,
  deleteTeamSchedule,
  type TeamScheduleUpdateRequest,
} from '@/api/team-schedule';

export function useUpdateTeamSchedule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TeamScheduleUpdateRequest }) =>
      updateTeamSchedule(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'team-schedule'] });
    },
  });
}

export function useDeleteTeamSchedule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteTeamSchedule(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'team-schedule'] });
    },
  });
}
