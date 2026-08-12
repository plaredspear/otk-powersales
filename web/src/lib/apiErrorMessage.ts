import { AxiosError } from 'axios';

/**
 * API 실패에서 사용자에게 보여줄 한글 메시지를 뽑는다.
 *
 * axios 는 4xx/5xx 를 reject 하므로 `err.message` 를 그대로 쓰면
 * `"Request failed with status code 403"` 같은 영문 문구가 노출된다.
 * 서버가 내려준 사유(`error.message` → `message`) 를 우선 사용하고, 없을 때만 폴백한다.
 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof AxiosError) {
    const data = err.response?.data as
      | { message?: string; error?: { message?: string } }
      | undefined;
    const serverMessage = data?.error?.message ?? data?.message;
    if (serverMessage) return serverMessage;
  }
  // API 래퍼가 success:false 를 Error 로 바꿔 던진 경우 (HTTP 2xx 경로).
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}
