/**
 * 거래처목표등록마스터의 당월/전월 매출 실적 → **원천 기준정보(ORORA 월매출)** 링크 helper.
 *
 * 실적 2컬럼은 입력값이 아니라 `monthly_sales_history` 의 마감 합계(전산합 + 물류합)에서 산출된
 * 파생값이다 (backend `SalesProgressRateMasterActualsService`). 값이 이상해 보일 때 근거를 바로
 * 확인할 수 있도록, 화면의 금액에서 해당 거래처 + 매출월의 ORORA 월매출 조회로 이동시킨다.
 */

/** 기준정보 > ORORA 월매출 화면 경로. */
export const ORORA_MONTHLY_SALES_PATH = '/settings/orora-monthly-sales';

/** ORORA 월매출 화면의 권한 가드 entity (링크 노출 여부 판정에 사용). */
export const MONTHLY_SALES_HISTORY_ENTITY = 'monthly_sales_history';

/**
 * 목표행의 (목표년도, 목표월) 에서 `YYYYMM` 매출월 토큰을 만든다.
 *
 * @param monthOffset 0 = 당월(목표월 자체), -1 = 전월. 연도 rollover 를 처리한다
 *   (1월의 전월 = 전년 12월 — backend 산출 로직과 동일 규칙).
 * @returns 연/월을 숫자로 해석할 수 없으면 null (링크 미노출).
 */
export function toSalesMonthToken(
  targetYear: string | null | undefined,
  targetMonth: string | null | undefined,
  monthOffset = 0,
): string | null {
  const year = Number(targetYear?.trim());
  const month = Number(targetMonth?.trim());
  if (!Number.isInteger(year) || !Number.isInteger(month)) return null;
  if (year < 1900 || month < 1 || month > 12) return null;

  // 목표월 표기는 zero-pad 비보장('9' / '09')이라 숫자로 정규화한 뒤 offset 을 적용한다.
  const zeroBased = year * 12 + (month - 1) + monthOffset;
  const shiftedYear = Math.floor(zeroBased / 12);
  const shiftedMonth = (zeroBased % 12) + 1;
  return `${shiftedYear}${String(shiftedMonth).padStart(2, '0')}`;
}

/**
 * ORORA 월매출 화면으로 이동하는 링크 href — 거래처코드 + 매출월이 조회조건으로 채워져 바로 조회된다.
 *
 * @param accountName 화면 상단에 표시할 거래처명 (선택).
 */
export function buildMonthlySalesSourceHref(
  accountCode: string,
  salesMonth: string,
  accountName?: string | null,
): string {
  const params = new URLSearchParams({ accountCode, salesMonth });
  if (accountName) params.set('accountName', accountName);
  return `${ORORA_MONTHLY_SALES_PATH}?${params.toString()}`;
}
