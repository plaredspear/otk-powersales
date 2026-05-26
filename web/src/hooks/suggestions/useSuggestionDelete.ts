import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteSuggestion } from '@/api/suggestions';

export function useSuggestionDelete() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, number>({
    mutationFn: (id) => deleteSuggestion(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'suggestions'] });
    },
  });
}
