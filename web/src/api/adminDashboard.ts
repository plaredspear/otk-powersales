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

/** 거래처유형(유통) 1행 — headcounts 는 차트 stackKeys 와 동일 순서의 환산인원. */
export interface ChannelStackRow {
  channelName: string;
  headcounts: number[];
}

/**
 * 유통(거래처유형) × 스택 누적 가로막대 1개 — SF 리포트 1개 대응.
 * stackKeys 는 스택 세그먼트 라벨 순서, 각 row.headcounts 가 같은 순서로 대응.
 */
export interface WorkTypeChannelChart {
  stackKeys: string[];
  rows: ChannelStackRow[];
  totalHeadcount: number;
}

/**
 * 여사원 투입현황 — SF 레거시 조장 대시보드 "투입현황" 6개 차트 정합.
 * 모두 조회월의 전월(마감) MFEIS 전건을 서로 다르게 집계. yearMonth 는 조회 조건 echo.
 *
 * - byAccountType: ① 거래처유형별 투입현황 (단일 가로막대)
 * - channelWorkType1: ② 근무형태별/유통별 인원현황 (유통 × 진열/행사 누적)
 * - workType1Ratio: ③ 근무형태 비중 (진열/행사 도넛)
 * - all: ④ 유통별/근무형태별(All) (유통 × 근무형태3&4 누적)
 * - display: ⑤ 유통별/근무형태별(진열)
 * - event: ⑥ 유통별/근무형태별(행사)
 */
export interface StaffDeployment {
  yearMonth: string;
  branchName: string | null;
  byAccountType: AccountTypeCount[];
  channelWorkType1: WorkTypeChannelChart;
  workType1Ratio: WorkTypeCount[];
  all: WorkTypeChannelChart;
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

/**
 * 기본 현황 — 사원 마스터의 현재 상태 스냅샷 집계. 조회월과 무관하다
 * (화면은 이 탭에서 조회월 셀렉터를 잠근다).
 *
 * 과거에는 선택월 MFEIS 환산인원 기준의 `byWorkType`(근무형태별 고정/격고/순회) 을 함께 받았으나,
 * 같은 탭 안에서 기준 시점이 섞이는 혼선 때문에 제거했다 — 근무형태별 환산인원은 여사원 투입현황 탭 담당.
 */
/** 직급별 인원현황 셀 1개 — 직위명(label)과 인원 수(count). */
export interface RankCount {
  label: string;
  count: number;
}

/**
 * 직급별 인원현황 1개 그룹 (표의 1단 헤더 = 판매조장 / 판촉직 / OSC직).
 *
 * `ranks` 구성이 그룹마다 다르다 — 판매조장은 해당 지점에 실제 존재하는 직위를 동적 생성하고
 * (지점에 따라 '주임' / 'OSPM' 등), 판촉직·OSC직은 OSPM/OSPE/OSPJ/OSC 를 고정 노출한 뒤
 * 그 외 값을 '기타' 로 합산한다. 인원 0인 그룹은 응답에서 제외된다.
 */
export interface RankGroupCount {
  group: string;
  ranks: RankCount[];
}

/**
 * 집계 기준(재직 / 재직+휴직) 하나에 대한 기본 현황 수치 묶음.
 * 화면의 '집계 기준' 토글이 둘 중 하나를 골라 렌더링한다 — 두 기준을 모두 받으므로 재조회가 없다.
 */
export interface BasicStatsByScope {
  staffType: StaffTypeCount;
  byAgeGroup: AgeGroupCount[];
  /**
   * 평균 만나이 (소수 1자리). 생년월일이 없는 사원("미상")은 모수에서 제외한다.
   * 산출 가능한 사원이 없으면 null — 화면은 표기를 생략한다.
   */
  averageAge: number | null;
  byRank: RankGroupCount[];
}

export interface BasicStats {
  branchName: string | null;
  /** 재직자만. */
  active: BasicStatsByScope;
  /** 재직 + 휴직 (퇴직만 제외, 토글 기본값). */
  includingLeave: BasicStatsByScope;
  /** 재직/휴직 비율 — 토글과 무관하게 항상 전체 기준 (좁히면 휴직 세그먼트가 0이 되어 무의미). */
  totalByPosition: TotalByPosition;
  /** 인원 기준일 (YYYY-MM-DD) — 서버 KST 기준 전일. 각 차트 우측 하단에 표기한다. */
  asOfDate: string;
}

export interface DashboardResponse {
  salesSummary: SalesSummary;
  staffDeployment: StaffDeployment;
  basicStats: BasicStats;
  /**
   * 이번 조회에 적용된 지점 스코프 방식 (개발자 도구 > 대시보드 > 지점 스코프 방식 토글의 현재 값).
   * 신/구 방식 수치를 비교하는 동안 화면이 어느 방식의 숫자인지 표시하기 위한 한시 필드 —
   * 비교 종료 후 토글과 함께 제거한다.
   */
  branchScopeMode: 'UNIFIED' | 'LEGACY';
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
