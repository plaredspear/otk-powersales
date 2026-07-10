import client from './client';
import type { ApiResponse } from './types';

/**
 * 월매출 대시보드 — 상단 KPI + 월별 추이 응답.
 */
export interface MonthlyTrendPoint {
  salesYear: number;
  salesMonth: number;
  targetAmount: number;
  achievedAmount: number;
  lastYearAchievedAmount: number | null;
}

export interface MonthlySalesDashboardSummary {
  salesYear: number;
  salesMonth: number;
  totalTargetAmount: number;
  totalAchievedAmount: number;
  overallAchievementRate: number;
  referenceAchievementRate: number;
  totalLastYearAchievedAmount: number | null;
  lastYearComparisonRatio: number | null;
  monthlyTrend: MonthlyTrendPoint[];
}

export interface MonthlySalesDashboardListItem {
  accountId: number;
  accountName: string | null;
  /** 유통형태 — 거래처상태코드 + 거래처유형(Type) 조합 (예: "02 슈퍼"). 화면 미표시, 엑셀 다운로드 전용. */
  distributionChannelLabel: string | null;
  /** 거래처유형 — ABC유형코드 + ABC유형 조합 (예: "6111 이마트"). 화면 미표시, 엑셀 다운로드 전용. */
  abcTypeLabel: string | null;
  sapAccountCode: string | null;
  branchCode: string | null;
  branchName: string | null;
  salesYear: number;
  salesMonth: number;
  targetAmount: number | null;
  totalAchievedAmount: number | null;
  achievementRate: number | null;
  ambientTargetAmount: number | null;
  ambientAchievedAmount: number | null;
  noodleTargetAmount: number | null;
  noodleAchievedAmount: number | null;
  frozenRefrigeratedTargetAmount: number | null;
  frozenRefrigeratedAchievedAmount: number | null;
  oilFatTargetAmount: number | null;
  oilFatAchievedAmount: number | null;
  lastYearAchievedAmount: number | null;
  lastYearComparisonRatio: number | null;
  isConfirmed: boolean;
  /** 진열 환산인원 (월 통합일정 기준, 상시/임시·위탁 무필터). 투입 없으면 0. */
  displayHeadcount: number;
  /** 행사 환산인원. 투입 없으면 0. */
  eventHeadcount: number;
  /** 총 환산인원 = 진열 + 행사. */
  totalHeadcount: number;
}

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MonthlySalesDashboardListResponse {
  items: MonthlySalesDashboardListItem[];
  pageInfo: PageInfo;
}

export interface MonthlySalesCategorySales {
  category: string;
  targetAmount: number;
  achievedAmount: number;
  achievementRate: number;
}

export interface MonthlySalesYearComparison {
  currentYear: number;
  previousYear: number;
}

export interface MonthlySalesMonthlyAverage {
  currentYearAverage: number;
  previousYearAverage: number;
  startMonth: number;
  endMonth: number;
}

export interface MonthlySalesDashboardDetail {
  customerId: number;
  customerName: string | null;
  salesYear: number;
  salesMonth: number;
  targetAmount: number;
  achievedAmount: number;
  achievementRate: number;
  referenceAchievementRate: number;
  categorySales: MonthlySalesCategorySales[];
  yearComparison: MonthlySalesYearComparison;
  monthlyAverage: MonthlySalesMonthlyAverage;
}

export interface MonthlySalesDashboardListRequest {
  year: number;
  month: number;
  costCenterCodes: string[];
  accountIds?: number[];
  accountGroup?: string;
  /** 거래처명 OR 거래처코드(externalKey) 통합 부분일치 (행사마스터 정합). */
  customerKeyword?: string;
  /** 유통형태 라벨 (예 "01 대형마트(3대)") 다중 정확일치 — POS매출 정합. */
  distributionChannels?: string[];
  /** 거래처유형(ABC유형) 라벨 (예 "6111 이마트") 다중 정확일치 — POS매출 정합. */
  accountTypes?: string[];
  /** 목표등록 구분 — 'registered'(목표 row 존재) | 'unregistered'(미존재) | undefined(전체). */
  targetRegistration?: 'registered' | 'unregistered';
  /** 근무등록 구분 — 'deployed'(여사원 근무등록 거래처) | 'undeployed'(미등록) | undefined(전체). */
  deploymentFilter?: 'deployed' | 'undeployed';
  page?: number;
  size?: number;
  sort?: string;
}

