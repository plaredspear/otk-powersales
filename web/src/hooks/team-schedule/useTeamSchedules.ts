import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  year: number;
  month: number;
  employeeNumbers: string[];
  accountSfids: string[];
}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'list', params.year, params.month, params.employeeNumbers, params.accountSfids],
    queryFn: () => fetchTeamSchedules(params),
  });
}
