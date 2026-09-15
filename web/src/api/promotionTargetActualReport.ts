import client from './client';
import type { ApiResponse } from './types';

/** 목표/실적 1행 (23컬럼) — SF Report new_report_AtQ 이식 (Spec #845). */
export interface PromotionTargetActualReportRow {
  promotionName: string | null;
  branchName: string | null;
  accountName: string | null;
  accountCode: string | null;
  primaryProductName: string | null;
  category1: string | null;
  otherProduct: string | null;
  employeeCode: string | null;
  employeeOrgName: string | null;
  employeeName: string | null;
  professionalPromotionTeamCurrent: string | null;
  professionalPromotionTeam: string | null;
  scheduleDate: string | null;
  targetAmount: number | null;
  actualAmount: number | null;
  standLocation: string | null;
  primarySalesQuantity: number | null;
  primaryProductAmount: number | null;
  otherSalesQuantity: number | null;
  otherSalesAmount: number | null;
  workType2: string | null;
  workType3: string | null;
  isWorkReport: string | null;
  commuteDate: string | null;
}

/** 행사명 그룹 — rows + 소계. */
export interface PromotionTargetActualReportGroup {
  promotionName: string | null;
  subtotalTargetAmount: number;
  subtotalActualAmount: number;
  subtotalPrimaryQuantity: number;
  subtotalPrimaryAmount: number;
  subtotalOtherQuantity: number;
  subtotalOtherAmount: number;
  rows: PromotionTargetActualReportRow[];
}

/** 도넛 차트 1항목 — 행사명별 실적금액. */
export interface PromotionTargetActualChartItem {
  promotionName: string | null;
  actualAmount: number;
}

export interface PromotionTargetActualReportResponse {
  startDate: string;
  endDate: string;
  groups: PromotionTargetActualReportGroup[];
  totalTargetAmount: number;
  totalActualAmount: number;
  totalPrimaryQuantity: number;
  totalPrimaryAmount: number;
  totalOtherQuantity: number;
  totalOtherAmount: number;
  chart: PromotionTargetActualChartItem[];
  /** 조회 조건에 걸린 전체 상세 행 수 (소계/합계/차트 산출 모수). */
  totalRowCount: number;
  /** 응답에 실제 포함된 상세 행 수 — 표시 상한(2,000행, SF 리포트 제한 정합) 적용 후. */
  displayedRowCount: number;
  /** 표시 상한으로 상세 행이 잘렸는지 여부 — true 면 전량은 엑셀 다운로드 안내. */
  truncated: boolean;
}

/** 지점 셀렉터 옵션 — 현재 사용자 권한별 조회 허용 지점 화이트리스트. */
export interface PromotionReportBranch {
  branchCode: string;
  branchName: string;
}

const BASE = '/api/v1/admin/promotions/target-actual-report';


function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 행사사원 목표 대비 실적 보고서 지점 셀렉터 옵션 조회.
 *
 * 전사 권한자는 전 지점, 그 외는 본인 지점 1건. 프론트는 응답 길이로 단일/다중을 판별한다.
 */
export async function fetchPromotionReportBranches(): Promise<PromotionReportBranch[]> {
  const res = await client.get<ApiResponse<PromotionReportBranch[]>>(`${BASE}/branches`);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('지점 목록', res));
  return res.data.data;
}

/** 행사사원 목표/실적 조회 (ScheduleDate 기간). branchCode 지정 시 그 지점(행사사원의 사원 마스터 소속)으로 좁힘. */
export async function fetchPromotionTargetActualReport(
  startDate: string,
  endDate: string,
  branchCode?: string,
): Promise<PromotionTargetActualReportResponse> {
  const res = await client.get<ApiResponse<PromotionTargetActualReportResponse>>(BASE, {
    params: { startDate, endDate, branchCode: branchCode || undefined },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('행사사원 목표 대비 실적', res));
  return res.data.data;
}

/**
 * 행사사원 목표/실적 엑셀 export 엔드포인트 — 페이지가 `useExcelDownload().run()` 으로 호출한다.
 *
 * 화면은 상위 일부 행만 표시하므로 전체 내역을 얻는 경로는 이 엑셀뿐이다 — 서버도 행 수 상한 없이 전량을 담는다.
 * (그래서 `run` 에 `maxRows` 를 넘기지 않는다 — 잘림 안내가 필요 없다.)
 */
export const PROMOTION_TARGET_ACTUAL_EXPORT_PATH = `${BASE}/export`;

/**
 * 엑셀 export 전용 타임아웃(ms) — 전량 추출이라 기간을 넓게 잡으면 공통 30초로는 부족할 수 있다.
 */
export const PROMOTION_TARGET_ACTUAL_EXPORT_TIMEOUT_MS = 300_000;
