import { useQuery } from '@tanstack/react-query';
import { fetchHolidayMasters } from '@/api/holidayMaster';

export function useHolidayMasters(year: number) {
  return useQuery({
    queryKey: ['admin', 'holiday-masters', year],
    queryFn: () => fetchHolidayMasters(year),
  });
}
