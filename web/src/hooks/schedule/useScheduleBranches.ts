import { useQuery } from '@tanstack/react-query';
import { fetchScheduleBranches } from '@/api/schedule';

export function useScheduleBranches() {
  return useQuery({
    queryKey: ['admin', 'schedule', 'branches'],
    queryFn: fetchScheduleBranches,
  });
}
