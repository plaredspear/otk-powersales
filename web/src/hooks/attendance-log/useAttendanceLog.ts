import { useQuery } from '@tanstack/react-query';
import {
  getAttendanceLog,
  searchAttendanceLog,
  type FetchAttendanceLogParams,
} from '@/api/attendanceLog';

const QUERY_KEY = ['admin', 'attendance-log'];

export function useAttendanceLogList(params: FetchAttendanceLogParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'list', params],
    queryFn: () => searchAttendanceLog(params),
  });
}

export function useAttendanceLogDetail(id: number | null) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'detail', id],
    queryFn: () => getAttendanceLog(id!),
    enabled: id != null,
  });
}
