/**
 * localStorage 키 상수 (Spec #851).
 *
 * 인증 토큰/사용자 키는 기존 리터럴을 광범위하게 사용하므로 점진 정리 대상.
 * 대행(impersonation) 키는 authStore 와 axios 인터셉터(client.ts) 양쪽에서 정리되어야 하므로
 * 오타/불일치 방지를 위해 공유 상수로 추출한다.
 */
export const IMPERSONATION_STORAGE_KEY = 'impersonation';
