import client from './client';
import type { ApiResponse } from './types';

/**
 * 투입현황 대시보드 (Spec 850) — Backend `DashboardResponse` 미러.
 *
 * 환산인원(convertedHeadcount 등)은 SF `ConvertedHeadcount__c`(Number 18,4) 정합으로
 * 소수 4자리 문자열/숫자로 직렬화된다. 차트는 환산인원 필드를 사용 (count 는 정수 인원).
 */

export interface ChannelSalesItem {
  channelName: string;
  targetAmount: number;
  actualAmount: number;
  progressRate: number;
}

export interface SalesSummary {
  yearMonth: string;
  branchName: string | null;
  targetAmount: number;
  actualAmount: number;
  progressRate: number;
  referenceProgressRate: number;
  lastYearAmount: number;
  lastYearRatio: number;
  channelSales: ChannelSalesItem[];
}

export interface AccountTypeCount {
  accountType: string;
  count: number;
  convertedHeadcount: number;
}

export interface WorkTypeCount {
  workType: string;
  count: number;
  convertedHeadcount: number;
}

export interface ChannelWorkTypeItem {
  channelName: string;
  fixed: number;
  alternating: number;
  visiting: number;
  fixedHeadcount: number;
  alternatingHeadcount: number;
  visitingHeadcount: number;
}

export interface PreviousMonthData {
  byWorkType: WorkTypeCount[];
}

export interface StaffDeployment {
  yearMonth: string;
  branchName: string | null;
  byAccountType: AccountTypeCount[];
  byWorkType: WorkTypeCount[];
  byChannelAndWorkType: ChannelWorkTypeItem[];
  previousMonth: PreviousMonthData;
}

export interface StaffTypeCount {
  promotion: number;
  osc: number;
}

export interface TotalByPosition {
  active: number;
  onLeave: number;
}

export interface AgeGroupCount {
  ageGroup: string;
  count: number;
}

export interface WorkTypeStats {
  fixed: number;
  alternating: number;
  visiting: number;
}

export interface BasicStats {
  branchName: string | null;
  staffType: StaffTypeCount;
  totalByPosition: TotalByPosition;
  byAgeGroup: AgeGroupCount[];
  byWorkType: WorkTypeStats;
}

export interface DashboardResponse {
  salesSummary: SalesSummary;
  staffDeployment: StaffDeployment;
  basicStats: BasicStats;
}

/**
 * 투입현황 대시보드 조회.
 *
 * @param yearMonth `yyyy-MM` (미지정 시 당월)
 * @param branchCode 지점 코드 (미지정 시 권한 스코프 전체)
 */
export async function fetchDashboard(
  yearMonth?: string,
  branchCode?: string,
): Promise<DashboardResponse> {
  const res = await client.get<ApiResponse<DashboardResponse>>('/api/v1/admin/dashboard', {
    params: {
      ...(yearMonth ? { yearMonth } : {}),
      ...(branchCode ? { branchCode } : {}),
    },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message ?? res.data.message ?? '대시보드 조회에 실패했습니다');
  }
  return res.data.data;
}
