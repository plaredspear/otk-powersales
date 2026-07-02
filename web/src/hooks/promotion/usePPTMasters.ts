import { keepPreviousData, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getPPTMasters,
  createPPTMaster,
  updatePPTMaster,
  deletePPTMaster,
  confirmPPTMastersByIds,
  type PPTMasterSearchParams,
  type PPTMasterFormData,
} from '@/api/pptMaster';

const QUERY_KEY = ['admin', 'ppt-masters'];

export function usePPTMasters(params: PPTMasterSearchParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: () => getPPTMasters(params),
    // 페이지/조건 변경 시 직전 데이터를 유지해 테이블 깜빡임(빈 상태 노출)을 방지.
    placeholderData: keepPreviousData,
  });
}

export function useCreatePPTMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PPTMasterFormData) => createPPTMaster(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useUpdatePPTMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PPTMasterFormData }) =>
      updatePPTMaster(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useDeletePPTMaster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deletePPTMaster(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useConfirmPPTMastersByIds() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => confirmPPTMastersByIds(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}
