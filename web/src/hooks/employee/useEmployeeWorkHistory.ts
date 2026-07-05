import { useQuery } from '@tanstack/react-query';
import {
  fetchEmployeeWorkHistory,
  fetchEmployeeMonthlyWorkHistory,
  type WorkHistoryScope,
} from '@/api/employee';

export function useEmployeeWorkHistory(
  employeeId: number | undefined,
  limit = 10,
  isFemale = false,
) {
  return useQuery({
    queryKey: ['admin', 'employee', employeeId, 'work-history', limit, isFemale ? 'female' : 'all'],
    queryFn: () => fetchEmployeeWorkHistory(employeeId as number, limit, isFemale),
    enabled: typeof employeeId === 'number',
  });
}

/**
 * 근무기간 조회(월별) — employeeId + yearMonth(`yyyy-MM`) 가 모두 있을 때만 조회.
 */
export function useEmployeeMonthlyWorkHistory(
  employeeId: number | undefined,
  yearMonth: string | undefined,
  scope: WorkHistoryScope = 'employee',
) {
  return useQuery({
    queryKey: ['admin', 'employee', employeeId, 'work-history', 'monthly', yearMonth, scope],
    queryFn: () => fetchEmployeeMonthlyWorkHistory(employeeId as number, yearMonth as string, scope),
    enabled: typeof employeeId === 'number' && !!yearMonth,
  });
}