const BASE = '/api/v1/admin/sales/monthly';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 상단 KPI + 월별 추이 조회.
 */
export async function fetchSummary(
  year: number,
  month: number,
  costCenterCodes: string[],
  customerKeyword?: string,
  accountGroup?: string,
  distributionChannels?: string[],
  accountTypes?: string[],
  targetRegistration?: 'registered' | 'unregistered',
  deploymentFilter?: 'deployed' | 'undeployed',
): Promise<MonthlySalesDashboardSummary> {
  const res = await client.get<ApiResponse<MonthlySalesDashboardSummary>>(`${BASE}/summary`, {
    params: {
      year,
      month,
      costCenterCodes: costCenterCodes.join(','),
      ...(customerKeyword ? { customerKeyword } : {}),
      ...(accountGroup ? { accountGroup } : {}),
      ...(distributionChannels && distributionChannels.length > 0
        ? { distributionChannels: distributionChannels.join(',') }
        : {}),
      ...(accountTypes && accountTypes.length > 0
        ? { accountTypes: accountTypes.join(',') }
        : {}),
      ...(targetRegistration ? { targetRegistration } : {}),
      ...(deploymentFilter ? { deploymentFilter } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월매출 요약', res));
  return res.data.data;
}

/**
 * 거래처별 명세 — 페이징 + 정렬 + 필터.
 */
export async function fetchList(
  request: MonthlySalesDashboardListRequest,
): Promise<MonthlySalesDashboardListResponse> {
  const res = await client.get<ApiResponse<MonthlySalesDashboardListResponse>>(`${BASE}/list`, {
    params: {
      year: request.year,
      month: request.month,
      costCenterCodes: request.costCenterCodes.join(','),
      ...(request.accountIds && request.accountIds.length > 0
        ? { accountIds: request.accountIds.join(',') }
        : {}),
      ...(request.accountGroup ? { accountGroup: request.accountGroup } : {}),
      ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
      ...(request.distributionChannels && request.distributionChannels.length > 0
        ? { distributionChannels: request.distributionChannels.join(',') }
        : {}),
      ...(request.accountTypes && request.accountTypes.length > 0
        ? { accountTypes: request.accountTypes.join(',') }
        : {}),
      ...(request.targetRegistration ? { targetRegistration: request.targetRegistration } : {}),
      ...(request.deploymentFilter ? { deploymentFilter: request.deploymentFilter } : {}),
      ...(request.page !== undefined ? { page: request.page } : {}),
      ...(request.size !== undefined ? { size: request.size } : {}),
      ...(request.sort ? { sort: request.sort } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월매출 명세', res));
  return res.data.data;
}

/** 거래처별 명세 엑셀 export 파라미터 (페이징 제외). */
export function exportListParams(request: MonthlySalesDashboardListRequest): Record<string, string> {
  return {
    year: String(request.year),
    month: String(request.month),
    costCenterCodes: request.costCenterCodes.join(','),
    ...(request.accountIds && request.accountIds.length > 0
      ? { accountIds: request.accountIds.join(',') }
      : {}),
    ...(request.accountGroup ? { accountGroup: request.accountGroup } : {}),
    ...(request.customerKeyword ? { customerKeyword: request.customerKeyword } : {}),
    ...(request.distributionChannels && request.distributionChannels.length > 0
      ? { distributionChannels: request.distributionChannels.join(',') }
      : {}),
    ...(request.accountTypes && request.accountTypes.length > 0
      ? { accountTypes: request.accountTypes.join(',') }
      : {}),
    ...(request.targetRegistration ? { targetRegistration: request.targetRegistration } : {}),
    ...(request.deploymentFilter ? { deploymentFilter: request.deploymentFilter } : {}),
    ...(request.sort ? { sort: request.sort } : {}),
  };
}

/** 거래처별 명세 엑셀 다운로드 경로. */
export const EXPORT_LIST_PATH = `${BASE}/list/export`;

/**
 * 단건 거래처 상세 (모바일 동등 6 영역).
 */
export async function fetchDetail(
  customerId: number,
  year: number,
  month: number,
): Promise<MonthlySalesDashboardDetail> {
  const res = await client.get<ApiResponse<MonthlySalesDashboardDetail>>(
    `${BASE}/detail/${customerId}`,
    { params: { year, month } },
  );
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월매출 상세', res));
  return res.data.data;
}
