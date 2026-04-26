import { useQuery } from '@tanstack/react-query';
import { fetchNotices, type NoticeListParams } from '@/api/notice';

export function useNotices(params: NoticeListParams) {
  return useQuery({
    queryKey: ['admin', 'notices', params.category, params.search, params.page, params.size],
    queryFn: () => fetchNotices(params),
  });
}
