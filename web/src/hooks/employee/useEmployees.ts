import { useQuery } from '@tanstack/react-query';
import {
  fetchEmployees,
  fetchFemaleEmployees,
  type FetchEmployeesParams,
  type FetchFemaleEmployeesParams,
} from '@/api/employee';

export function useEmployees(params: FetchEmployeesParams) {
  return useQuery({
    queryKey: ['admin', 'employees', params.status, params.costCenterCode, params.keyword, params.role, params.page, params.size],
    queryFn: () => fetchEmployees(params),
  });
}

export function useFemaleEmployees(params: FetchFemaleEmployeesParams) {
  return useQuery({
    queryKey: ['admin', 'female-employees', params.status, params.costCenterCode, params.keyword, params.page, params.size],
    queryFn: () => fetchFemaleEmployees(params),
  });
}
