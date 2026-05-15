import { useQuery } from '@tanstack/react-query';
import { fetchTeamMembers } from '@/api/team-schedule';

export function useTeamMembers(branchCode?: string) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'members', branchCode ?? ''],
    queryFn: () => fetchTeamMembers(branchCode),
  });
}
