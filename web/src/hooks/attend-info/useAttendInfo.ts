import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createAttendInfo,
  deleteAttendInfo,
  fetchAttendInfoBranches,
  fetchAttendInfoMembers,
  fetchWorkHistoryEmployeeAccounts,
  getAttendInfo,
  searchAttendInfo,
  updateAttendInfo,
  type CreateAttendInfoRequest,
  type FetchAttendInfoParams,
  type FetchWorkHistoryEmployeeAccountParams,
  type UpdateAttendInfoRequest,
} from '@/api/attendInfo';
import { useAuthStore } from '@/stores/authStore';

const QUERY_KEY = ['admin', 'attend-info'];

export function useAttendInfoList(params: FetchAttendInfoParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'list', params],
    queryFn: () => searchAttendInfo(params),
    placeholderData: keepPreviousData,
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
 * `options.enabled=false` 로 조회 지점 확정 전 불필요한 fetch 를 억제할 수 있다 (기본 활성).
 */
export function useAttendInfoMembers(branchCode?: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'members', branchCode ?? ''],
    queryFn: () => fetchAttendInfoMembers(branchCode),
    enabled: options?.enabled ?? true,
  });
}

/**
 * 기간별 근무내역(개인) — 여사원 1명의 거래처별 집계 조회.
 * params 가 null 이면 비활성 (여사원 미선택 또는 기간 입력 오류).
 */
export function useWorkHistoryEmployeeAccounts(params: FetchWorkHistoryEmployeeAccountParams | null) {
  return useQuery({
    queryKey: [...QUERY_KEY, 'period-summary-accounts', params],
    queryFn: () => fetchWorkHistoryEmployeeAccounts(params!),
    enabled: params != null,
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
