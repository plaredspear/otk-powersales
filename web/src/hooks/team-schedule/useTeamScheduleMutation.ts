import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createTeamSchedule,
  updateTeamSchedule,
  deleteTeamSchedule,
  type TeamScheduleCreateRequest,
  type TeamScheduleUpdateRequest,
} from '@/api/team-schedule';

export function useCreateTeamSchedule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: TeamScheduleCreateRequest) => createTeamSchedule(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'team-schedule'] });
    },
  });
}

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
