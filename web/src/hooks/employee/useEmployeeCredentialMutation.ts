import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  resetEmployeeDevice,
  resetEmployeePassword,
  type ResetDeviceResponse,
  type ResetPasswordResponse,
} from '@/api/admin/employeeCredential';

/**
 * 사원 단말 초기화 mutation hook (Spec #582 P2-W).
 *
 * @param isFemale 여사원 현황 화면에서 진입한 경우 `true` — `/female-employees/*`
 *   (`female_employee:EDIT` 가드) 엔드포인트를 호출. 설정 사원목록은 `false` (`MANAGE_USERS`).
 */
export function useResetDevice(isFemale = false) {
  const queryClient = useQueryClient();
  return useMutation<ResetDeviceResponse, Error, number>({
    mutationFn: (employeeId: number) => resetEmployeeDevice(employeeId, isFemale),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}

/**
 * 사원 비밀번호 임시 리셋 mutation hook (Spec #582 P2-W).
 *
 * @param isFemale 여사원 현황 화면에서 진입한 경우 `true` — `/female-employees/*`
 *   (`female_employee:EDIT` 가드) 엔드포인트를 호출. 설정 사원목록은 `false` (`MANAGE_USERS`).
 */
export function useResetPassword(isFemale = false) {
  const queryClient = useQueryClient();
  return useMutation<ResetPasswordResponse, Error, number>({
    mutationFn: (employeeId: number) => resetEmployeePassword(employeeId, isFemale),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}
