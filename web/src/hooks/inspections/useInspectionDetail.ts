import { useQuery } from '@tanstack/react-query';
import { fetchInspectionDetail } from '@/api/inspections';

export function useInspectionDetail(id: number | null) {
  return useQuery({
    queryKey: ['admin', 'inspections', 'detail', id],
    queryFn: () => fetchInspectionDetail(id as number),
    enabled: id != null && id > 0,
  });
}
