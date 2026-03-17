import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  year: number;
  month: number;
  employeeIds: string[];
  accountSfids: string[];
}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'list', params.year, params.month, params.employeeIds, params.accountSfids],
    queryFn: () => fetchTeamSchedules(params),
  });
}
