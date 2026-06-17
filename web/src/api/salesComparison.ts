import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/**
 * 집계표 카테고리 컬럼 — backend `AccountCategoryColumn` enum (고정 10컬럼) 과 1:1.
 *
 * SF `SalesComparisonSearchController.summaryItems` 고정 필드 + `getCategory` 코드 매핑 정합.
 * 검색조건의 거래처유형 필터(가변 picklist, useSearch=true 마스터명)와는 별개 — 집계표 컬럼은 항상 이 10개 고정.
 * backend 응답 `countsByCategory` 의 키가 곧 이 displayName 이므로, 컬럼 헤더는 반드시 이 순서/라벨을 그대로 써야 매칭된다.
 */
export const SUMMARY_CATEGORY_COLUMNS = [
  '대형마트',
  '농협',
  '체인',
  '슈퍼',
  '대리점',
  '백화점',
  '홀세일',
  '군납',
  '식자재',
  '기타',
] as const;

/**
 * 거래처유형 컬럼(displayName) → SF accountCode 집합 매핑.
 *
 * SF `SalesComparisonSearchController.getCategory` (cls:663-685) 의 코드→컬럼 분기를 역으로 정의.
 * 좌측 거래처유형 필터를 컬럼 헤더와 동일한 고정 10개로 노출하기 위해, 각 컬럼 선택 시 서버 `categoryCodes` 로
 * 보낼 코드 집합. '기타' 는 단일 코드가 없으므로 SF `getCategory` else 분기로 분류되는 13개 코드 전체를 전송한다
 * (SF '모든조건선택' typologyAllOptions 중 others 로 가는 04/11~14/16/19~25 정합).
 */
/**
 * 거래처유형 컬럼(displayName) → 화면 표시 라벨.
 *
 * SF `SalesComparisonListComponent1Helper.js` setColumns 의 헤더 라벨 정합 — 대형마트 컬럼만 "(3대)" 표기.
 * 데이터 키(countsByCategory)는 enum displayName 그대로이므로 표시 라벨만 별도 매핑한다.
 */
export function categoryColumnLabel(column: string): string {
  return column === '대형마트' ? '대형마트(3대)' : column;
}

export const CATEGORY_COLUMN_CODES: Record<string, string[]> = {
  대형마트: ['01'],
  농협: ['05'],
  체인: ['02'],
  슈퍼: ['06'],
  대리점: ['07'],
  백화점: ['03'],
  홀세일: ['08'],
  군납: ['15'],
  식자재: ['10'],
  기타: ['04', '11', '12', '13', '14', '16', '19', '20', '21', '22', '23', '24', '25'],
};

export interface SalesComparisonSummaryRow {
  suitability: string;
  totalCount: number;
  countsByCategory: Record<string, number>;
  accountIdsByCategory: Record<string, number[]>;
}

export interface SalesComparisonSummaryResponse {
  year: number;
  month: number;
  rows: SalesComparisonSummaryRow[];
  total: SalesComparisonSummaryRow;
}

export interface SalesComparisonMiddleItem {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountBranchName: string | null;
  accountCategory: string;
  suitability: string;
  avgClosingAmount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  fixedStandardAmount: number | null;
  bifurcationHalfStandardAmount: number | null;
  totalInputCount: number;
  totalEquivalentWorkingDays: number;
  thisMonthSalesAmount: number;
  ediPos: string | null;
}

export interface SalesComparisonMiddleSubtotal {
  suitability: string;
  accountCount: number;
  avgClosingAmount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  totalInputCount: number;
  totalEquivalentWorkingDays: number;
  thisMonthSalesAmount: number;
}

export interface SalesComparisonMiddleResponse {
  year: number;
  month: number;
  items: SalesComparisonMiddleItem[];
  subtotals: SalesComparisonMiddleSubtotal[];
  total: SalesComparisonMiddleSubtotal;
}

export interface SalesComparisonDetailItem {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountBranchName: string | null;
  accountCategory: string;
  accountCategoryCode: string | null;
  employeeCode: string;
  employeeName: string;
  title: string | null;
  workingCategory1: string;
  workingCategory3: string | null;
  workingCategory4: string | null;
  workingCategory5: string | null;
  suitability: string;
  avgClosingAmount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  fixedStandardAmount: number | null;
  bifurcationHalfStandardAmount: number | null;
  inputCount: number;
  equivalentWorkingDays: number;
  convertedHeadcount: number;
  thisMonthSalesAmount: number;
  ediPos: string | null;
}

