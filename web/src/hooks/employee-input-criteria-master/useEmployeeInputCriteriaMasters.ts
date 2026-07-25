import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  bulkConfirmEmployeeInputCriteriaMasters,
  confirmEmployeeInputCriteriaMaster,
  createEmployeeInputCriteriaMaster,
  deleteEmployeeInputCriteriaMaster,
  fetchEmployeeInputCriteriaMasters,
  updateEmployeeInputCriteriaMaster,
  type EmployeeInputCriteriaMasterRequest,
  type ValidStatusFilter,
} from '@/api/employeeInputCriteriaMaster';

const QUERY_KEY = ['admin', 'employee-input-criteria-masters'];

/**
 * 목록 queryKey. 도메인 prefix 아래 'list' 세그먼트로 한 단계 내려 form-meta / list-meta 와 분리한다.
 * mutation 의 invalidate 가 정적 성격의 메타까지 재조회하지 않도록 하기 위함.
 */
const LIST_QUERY_KEY = [...QUERY_KEY, 'list'];

export function useEmployeeInputCriteriaMasters(status: ValidStatusFilter) {
  return useQuery({
    queryKey: [...LIST_QUERY_KEY, status],
    queryFn: () => fetchEmployeeInputCriteriaMasters(status),
  });
}

function invalidate(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: LIST_QUERY_KEY });
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
