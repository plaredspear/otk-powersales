import { useMutation } from '@tanstack/react-query';
import { fetchMonthlyIntegrationExport } from '@/api/monthlyIntegration';

export function useMonthlyIntegrationExport() {
  return useMutation({
    mutationFn: (params: {
      year: number;
      month: number;
      costCenterCodes: string[];
      keyword?: string;
      accountKeyword?: string;
      distributionKeyword?: string;
      accountTypeKeyword?: string;
    }) =>
      fetchMonthlyIntegrationExport(
        params.year,
        params.month,
        params.costCenterCodes,
        params.keyword,
        params.accountKeyword,
        params.distributionKeyword,
        params.accountTypeKeyword,
      ),
  });
}
