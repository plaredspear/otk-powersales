import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  bulkConfirmEmployeeInputCriteriaMasters,
  confirmEmployeeInputCriteriaMaster,
  createEmployeeInputCriteriaMaster,
  deleteEmployeeInputCriteriaMaster,
  fetchAccountCategoryOptions,
  fetchEmployeeInputCriteriaMasters,
  updateEmployeeInputCriteriaMaster,
  type EmployeeInputCriteriaMasterRequest,
  type ValidStatusFilter,
} from '@/api/employeeInputCriteriaMaster';

const QUERY_KEY = ['admin', 'employee-input-criteria-masters'];

export function useEmployeeInputCriteriaMasters(status: ValidStatusFilter) {
  return useQuery({
    queryKey: [...QUERY_KEY, status],
    queryFn: () => fetchEmployeeInputCriteriaMasters(status),
  });
}

export function useAccountCategoryOptions() {
  return useQuery({
    queryKey: [...QUERY_KEY, 'account-categories'],
    queryFn: () => fetchAccountCategoryOptions(),
  });
}

function invalidate(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: QUERY_KEY });
}

export function useCreateEmployeeInputCriteriaMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EmployeeInputCriteriaMasterRequest) => createEmployeeInputCriteriaMaster(data),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useUpdateEmployeeInputCriteriaMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: EmployeeInputCriteriaMasterRequest }) =>
      updateEmployeeInputCriteriaMaster(id, data),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useConfirmEmployeeInputCriteriaMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => confirmEmployeeInputCriteriaMaster(id),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useBulkConfirmEmployeeInputCriteriaMasters() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => bulkConfirmEmployeeInputCriteriaMasters(ids),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useDeleteEmployeeInputCriteriaMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteEmployeeInputCriteriaMaster(id),
    onSuccess: () => invalidate(queryClient),
  });
}
