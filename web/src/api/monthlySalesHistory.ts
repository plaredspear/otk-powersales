import client from './client';
import type { ApiResponse } from './types';

/**
 * ORORA 월매출 조회 API. (Backend `AdminMonthlySalesHistoryController`, `/api/v1/admin/monthly-sales-histories`)
 *
 * ORORA 월별 마감 적재 배치가 메인 RDS `monthly_sales_history` 에 적재한 결과를 거래처 + 매출년월
 * 단위로 조회한다(조회 전용). 권한: monthly_sales_history READ.
 */

export interface MonthlySalesHistoryListItem {
  id: number;
  /** 매출발생년 (`yyyy`). */
  salesYear: string | null;
  /** 매출발생월 (`MM`). */
  salesMonth: string | null;
  sapAccountCode: string | null;
  /** `거래처코드 + yyyy + MM` — 적재 upsert 키. */
  externalKey: string | null;
  /** 전산마감실적_상온 (원). */
  abcClosingAmount1: number | null;
  /** 전산마감실적_라면 (원). */
  abcClosingAmount2: number | null;
  /** 전산마감실적_냉장냉동 (원). */
  abcClosingAmount3: number | null;
  /** 전산마감실적_유지 (원). */
  abcClosingAmount4: number | null;
  /** 전산마감실적_합계 (원) — ORORA 가 내려주는 값이며 1~4 재합산이 아니다. */
  abcClosingSumAmount: number | null;
  /** 물류마감실적_상온 (원). */
  shipClosingAmount1: number | null;
  /** 물류마감실적_라면 (원). */
  shipClosingAmount2: number | null;
  /** 물류마감실적_냉장냉동 (원). */
  shipClosingAmount3: number | null;
  /** 물류마감실적_유지 (원). */
  shipClosingAmount4: number | null;
  /** 물류마감실적_합계 (원) — ORORA 가 내려주는 값이며 1~4 재합산이 아니다. */
  shipClosingSumAmount: number | null;
  /** SF `IsDeleted` soft-delete 여부 — 목록에는 노출되지만 합계에서는 제외된 행. */
  isDeleted: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface MonthlySalesHistoryListResponse {
  salesMonth: string;
  sapAccountCode: string;
  accountName: string | null;
  branchName: string | null;
  content: MonthlySalesHistoryListItem[];
  /** 전산마감실적 합계 (원) — 적재된 합계 컬럼 기준, soft-delete 행 제외. */
  totalAbcClosingAmount: number;
  /** 물류마감실적 합계 (원) — 적재된 합계 컬럼 기준, soft-delete 행 제외. */
  totalShipClosingAmount: number;
  /** 조회한 거래처 + 매출년월 행의 마지막 적재 시각 (max(updatedAt)). 결과 0건이면 null. */
  lastMaterializedAt: string | null;
}

export interface FetchMonthlySalesHistoriesParams {
  /** 거래처코드 (`account.externalKey`). ORORA 원본 형식(선행 `000`) 입력도 서버가 흡수한다. */
  accountCode: string;
  /** 매출발생년월 (`yyyyMM`). */
  salesMonth: string;
}

/**
 * ORORA 월매출 목록 조회 — 거래처 1곳 + 매출년월 1개의 적재 행 + 금액 합계.
 *
 * 거래처/매출년월 모두 필수 (서버가 필수 파라미터로 강제). ApiResponse 의 data 만 언래핑하여 반환.
 */
export async function fetchMonthlySalesHistories(
  params: FetchMonthlySalesHistoriesParams,
): Promise<MonthlySalesHistoryListResponse> {
  const res = await client.get<ApiResponse<MonthlySalesHistoryListResponse>>(
    '/api/v1/admin/monthly-sales-histories',
    { params },
  );
  if (!res.data.data) {
    throw new Error(res.data.message || 'ORORA 월매출 조회에 실패했습니다');
  }
  return res.data.data;
}
