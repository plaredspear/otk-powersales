import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  resetEmployeeDevice,
  resetEmployeePassword,
  type ResetDeviceResponse,
  type ResetPasswordResponse,
} from '@/api/admin/employeeCredential';

/**
 * 사원 단말 초기화 mutation hook (Spec #582 P2-W).
 */
export function useResetDevice() {
  const queryClient = useQueryClient();
  return useMutation<ResetDeviceResponse, Error, number>({
    mutationFn: (employeeId: number) => resetEmployeeDevice(employeeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}

/**
 * 사원 비밀번호 임시 리셋 mutation hook (Spec #582 P2-W).
 */
export function useResetPassword() {
  const queryClient = useQueryClient();
  return useMutation<ResetPasswordResponse, Error, number>({
    mutationFn: (employeeId: number) => resetEmployeePassword(employeeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}
