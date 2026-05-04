import axios from 'axios';

/**
 * 백엔드 errorCode → 사용자 안내 메시지 매핑 (Spec #582 P2-W §1.4).
 */
const ERROR_MESSAGES: Record<string, string> = {
  EMP_NOT_FOUND: '사원 정보를 찾을 수 없습니다.',
  EMP_AUTH_FORBIDDEN: '이 작업을 수행할 권한이 없습니다.',
  EMP_LOGIN_INACTIVE: '앱 로그인이 비활성화된 사원입니다. 사원 정보를 먼저 활성화해 주세요.',
};

const HTTP_STATUS_MESSAGES: Record<number, string> = {
  401: '로그인이 필요합니다.',
  403: '이 작업을 수행할 권한이 없습니다.',
  404: '사원 정보를 찾을 수 없습니다.',
};

const FALLBACK_MESSAGE = '처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';

export function mapCredentialErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const code = err.response?.data?.error?.code as string | undefined;
    if (code && ERROR_MESSAGES[code]) return ERROR_MESSAGES[code];

    const status = err.response?.status;
    if (status && HTTP_STATUS_MESSAGES[status]) return HTTP_STATUS_MESSAGES[status];
  }
  return FALLBACK_MESSAGE;
}
