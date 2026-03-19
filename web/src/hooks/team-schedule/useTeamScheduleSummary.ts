import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleSummary } from '@/api/team-schedule';

export function useTeamScheduleSummary(params: {
  year: number;
  month: number;
  employeeNumbers: string[];
  accountSfids: string[];
}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'summary', params.year, params.month, params.employeeNumbers, params.accountSfids],
    queryFn: () => fetchTeamScheduleSummary(params),
  });
}
