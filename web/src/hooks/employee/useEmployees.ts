import { keepPreviousData, useQuery } from '@tanstack/react-query';
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
    // 재조회(페이지 이동/필터 변경) 중 이전 결과를 유지해 빈 화면 깜빡임 방지.
    placeholderData: keepPreviousData,
  });
}

export function useFemaleEmployees(params: FetchFemaleEmployeesParams) {
  return useQuery({
    queryKey: ['admin', 'female-employees', params.status, params.costCenterCode, params.keyword, params.page, params.size],
    queryFn: () => fetchFemaleEmployees(params),
    // 재조회(페이지 이동/필터 변경) 중 이전 결과를 유지해 빈 화면 깜빡임 방지.
    placeholderData: keepPreviousData,
  });
}
