import { useQuery } from '@tanstack/react-query';
import {
  fetchEmployees,
  fetchWomanEmployees,
  type FetchEmployeesParams,
  type FetchWomanEmployeesParams,
} from '@/api/employee';

export function useEmployees(params: FetchEmployeesParams) {
  return useQuery({
    queryKey: ['admin', 'employees', params.status, params.costCenterCode, params.keyword, params.role, params.page, params.size],
    queryFn: () => fetchEmployees(params),
  });
}

export function useWomanEmployees(params: FetchWomanEmployeesParams) {
  return useQuery({
    queryKey: ['admin', 'women-employees', params.status, params.costCenterCode, params.keyword, params.page, params.size],
    queryFn: () => fetchWomanEmployees(params),
  });
}
