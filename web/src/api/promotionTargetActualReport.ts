import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
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
  subtotalOtherQuantity: number;
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
  totalOtherQuantity: number;
  chart: PromotionTargetActualChartItem[];
}

const BASE = '/api/v1/admin/promotions/target-actual-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 행사사원 목표/실적 조회 (ScheduleDate 기간, 전사). */
export async function fetchPromotionTargetActualReport(
  startDate: string,
  endDate: string,
): Promise<PromotionTargetActualReportResponse> {
  const res = await client.get<ApiResponse<PromotionTargetActualReportResponse>>(BASE, {
    params: { startDate, endDate },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('행사사원 목표 대비 실적', res));
  return res.data.data;
}

/** 행사사원 목표/실적 엑셀 다운로드. */
export async function exportPromotionTargetActualReport(
  startDate: string,
  endDate: string,
): Promise<void> {
  await downloadExcel(`${BASE}/export`, `행사사원목표대비실적_${startDate}_${endDate}.xlsx`, {
    params: { startDate, endDate },
  });
}
