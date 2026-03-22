import { useMutation } from '@tanstack/react-query';
import { fetchCategoryExport } from '@/api/monthlyIntegration';

export function useCategoryExport() {
  return useMutation({
    mutationFn: (params: { year: number; month: number; costCenterCodes: string[] }) =>
      fetchCategoryExport(params.year, params.month, params.costCenterCodes),
  });
}
