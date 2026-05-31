import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createTheme,
  deleteTheme,
  fetchThemeDetail,
  fetchThemes,
  updateTheme,
  type ThemeListParams,
  type ThemeMutationRequest,
} from '@/api/inspectionThemes';

const THEME_KEY = ['admin', 'inspection-themes'] as const;

export function useThemes(params: ThemeListParams) {
  return useQuery({
    queryKey: [...THEME_KEY, params.keyword, params.department, params.branchCode, params.page, params.size],
    queryFn: () => fetchThemes(params),
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
