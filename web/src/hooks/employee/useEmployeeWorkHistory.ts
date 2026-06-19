import { useQuery } from '@tanstack/react-query';
import {
  fetchEmployeeWorkHistory,
  fetchEmployeeMonthlyWorkHistory,
} from '@/api/employee';

export function useEmployeeWorkHistory(employeeId: number | undefined, limit = 10) {
  return useQuery({
    queryKey: ['admin', 'employee', employeeId, 'work-history', limit],
    queryFn: () => fetchEmployeeWorkHistory(employeeId as number, limit),
    enabled: typeof employeeId === 'number',
  });
}

/**
 * 근무기간 조회(월별) — employeeId + yearMonth(`yyyy-MM`) 가 모두 있을 때만 조회.
 */
export function useEmployeeMonthlyWorkHistory(
  employeeId: number | undefined,
  yearMonth: string | undefined,
) {
  return useQuery({
    queryKey: ['admin', 'employee', employeeId, 'work-history', 'monthly', yearMonth],
    queryFn: () => fetchEmployeeMonthlyWorkHistory(employeeId as number, yearMonth as string),
    enabled: typeof employeeId === 'number' && !!yearMonth,
  });
}
