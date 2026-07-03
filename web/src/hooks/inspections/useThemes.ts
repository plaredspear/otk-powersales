import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createTheme,
  deleteTheme,
  fetchThemeBranches,
  fetchThemeDetail,
  fetchThemes,
  updateTheme,
  type ThemeListParams,
  type ThemeMutationRequest,
} from '@/api/inspectionThemes';
import { useAuthStore } from '@/stores/authStore';

const THEME_KEY = ['admin', 'inspection-themes'] as const;

/**
 * 현장점검 테마 관리 지점 셀렉터 옵션.
 *
 * 권한 주체별로 지점 목록이 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useThemeBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: [...THEME_KEY, 'branches', userId],
    queryFn: fetchThemeBranches,
  });
}

export function useThemes(params: ThemeListParams) {
  return useQuery({
    queryKey: [...THEME_KEY, params.keyword, params.department, params.branchCode, params.page, params.size],
    queryFn: () => fetchThemes(params),
    // 페이지/필터 전환 중 직전 데이터 유지 — 테이블이 빈 상태로 깜빡이지 않게.
    placeholderData: keepPreviousData,
  });
}

export function useThemeDetail(id: number | null) {
  return useQuery({
    queryKey: [...THEME_KEY, 'detail', id],
    queryFn: () => fetchThemeDetail(id as number),
    enabled: id != null && id > 0,
  });
}

export function useCreateTheme() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ThemeMutationRequest) => createTheme(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: THEME_KEY });
    },
  });
}

export function useUpdateTheme() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: ThemeMutationRequest }) => updateTheme(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: THEME_KEY });
    },
  });
}

export function useDeleteTheme() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteTheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: THEME_KEY });
    },
  });
}
