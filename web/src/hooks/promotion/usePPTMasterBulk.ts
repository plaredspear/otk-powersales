import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  validatePPTMasterBulk,
  confirmPPTMasterBulk,
  type PPTMasterBulkItem,
} from '@/api/pptMaster';

export function useValidatePPTMasterBulk() {
  return useMutation({
    mutationFn: (items: PPTMasterBulkItem[]) => validatePPTMasterBulk(items),
  });
}

export function useConfirmPPTMasterBulk() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (items: PPTMasterBulkItem[]) => confirmPPTMasterBulk(items),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'ppt-masters'] });
    },
  });
}
