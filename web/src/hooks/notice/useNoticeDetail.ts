import { useQuery } from '@tanstack/react-query';
import { fetchNoticeDetail } from '@/api/notice';

export function useNoticeDetail(id: number) {
  return useQuery({
    queryKey: ['admin', 'notices', id],
    queryFn: () => fetchNoticeDetail(id),
    enabled: id > 0,
  });
}
