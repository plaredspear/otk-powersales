import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  from: string;
  to: string;
  employeeIds: number[];
  accountIds: number[];
  promotionTeams: string[];
}) {
  const hasFilter = params.employeeIds.length > 0 || params.accountIds.length > 0;
  return useQuery({
    queryKey: [
      'admin',
      'team-schedule',
      'list',
      params.from,
      params.to,
      params.employeeIds,
      params.accountIds,
      params.promotionTeams,
    ],
    queryFn: () => fetchTeamSchedules(params),
    enabled: hasFilter,
  });
}
