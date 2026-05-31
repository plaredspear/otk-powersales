import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createInspection,
  fetchInspections,
  type CreateInspectionRequest,
  type InspectionListParams,
} from '@/api/inspections';

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

export function useCreateInspection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ request, photos }: { request: CreateInspectionRequest; photos: File[] }) =>
      createInspection(request, photos),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'inspections'] });
    },
  });
}
