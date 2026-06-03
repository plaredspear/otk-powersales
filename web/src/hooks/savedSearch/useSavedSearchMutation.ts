import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createSavedSearch,
  updateSavedSearch,
  deleteSavedSearch,
  type SavedSearchCreateRequest,
  type SavedSearchUpdateRequest,
} from '@/api/savedSearch';

function useInvalidate() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['admin', 'saved-searches'] });
}

export function useCreateSavedSearch() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (request: SavedSearchCreateRequest) => createSavedSearch(request),
    onSuccess: invalidate,
  });
}

export function useUpdateSavedSearch() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: SavedSearchUpdateRequest }) =>
      updateSavedSearch(id, request),
    onSuccess: invalidate,
  });
}

export function useDeleteSavedSearch() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (id: number) => deleteSavedSearch(id),
    onSuccess: invalidate,
  });
}
