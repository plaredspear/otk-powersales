import { useQuery } from '@tanstack/react-query';
import { fetchEmployeeWorkHistory } from '@/api/employee';

export function useEmployeeWorkHistory(employeeId: number | undefined, limit = 10) {
  return useQuery({
    queryKey: ['admin', 'employee', employeeId, 'work-history', limit],
    queryFn: () => fetchEmployeeWorkHistory(employeeId as number, limit),
    enabled: typeof employeeId === 'number',
  });
}
