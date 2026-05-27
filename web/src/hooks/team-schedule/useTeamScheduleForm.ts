import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleForm } from '@/api/team-schedule';

export function useTeamScheduleForm() {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'form'],
    queryFn: fetchTeamScheduleForm,
  });
}
