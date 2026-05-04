/**
 * 공통 API 응답 타입 (Spec #580 P3-W).
 *
 * 22개 api 모듈이 동일한 형태로 중복 정의하던 `ApiResponse<T>` 를 단일 모듈로 통합한다.
 * 백엔드 wire format 은 P1-B 적용 이후 camelCase 단일 정책이며, 본 타입은 그 응답 봉투를 표현한다.
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

/**
 * axios `error.response?.data` 의 타입을 좁히는 데 사용하는 별칭.
 *
 * 서버 에러 응답은 `success: false` 와 `error: { code, message }` 만 보장된다.
 */
export type ApiErrorBody = Pick<ApiResponse<unknown>, 'success' | 'error'>;

/**
 * `ApiErrorBody` 타입 가드.
 *
 * - `null` / `undefined` / 원시 타입 → false
 * - `{}` / `{ error: null }` / `{ error: {} }` → false (code 가 string 이 아님)
 * - `{ error: { code: 'X' } }` / `{ error: { code: 'X', message: 'Y' } }` → true
 */
export function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== 'object' || value === null) return false;
  const error = (value as { error?: unknown }).error;
  if (typeof error !== 'object' || error === null) return false;
  const code = (error as { code?: unknown }).code;
  return typeof code === 'string';
}
