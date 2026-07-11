/**
 * 전문행사조 유형(PPT) 상수 모듈
 *
 * Backend `ProfessionalPromotionTeamType` enum과 동일한 한글 displayName을 사용한다.
 * API 응답은 string으로 받고 UI 렌더링 단계에서 union type으로 좁혀 사용한다.
 */

export type PPTTeamType =
  | '일반'
  | '라면세일조'
  | '프레시세일조_냉장'
  | '프레시세일조_냉동'
  | '프레시세일조_만두'
  | '카레세일조';

export const PPT_TEAM_TYPES: readonly PPTTeamType[] = [
  '일반',
  '라면세일조',
  '프레시세일조_냉장',
  '프레시세일조_냉동',
  '프레시세일조_만두',
  '카레세일조',
] as const;

/** 마스터 등록 가능한 5개 (일반 제외) */
export const PPT_TEAM_TYPES_FOR_MASTER: readonly PPTTeamType[] = [
  '라면세일조',
  '프레시세일조_냉장',
  '프레시세일조_냉동',
  '프레시세일조_만두',
  '카레세일조',
] as const;

interface SelectOption {
  value: PPTTeamType;
  label: string;
}

/** Ant Design Select용 옵션 (마스터 등록용 5개) */
export const PPT_TEAM_TYPE_OPTIONS: SelectOption[] = PPT_TEAM_TYPES_FOR_MASTER.map((v) => ({
  value: v,
  label: v,
}));

/** Ant Design Select용 옵션 (필터 등 일반 포함 시나리오용 6개) */
export const PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL: SelectOption[] = PPT_TEAM_TYPES.map((v) => ({
  value: v,
  label: v,
}));

/** Ant Design Tag 색상 매핑 */
export const PPT_TEAM_TYPE_COLORS: Record<PPTTeamType, string> = {
  '일반': 'default',
  '라면세일조': 'red',
  '프레시세일조_냉장': 'blue',
  '프레시세일조_냉동': 'cyan',
  '프레시세일조_만두': 'green',
  '카레세일조': 'orange',
};

/**
 * 표시명이 바뀐 유형의 이전(legacy) 문자열 → 색상 매핑.
 *
 * Backend 는 응답을 항상 신규 표시명('카레세일조')으로 정규화하므로 통상 legacy 문자열이
 * 화면에 도달하지 않지만, 만일의 경우('카레행사조')에도 색상이 깨지지 않도록 방어적으로 매핑한다.
 */
const PPT_TEAM_TYPE_LEGACY_COLORS: Record<string, string> = {
  '카레행사조': 'orange',
};

/**
 * 전문행사조 값에 매칭되는 Tag 색상을 반환한다.
 * 미정의 값(enum 외)은 'default'를 반환하여 UI 깨짐을 방지한다.
 */
export function getPPTTeamTypeColor(type: string | null | undefined): string {
  if (type == null) return 'default';
  return PPT_TEAM_TYPE_COLORS[type as PPTTeamType] ?? PPT_TEAM_TYPE_LEGACY_COLORS[type] ?? 'default';
}

export function isPPTTeamType(value: unknown): value is PPTTeamType {
  return typeof value === 'string' && (PPT_TEAM_TYPES as readonly string[]).includes(value);
}
