import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchEmployee,
  updateEmployee,
  manualRegisterEmployee,
  type EmployeeDetail,
  type EmployeeUpdateRequest,
  type EmployeeManualRegisterRequest,
} from '@/api/employee';

export function useEmployee(employeeId: number | undefined, isFemale = false) {
  return useQuery({
    queryKey: ['admin', 'employee', employeeId, isFemale ? 'female' : 'all'],
    queryFn: () => fetchEmployee(employeeId as number, isFemale),
    enabled: typeof employeeId === 'number',
  });
}

export function useUpdateEmployee() {
  const queryClient = useQueryClient();
  return useMutation<EmployeeDetail, Error, { employeeId: number; request: EmployeeUpdateRequest }>({
    mutationFn: ({ employeeId, request }) => updateEmployee(employeeId, request),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'employee', vars.employeeId] });
    },
  });
}

export function useManualRegisterEmployee() {
  const queryClient = useQueryClient();
  return useMutation<EmployeeDetail, Error, EmployeeManualRegisterRequest>({
    mutationFn: (request) => manualRegisterEmployee(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}
