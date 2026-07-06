import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchEmployee,
  updateEmployee,
  updateEmployeeRole,
  manualRegisterEmployee,
  type EmployeeDetail,
  type EmployeeUpdateRequest,
  type EmployeeManualRegisterRequest,
} from '@/api/employee';
import type { AppAuthority } from '@/constants/userRole';

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

/**
 * 사원 권한(role) 전용 수정 — origin=SAP 사원도 허용(권한은 SAP 인입과 경합하지 않음).
 */
export function useUpdateEmployeeRole() {
  const queryClient = useQueryClient();
  return useMutation<EmployeeDetail, Error, { employeeId: number; role: AppAuthority }>({
    mutationFn: ({ employeeId, role }) => updateEmployeeRole(employeeId, role),
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
