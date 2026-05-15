import { useQuery } from '@tanstack/react-query';
import { fetchProfessionalPromotionTeams } from '@/api/team-schedule';

export function useProfessionalPromotionTeams() {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'professional-promotion-teams'],
    queryFn: fetchProfessionalPromotionTeams,
    staleTime: 5 * 60 * 1000,
  });
}
