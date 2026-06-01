/**
 * 공통 API 응답 봉투 — backend `com.otoki.powersales.common.dto.ApiResponse` 와 1:1.
 *
 * web/(admin) 의 `api/types.ts` 와 동일 형태(camelCase wire format). 모바일앱용 web 도
 * 같은 backend 를 쓰므로 봉투 타입을 공유 형태로 재정의한다.
 */
export interface ApiErrorDetail {
  code: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error?: ApiErrorDetail;
  message?: string;
  timestamp?: string;
}

/** Spring `Page<T>` 직렬화 형태 (제품검색·제안 목록 등에서 사용). */
export interface SpringPage<T> {
  content: T[];
  number: number; // 0-base page index
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** 응답 봉투를 풀고 실패 시 메시지와 함께 throw 하는 헬퍼. */
export function unwrap<T>(res: { data: ApiResponse<T> }, fallbackMessage: string): T {
  if (!res.data.success || res.data.data == null) {
    throw new Error(res.data.error?.message || res.data.message || fallbackMessage);
  }
  return res.data.data;
}
