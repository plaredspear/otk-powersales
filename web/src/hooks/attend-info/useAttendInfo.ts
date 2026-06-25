import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createAttendInfo,
  deleteAttendInfo,
  fetchAttendInfoMembers,
  getAttendInfo,
  searchAttendInfo,
  updateAttendInfo,
  type CreateAttendInfoRequest,
  type FetchAttendInfoParams,
  type UpdateAttendInfoRequest,
} from '@/api/attendInfo';

const QUERY_KEY = ['admin', 'attend-info'];

export function useAttendInfoList(params: FetchAttendInfoParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'list', params],
    queryFn: () => searchAttendInfo(params),
  });
}

/**
 * 근무기간 조회 좌측 여사원 선택 목록 (퇴사/휴직 포함, attend_info 권한 가드).
 */
export function useAttendInfoMembers() {
  return useQuery({
    queryKey: [...QUERY_KEY, 'members'],
    queryFn: fetchAttendInfoMembers,
  });
}

export function useAttendInfoDetail(id: number | null) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'detail', id],
    queryFn: () => getAttendInfo(id!),
    enabled: id != null,
  });
}

function invalidate(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: QUERY_KEY });
}

export function useCreateAttendInfo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAttendInfoRequest) => createAttendInfo(data),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useUpdateAttendInfo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateAttendInfoRequest }) =>
      updateAttendInfo(id, data),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useDeleteAttendInfo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAttendInfo(id),
    onSuccess: () => invalidate(queryClient),
  });
}
