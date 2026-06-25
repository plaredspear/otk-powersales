import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createAttendInfo,
  deleteAttendInfo,
  fetchAttendInfoBranches,
  fetchAttendInfoMembers,
  getAttendInfo,
  searchAttendInfo,
  updateAttendInfo,
  type CreateAttendInfoRequest,
  type FetchAttendInfoParams,
  type UpdateAttendInfoRequest,
} from '@/api/attendInfo';
import { useAuthStore } from '@/stores/authStore';

const QUERY_KEY = ['admin', 'attend-info'];

export function useAttendInfoList(params: FetchAttendInfoParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'list', params],
    queryFn: () => searchAttendInfo(params),
  });
}

/**
 * 근무기간 조회 "지점 선택" 옵션 (권한별 허용 지점).
 *
 * 지점 목록은 권한 주체(사용자)별로 다르므로 대행 시작/종료로 주체가 바뀌면 캐시가 분리되도록
 * 사용자 id 를 쿼리 키에 포함.
 */
export function useAttendInfoBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: [...QUERY_KEY, 'branches', userId],
    queryFn: fetchAttendInfoBranches,
  });
}

/**
 * 근무기간 조회 좌측 여사원 선택 목록 (퇴사/휴직 포함, attend_info 권한 가드).
 *
 * `branchCode` 지정 시 해당 지점 여사원 조회 (다중/전사 권한자). 미지정 시 본인 지점 스코프.
 */
export function useAttendInfoMembers(branchCode?: string) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'members', branchCode ?? ''],
    queryFn: () => fetchAttendInfoMembers(branchCode),
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
