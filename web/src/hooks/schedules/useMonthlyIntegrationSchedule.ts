import { useQuery } from '@tanstack/react-query';
import { fetchMonthlyIntegrationSchedule } from '@/api/monthlyIntegration';

export function useMonthlyIntegrationSchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
  enabled: boolean,
  keyword?: string,
  accountKeyword?: string,
  distributionCode?: string,
  accountTypeKeyword?: string,
) {
  return useQuery({
    queryKey: [
      'admin', 'schedules', 'monthly-integration', year, month, costCenterCodes,
      keyword ?? '', accountKeyword ?? '', distributionCode ?? '', accountTypeKeyword ?? '',
    ],
    queryFn: () =>
      fetchMonthlyIntegrationSchedule(
        year, month, costCenterCodes, keyword, accountKeyword, distributionCode, accountTypeKeyword,
      ),
    enabled,
  });
}
