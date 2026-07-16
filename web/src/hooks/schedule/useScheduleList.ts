import { useQuery } from '@tanstack/react-query';
import { fetchScheduleList } from '@/api/schedule';
import type { ScheduleListParams } from '@/api/schedule';

export function useScheduleList(params: ScheduleListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'schedule',
      'list',
      params.page,
      params.size,
      params.employeeCode,
      params.accountName,
      params.accountType,
      params.accountStatus,
      params.confirmed,
      params.typeOfWork3,
      params.startDateFrom,
      params.startDateTo,
      params.validData,
      params.branchCode,
      params.sortBy,
      params.sortDir,
    ],
    queryFn: () => fetchScheduleList(params),
  });
}
