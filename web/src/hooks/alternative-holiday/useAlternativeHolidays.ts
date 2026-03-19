import { useQuery } from '@tanstack/react-query';
import { fetchAlternativeHolidays } from '@/api/alternativeHoliday';
import type { AlternativeHolidayListParams } from '@/api/alternativeHoliday';

export function useAlternativeHolidays(params: AlternativeHolidayListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'alternative-holidays',
      params.startDate,
      params.endDate,
      params.status,
      params.employeeNumber,
      params.orgCode,
    ],
    queryFn: () => fetchAlternativeHolidays(params),
  });
}
