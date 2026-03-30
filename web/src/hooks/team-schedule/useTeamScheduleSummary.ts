import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleSummary } from '@/api/team-schedule';

export function useTeamScheduleSummary(params: {
  year: number;
  month: number;
  employeeIds: number[];
  accountIds: number[];
}) {
  const hasFilter = params.employeeIds.length > 0 || params.accountIds.length > 0;
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'summary', params.year, params.month, params.employeeIds, params.accountIds],
    queryFn: () => fetchTeamScheduleSummary(params),
    enabled: hasFilter,
  });
}