export interface SalesComparisonDetailTotal {
  rowCount: number;
  totalDisplayHeadcount: number;
  totalDisplayConvertedHeadcount: number;
  totalEventConvertedHeadcount: number;
  totalInputCount: number;
  totalEquivalentWorkingDays: number;
  totalConvertedHeadcount: number;
  totalThisMonthSalesAmount: number;
}

export interface SalesComparisonDetailResponse {
  year: number;
  month: number;
  items: SalesComparisonDetailItem[];
  total: SalesComparisonDetailTotal;
}

const BASE = '/api/v1/admin/schedules/sales-comparison';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 집계 검색조건 필터 — SF `getSummaryItems` (cls:567-569) 3중 AND 필터 정합.
 * 빈 배열은 무필터(전체 통과). 거래처유형은 SF `typologyValues` 와 동일하게 **코드(accountCode)** 로 보낸다.
 */
export interface SummaryFilter {
  suitabilities: string[];
  categoryCodes: string[];
  workingCategory3: string[];
}

/** SummaryFilter → 쿼리 파라미터 (빈 배열 항목은 생략). */
function summaryFilterParams(filter?: SummaryFilter): Record<string, string> {
  if (!filter) return {};
  const params: Record<string, string> = {};
  if (filter.suitabilities.length > 0) params.suitabilities = filter.suitabilities.join(',');
  if (filter.categoryCodes.length > 0) params.categoryCodes = filter.categoryCodes.join(',');
  if (filter.workingCategory3.length > 0) params.workingCategory3 = filter.workingCategory3.join(',');
  return params;
}

export async function fetchSummary(
  year: number,
  month: number,
  costCenterCodes: string[],
  filter?: SummaryFilter,
): Promise<SalesComparisonSummaryResponse> {
  const res = await client.get<ApiResponse<SalesComparisonSummaryResponse>>(`${BASE}/summary`, {
    params: { year, month, costCenterCodes: costCenterCodes.join(','), ...summaryFilterParams(filter) },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('집계', res));
  return res.data.data;
}

export async function fetchMiddle(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
): Promise<SalesComparisonMiddleResponse> {
  const res = await client.get<ApiResponse<SalesComparisonMiddleResponse>>(`${BASE}/middle`, {
    params: {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('중간집계', res));
  return res.data.data;
}

export async function fetchDetail(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
  workingCategory1?: string,
  workingCategory5?: string,
): Promise<SalesComparisonDetailResponse> {
  const res = await client.get<ApiResponse<SalesComparisonDetailResponse>>(`${BASE}/detail`, {
    params: {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
      ...(workingCategory1 ? { workingCategory1 } : {}),
      ...(workingCategory5 ? { workingCategory5 } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('상세', res));
  return res.data.data;
}

/** 집계 엑셀 다운로드. */
export async function exportSummary(
  year: number,
  month: number,
  costCenterCodes: string[],
  filter?: SummaryFilter,
): Promise<void> {
  await downloadExcel(
    `${BASE}/summary/export`,
    `${year}년${month}월_월평균_매출대비_여사원배치_현황_집계.xlsx`,
    { params: { year, month, costCenterCodes: costCenterCodes.join(','), ...summaryFilterParams(filter) } },
  );
}

/** 중간집계 엑셀 다운로드. */
export async function exportMiddle(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
): Promise<void> {
  await downloadExcel(
    `${BASE}/middle/export`,
    `${year}년${month}월_월평균_매출대비_여사원배치_현황_중간집계.xlsx`,
    {
      params: {
        year,
        month,
        costCenterCodes: costCenterCodes.join(','),
        ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
      },
    },
  );
}

/** 상세 엑셀 다운로드. */
export async function exportDetail(
  year: number,
  month: number,
  costCenterCodes: string[],
  accountIds: number[],
  workingCategory1?: string,
  workingCategory5?: string,
): Promise<void> {
  await downloadExcel(
    `${BASE}/detail/export`,
    `${year}년${month}월_월평균_매출대비_여사원배치_현황_상세.xlsx`,
    {
      params: {
        year,
        month,
        costCenterCodes: costCenterCodes.join(','),
        ...(accountIds.length > 0 ? { accountIds: accountIds.join(',') } : {}),
        ...(workingCategory1 ? { workingCategory1 } : {}),
        ...(workingCategory5 ? { workingCategory5 } : {}),
      },
    },
  );
}
