import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createInspection,
  deleteInspection,
  fetchInspections,
  updateInspection,
  type CreateInspectionRequest,
  type InspectionListParams,
  type UpdateInspectionRequest,
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
    // 페이지/필터 전환 중 직전 데이터 유지 — 테이블이 빈 상태로 깜빡이지 않게.
    placeholderData: keepPreviousData,
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

export function useUpdateInspection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateInspectionRequest }) =>
      updateInspection(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'inspections'] });
    },
  });
}

export function useDeleteInspection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteInspection(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'inspections'] });
    },
  });
}
