import { useQuery } from '@tanstack/react-query';
import { fetchMonthlyIntegrationSchedule } from '@/api/monthlyIntegration';

export function useMonthlyIntegrationSchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
  enabled: boolean,
) {
  return useQuery({
    queryKey: ['admin', 'schedules', 'monthly-integration', year, month, costCenterCodes],
    queryFn: () => fetchMonthlyIntegrationSchedule(year, month, costCenterCodes),
    enabled,
  });
}
