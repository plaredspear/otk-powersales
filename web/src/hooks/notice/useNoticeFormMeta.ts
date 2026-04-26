import { useQuery } from '@tanstack/react-query';
import { fetchNoticeFormMeta } from '@/api/notice';

export function useNoticeFormMeta() {
  return useQuery({
    queryKey: ['admin', 'notices', 'form-meta'],
    queryFn: fetchNoticeFormMeta,
    staleTime: 10 * 60 * 1000,
  });
}
