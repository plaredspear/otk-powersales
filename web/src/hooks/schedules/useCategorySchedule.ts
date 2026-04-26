import { useQuery } from '@tanstack/react-query';
import { fetchCategorySchedule } from '@/api/monthlyIntegration';

export function useCategorySchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
  enabled: boolean,
) {
  return useQuery({
    queryKey: ['admin', 'schedules', 'monthly-integration', 'category', year, month, costCenterCodes],
    queryFn: () => fetchCategorySchedule(year, month, costCenterCodes),
    enabled,
  });
}
