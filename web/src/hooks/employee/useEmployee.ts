import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchEmployee,
  updateEmployee,
  updateEmployeeRole,
  updateEmployeeAppLoginActive,
  manualRegisterEmployee,
  confirmPostponedAppointment,
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

/**
 * 앱 로그인 활성(appLoginActive) 전용 수정 — origin=SAP 사원도 허용.
 *
 * 서버가 현장 여사원 보호 규칙을 적용하므로 응답값이 요청값과 다를 수 있다 (호출부가 결과 확인).
 */
export function useUpdateEmployeeAppLoginActive() {
  const queryClient = useQueryClient();
  return useMutation<EmployeeDetail, Error, { employeeId: number; appLoginActive: boolean }>({
    mutationFn: ({ employeeId, appLoginActive }) =>
      updateEmployeeAppLoginActive(employeeId, appLoginActive),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'employee', vars.employeeId] });
    },
  });
}

/**
 * 발령정보 승인 — 유예된 발령 참조를 즉시 반영 (SF Quick Action "신규발령확정" 동등).
 */
export function useConfirmPostponedAppointment() {
  const queryClient = useQueryClient();
  return useMutation<EmployeeDetail, Error, { employeeId: number }>({
    mutationFn: ({ employeeId }) => confirmPostponedAppointment(employeeId),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'employee', vars.employeeId] });
    },
  });
}

/** 사원 수동 등록 — 기준정보 > 사원 전용 (여사원 현황은 조회 전용). */
export function useManualRegisterEmployee() {
  const queryClient = useQueryClient();
  return useMutation<EmployeeDetail, Error, EmployeeManualRegisterRequest>({
    mutationFn: (request) => manualRegisterEmployee(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}
