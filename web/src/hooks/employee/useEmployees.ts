import { useQuery } from '@tanstack/react-query';
import { fetchEmployees, type FetchEmployeesParams } from '@/api/employee';

export function useEmployees(params: FetchEmployeesParams) {
  return useQuery({
    queryKey: ['admin', 'employees', params.status, params.costCenterCode, params.keyword, params.role, params.page, params.size],
    queryFn: () => fetchEmployees(params),
  });
}
