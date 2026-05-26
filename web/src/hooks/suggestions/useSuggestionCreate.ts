import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createSuggestion, type SuggestionCreatePayload, type SuggestionCreateResponseData } from '@/api/suggestions';

export function useSuggestionCreate() {
  const queryClient = useQueryClient();
  return useMutation<SuggestionCreateResponseData, Error, { payload: SuggestionCreatePayload; photos: File[] }>({
    mutationFn: ({ payload, photos }) => createSuggestion(payload, photos),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions'] });
    },
  });
}
