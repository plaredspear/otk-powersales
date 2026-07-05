import { useMutation } from '@tanstack/react-query';
import { postPushTest } from '@/api/admin/pushTest';
import type { PushTestRequest, PushTestResponse } from '@/api/admin/pushTest';

/**
 * FCM push 발송 테스트 mutation 훅 (개발자 도구 > 외부 API 테스트).
 *
 * 단건 발송이라 invalidate 대상 query 없음. 페이지에서 onSuccess / onError 핸들링.
 */
export function usePushTest() {
  return useMutation<PushTestResponse, Error, PushTestRequest>({
    mutationFn: postPushTest,
  });
}
