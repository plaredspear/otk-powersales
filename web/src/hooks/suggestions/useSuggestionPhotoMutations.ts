import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  deleteSuggestionPhoto,
  uploadSuggestionPhotos,
  type SuggestionAttachment,
} from '@/api/suggestions';

export function useSuggestionPhotoUpload(suggestionId: number) {
  const queryClient = useQueryClient();
  return useMutation<SuggestionAttachment[], Error, File[]>({
    mutationFn: (photos) => uploadSuggestionPhotos(suggestionId, photos),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions', suggestionId] });
    },
  });
}

export function useSuggestionPhotoDelete(suggestionId: number) {
  const queryClient = useQueryClient();
  return useMutation<void, Error, number>({
    mutationFn: (photoId) => deleteSuggestionPhoto(suggestionId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions', suggestionId] });
    },
  });
}
