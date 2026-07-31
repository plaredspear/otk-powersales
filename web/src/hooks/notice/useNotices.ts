import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchNotices, type NoticeListParams } from '@/api/notice';

export function useNotices(params: NoticeListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'notices',
      params.category,
      params.search,
      params.branchCode,
      params.page,
      params.size,
    ],
    queryFn: () => fetchNotices(params),
    placeholderData: keepPreviousData,
  });
}
