import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  year: number;
  month: number;
  employeeCodes: string[];
  accountSfids: string[];
}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'list', params.year, params.month, params.employeeCodes, params.accountSfids],
    queryFn: () => fetchTeamSchedules(params),
  });
}
