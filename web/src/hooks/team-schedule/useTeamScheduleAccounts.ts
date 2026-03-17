import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleAccounts } from '@/api/team-schedule';

export function useTeamScheduleAccounts(branchCode: string) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'accounts', branchCode],
    queryFn: () => fetchTeamScheduleAccounts(branchCode),
  });
}
