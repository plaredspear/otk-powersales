import { useQuery } from '@tanstack/react-query';
import { fetchTeamMembers } from '@/api/team-schedule';

export function useTeamMembers() {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'members'],
    queryFn: fetchTeamMembers,
  });
}
