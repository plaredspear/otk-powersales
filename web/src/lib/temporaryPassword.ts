/**
 * 임시 비밀번호 발급 규칙 (backend `TemporaryPasswordPolicy` 미러).
 *
 * 초기화 API 는 발급된 평문을 응답에 담지 않는다 — 화면이 대상자의 사번으로 동일 규칙을
 * 재조립해 안내한다. 사번이 없는 계정(사원 미매칭 순수 관리자) 은 종전 고정값으로 되돌아간다.
 */
export const TEMPORARY_PASSWORD_SUFFIX = '@pwrs';

/** 사번이 없는 계정용 종전 고정 임시 비밀번호. */
export const TEMPORARY_PASSWORD_FALLBACK = 'pwrs1234!';

/** 사번 기반 임시 비밀번호 평문을 산출한다. */
export function temporaryPasswordFor(employeeCode?: string | null): string {
  const code = employeeCode?.trim();
  return code ? `${code}${TEMPORARY_PASSWORD_SUFFIX}` : TEMPORARY_PASSWORD_FALLBACK;
}

/** 대상자가 특정되지 않은 목록/툴팁용 안내 문구. */
export const TEMPORARY_PASSWORD_RULE_HINT = `임시 비밀번호는 '사번${TEMPORARY_PASSWORD_SUFFIX}' 형식입니다.`;
