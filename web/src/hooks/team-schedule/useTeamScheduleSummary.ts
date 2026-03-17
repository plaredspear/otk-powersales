import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleSummary } from '@/api/team-schedule';

export function useTeamScheduleSummary(params: {
  year: number;
  month: number;
  employeeIds: string[];
  accountSfids: string[];
}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'summary', params.year, params.month, params.employeeIds, params.accountSfids],
    queryFn: () => fetchTeamScheduleSummary(params),
  });
}
