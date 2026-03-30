import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  year: number;
  month: number;
  employeeIds: number[];
  accountIds: number[];
}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'list', params.year, params.month, params.employeeIds, params.accountIds],
    queryFn: () => fetchTeamSchedules(params),
  });
}
