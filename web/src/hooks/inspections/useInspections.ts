import { useQuery } from '@tanstack/react-query';
import { fetchInspections, type InspectionListParams } from '@/api/inspections';

export function useInspections(params: InspectionListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'inspections',
      params.startDate,
      params.endDate,
      params.category,
      params.fieldType,
      params.employeeName,
      params.accountCode,
      params.page,
      params.size,
    ],
    queryFn: () => fetchInspections(params),
  });
}
