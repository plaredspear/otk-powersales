import client from './client';
import type { ApiResponse } from './types';

/**
 * ORORA 일매출 조회 API. (Backend `AdminDailySalesHistoryController`, `/api/v1/admin/daily-sales-histories`)
 *
 * ORORA 일별 매출 적재 배치가 메인 RDS `daily_sales_history` 에 적재한 결과를 거래처 + 매출월 단위로
 * 조회한다(조회 전용). 권한: daily_sales_history READ.
 */

export interface DailySalesHistoryListItem {
  id: number;
  /** 매출발생일자 (`yyyyMMdd`) — 적재 대상월이 당월이면 적재일, 아니면 그 달 말일. */
  salesDate: string;
  sapAccountCode: string;
  /** `거래처코드 + ORORA 원본 매출일자` — 적재 upsert 키. */
  externalKey: string;
  erpSalesAmount: number | null;
  erpDistributionAmount: number | null;
  ledgerAmount: number | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface DailySalesHistoryListResponse {
  salesMonth: string;
  sapAccountCode: string;
  accountName: string | null;
  branchName: string | null;
  content: DailySalesHistoryListItem[];
  totalErpSalesAmount: number;
  totalErpDistributionAmount: number;
  totalLedgerAmount: number;
  /** 조회한 거래처 + 매출월 행의 마지막 적재 시각 (max(updatedAt)). 결과 0건이면 null. */
  lastMaterializedAt: string | null;
}

export interface FetchDailySalesHistoriesParams {
  /** 거래처코드 (`account.externalKey`). ORORA 원본 형식(선행 `000`) 입력도 서버가 흡수한다. */
  accountCode: string;
  /** 매출발생년월 (`yyyyMM`). */
  salesMonth: string;
}

/**
 * ORORA 일매출 목록 조회 — 거래처 1곳 + 매출월 1개의 일별 적재 행 + 금액 합계.
 *
 * 거래처/매출월 모두 필수 (서버가 필수 파라미터로 강제). ApiResponse 의 data 만 언래핑하여 반환.
 */
export async function fetchDailySalesHistories(
  params: FetchDailySalesHistoriesParams,
): Promise<DailySalesHistoryListResponse> {
  const res = await client.get<ApiResponse<DailySalesHistoryListResponse>>(
    '/api/v1/admin/daily-sales-histories',
    { params },
  );
  if (!res.data.data) {
    throw new Error(res.data.message || 'ORORA 일매출 조회에 실패했습니다');
  }
  return res.data.data;
}
