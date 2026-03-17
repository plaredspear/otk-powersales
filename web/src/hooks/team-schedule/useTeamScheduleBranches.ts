import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleBranches } from '@/api/team-schedule';

export function useTeamScheduleBranches() {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'branches'],
    queryFn: fetchTeamScheduleBranches,
  });
}
