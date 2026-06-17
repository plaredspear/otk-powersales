import client from './client';
import type { ApiResponse } from './types';
import type { Branch } from './team-schedule';

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
  /** 매출 데이터 적재 여부 — 0원이 "미적재"인지 "실제 0"인지 구분. false 면 화면에서 "—" 표시. */
  hasActualData: boolean;
  hasLastYearData: boolean;
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
  /** 판촉직/OSC직 외 직군 또는 null — 모수 정합용 "기타". */
  etc: number;
}

export interface TotalByPosition {
  active: number;
  onLeave: number;
  /** 재직/휴직 외 상태(퇴직 등) 또는 null — 모수 정합용 "기타". */
  etc: number;
}

export interface AgeGroupCount {
  ageGroup: string;
  count: number;
}

/** 근무형태(고정/격고/순회)별 환산인원 SUM — MFEIS ConvertedHeadcount__c 정합 (소수 가능). */
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

/**
 * 대시보드 지점 셀렉터 옵션 조회.
 *
 * 여사원일정 `/team-schedule/branches` 와 동일 산출 로직이나 권한 가드 없는 대시보드 전용 endpoint.
 * (대시보드는 인증된 모든 admin 사용자에게 열려있어 `team_member_schedule:R` 미보유자도 접근 가능)
 */
export async function fetchDashboardBranches(): Promise<Branch[]> {
  const res = await client.get<ApiResponse<Branch[]>>('/api/v1/admin/dashboard/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message ?? res.data.message ?? '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
