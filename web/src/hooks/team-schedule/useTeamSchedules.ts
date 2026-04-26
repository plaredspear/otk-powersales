import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  year: number;
  month: number;
  employeeIds: number[];
  accountIds: number[];
}) {
  const hasFilter = params.employeeIds.length > 0 || params.accountIds.length > 0;
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'list', params.year, params.month, params.employeeIds, params.accountIds],
    queryFn: () => fetchTeamSchedules(params),
    enabled: hasFilter,
  });
}
