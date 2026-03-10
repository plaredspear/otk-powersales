import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createEducation, updateEducation, deleteEducation } from '@/api/education';

export function useCreateEducation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => createEducation(formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'education'] });
    },
  });
}

export function useUpdateEducation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, formData }: { id: string; formData: FormData }) => updateEducation(id, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'education'] });
    },
  });
}

export function useDeleteEducation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteEducation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'education'] });
    },
  });
}
