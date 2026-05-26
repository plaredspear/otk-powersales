import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateSuggestion, type SuggestionDetail, type SuggestionUpdatePayload } from '@/api/suggestions';

export function useSuggestionUpdate(id: number) {
  const queryClient = useQueryClient();
  return useMutation<SuggestionDetail, Error, SuggestionUpdatePayload>({
    mutationFn: (payload) => updateSuggestion(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions', id] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions'] });
    },
  });
}
