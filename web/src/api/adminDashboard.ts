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
  /** 집계 모수 — 해당 월 여사원 통합일정(출근등록)에 등장하는 투입 거래처 수 (distinct). */
  investedAccountCount: number;
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
  /** 당월 목표 등록 여부 — 투입 거래처 중 당월 목표가 전무하면 false. false 면 화면 "—" (계산은 0). */
  hasTargetData: boolean;
}

/** 거래처유형(유통) 1행 — headcounts 는 차트 stackKeys 와 동일 순서의 환산인원. */
export interface ChannelStackRow {
  channelName: string;
  headcounts: number[];
}

/**
 * 유통(거래처유형) × 근무형태 스택 누적 막대 1개 — SF 리포트 1개 대응.
 * stackKeys 는 스택 세그먼트 라벨 순서, 각 row.headcounts 가 같은 순서로 대응.
 */
export interface WorkTypeChannelChart {
  stackKeys: string[];
  rows: ChannelStackRow[];
  totalHeadcount: number;
}

/**
 * 여사원 투입현황 — SF 레거시 대시보드 리포트(`근무형태별(상세) 환산인원현황(진열)/(행사)`) 정합.
 * 근무유형1(진열/행사)별 가로 누적 막대 2개. 조회월의 전월(마감) 데이터 기준.
 * yearMonth 는 조회 조건 echo (데이터 기준월은 그 전월).
 */
export interface StaffDeployment {
  yearMonth: string;
  branchName: string | null;
  display: WorkTypeChannelChart;
  event: WorkTypeChannelChart;
}

/** "기타" 항목 세부 내역 1건 — 원본 값(label)과 인원 수(count). null/공백은 "미분류". */
export interface EtcBreakdownItem {
  label: string;
  count: number;
}

export interface StaffTypeCount {
  promotion: number;
  osc: number;
  /** 판촉직/OSC직 외 직군 또는 null — 모수 정합용 "기타". */
  etc: number;
  /** "기타" 구성 원본 jobCode 값별 세부 내역 (count 내림차순). */
  etcBreakdown: EtcBreakdownItem[];
}

export interface TotalByPosition {
  active: number;
  onLeave: number;
  /** 재직/휴직 외 상태(퇴직 등) 또는 null — 모수 정합용 "기타". */
  etc: number;
  /** "기타" 구성 원본 status 값별 세부 내역 (count 내림차순). */
  etcBreakdown: EtcBreakdownItem[];
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
 * @param branchCodes 지점 코드 목록 (비어 있으면 권한 스코프 전체). 다중 선택 시 여러 지점 합산 조회.
 */
export async function fetchDashboard(
  yearMonth?: string,
  branchCodes?: string[],
): Promise<DashboardResponse> {
  const codes = (branchCodes ?? []).filter(Boolean);
  const res = await client.get<ApiResponse<DashboardResponse>>('/api/v1/admin/dashboard', {
    params: {
      ...(yearMonth ? { yearMonth } : {}),
      // Spring `List<String>` 바인딩에 맞춰 반복 키(branchCode=A&branchCode=B)로 직렬화.
      ...(codes.length > 0 ? { branchCode: codes } : {}),
    },
    paramsSerializer: {
      serialize: (params) => {
        const search = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
          if (Array.isArray(value)) {
            value.forEach((v) => search.append(key, String(v)));
          } else if (value != null) {
            search.append(key, String(value));
          }
        });
        return search.toString();
      },
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
