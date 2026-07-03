/**
 * 비밀번호 정책 (로그인 후 비밀번호 변경 — web + mobile + backend 공통).
 *
 * 백엔드 권위는 `PasswordPolicyValidator`. 클라이언트는 동일 규칙을 사전 검증 UX 로 표현한다.
 * - 8자 이상
 * - 영문 대문자 / 영문 소문자 / 숫자 / 특수문자 중 3종 이상 조합
 *   (한글 등 기타 문자는 어느 카테고리에도 속하지 않아 종류 수에 포함되지 않는다)
 */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MIN_CHARACTER_TYPES = 3;

/** 8자 이상 + 3종 이상 조합 안내 문구 (Form extra / placeholder 등에 사용). */
export const PASSWORD_POLICY_HINT =
  '8자 이상, 영문 대/소문자·숫자·특수문자 중 3종 이상 조합';

/**
 * 특수문자 = 유니코드 letter / number / 공백이 아닌 문자.
 * backend `PasswordPolicyValidator` (`!isLetterOrDigit() && !isWhitespace()`) 와 동일 판정 —
 * 한글 등 letter 는 어느 카테고리에도 넣지 않으므로 특수문자로 세지 않는다.
 */
const SPECIAL_CHAR_PATTERN = /[^\p{L}\p{N}\s]/u;

/** 영문 대문자/소문자/숫자/특수문자 중 몇 종을 포함하는지 계산. */
export function countCharacterTypes(password: string): number {
  let types = 0;
  if (/[A-Z]/.test(password)) types++;
  if (/[a-z]/.test(password)) types++;
  if (/[0-9]/.test(password)) types++;
  if (SPECIAL_CHAR_PATTERN.test(password)) types++;
  return types;
}

export function isLengthValid(password: string): boolean {
  return password.length >= PASSWORD_MIN_LENGTH;
}

export function hasEnoughCharacterTypes(password: string): boolean {
  return countCharacterTypes(password) >= PASSWORD_MIN_CHARACTER_TYPES;
}

/** 전체 정책 충족 여부. */
export function isPasswordValid(password: string): boolean {
  return isLengthValid(password) && hasEnoughCharacterTypes(password);
}

/** 정책 규칙 1건 (실시간 체크리스트 UI 렌더링용). */
export interface PasswordPolicyRule {
  /** 규칙 라벨 (사용자에게 표시). */
  label: string;
  /** 해당 비밀번호가 이 규칙을 충족하는지. */
  isValid: boolean;
}

/** 비밀번호에 대한 각 정책 규칙의 충족 여부 목록 (실시간 체크리스트용). */
export function getPasswordPolicyRules(password: string): PasswordPolicyRule[] {
  return [
    { label: `${PASSWORD_MIN_LENGTH}자 이상`, isValid: isLengthValid(password) },
    {
      label: '영문 대/소문자·숫자·특수문자 중 3종 이상 조합',
      isValid: hasEnoughCharacterTypes(password),
    },
  ];
}
