import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleAccounts } from '@/api/team-schedule';

interface Options {
  enabled?: boolean;
}

export function useTeamScheduleAccounts(branchCode: string, options: Options = {}) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'accounts', branchCode],
    queryFn: () => fetchTeamScheduleAccounts(branchCode),
    enabled: options.enabled ?? true,
  });
}
