import { useMutation } from '@tanstack/react-query';
import { uploadScheduleExcel, confirmScheduleUpload } from '@/api/schedule';

export function useScheduleUpload() {
  return useMutation({
    mutationFn: (file: File) => uploadScheduleExcel(file),
  });
}

export function useScheduleConfirm() {
  return useMutation({
    mutationFn: (uploadId: string) => confirmScheduleUpload(uploadId),
  });
}
