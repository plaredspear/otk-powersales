import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createNotice,
  updateNotice,
  deleteNotice,
  publishNotice,
  unpublishNotice,
  sendNoticePush,
  type NoticeFormData,
} from '@/api/notice';

export function useCreateNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: NoticeFormData) => createNotice(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices'] });
    },
  });
}

export function useUpdateNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: NoticeFormData }) => updateNotice(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices'] });
    },
  });
}

export function useDeleteNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteNotice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices'] });
    },
  });
}

export function usePublishNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => publishNotice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices'] });
    },
  });
}

export function useUnpublishNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => unpublishNotice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices'] });
    },
  });
}

export function useSendNoticePush() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => sendNoticePush(id),
    onSuccess: (_data, id) => {
      // 상세 발송 이력(pushSentCount/lastPush) 갱신을 위해 해당 공지 상세를 무효화.
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices', id] });
    },
  });
}
