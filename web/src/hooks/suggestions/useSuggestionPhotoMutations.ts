import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteSuggestionPhoto } from '@/api/suggestions';

export function useSuggestionPhotoDelete(suggestionId: number) {
  const queryClient = useQueryClient();
  return useMutation<void, Error, number>({
    mutationFn: (photoId) => deleteSuggestionPhoto(suggestionId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions', suggestionId] });
    },
  });
}
