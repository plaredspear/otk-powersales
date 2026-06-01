import dayjs from 'dayjs';

/** LocalDateTime/LocalDate 문자열 → 'YYYY.MM.DD' */
export function formatDate(value?: string | null): string {
  if (!value) return '-';
  const d = dayjs(value);
  return d.isValid() ? d.format('YYYY.MM.DD') : '-';
}

/** LocalDateTime → 'YYYY.MM.DD HH:mm' */
export function formatDateTime(value?: string | null): string {
  if (!value) return '-';
  const d = dayjs(value);
  return d.isValid() ? d.format('YYYY.MM.DD HH:mm') : '-';
}

/** 숫자 → 천단위 콤마. null/undefined 는 '-'. */
export function formatNumber(value?: number | null): string {
  if (value == null) return '-';
  return value.toLocaleString('ko-KR');
}

/** 금액(원) 표기. */
export function formatWon(value?: number | null): string {
  if (value == null) return '-';
  return `${value.toLocaleString('ko-KR')}원`;
}
