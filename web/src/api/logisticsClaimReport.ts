import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 물류 클레임 보고서 기간 프리셋. */
export type LogisticsClaimReportPeriod = 'THIS_MONTH' | 'LAST_MONTH' | 'CUSTOM';

/** (영업본부) 물류 클레임 보고서 1행 (22컬럼) — SF Report OLS_dmK/new_report_6dy/OLS_NDx 이식 (Spec #844). */
export interface LogisticsClaimReportItem {
  custName: string | null;
  createdDate: string | null;
  claimDate: string | null;
  responsibleLogisticsCenter: string | null;
  logisticsResponsibility: string | null;
  claimType: string | null;
  title: string | null;
  content: string | null;
  productCode: string | null;
  productName: string | null;
  productCategory: string | null;
  branchName: string | null;
  accountCode: string | null;
  accountName: string | null;
  orgName: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  jikwee: string | null;
  jobCode: string | null;
  carNumber: string | null;
  actionStatus: string | null;
  actionContent: string | null;
  duplicateProposalNum: string | null;
}

export interface LogisticsClaimReportResponse {
  period: LogisticsClaimReportPeriod;
  startDate: string;
  endDate: string;
  items: LogisticsClaimReportItem[];
}

/** 지점 셀렉터 옵션 — 현재 사용자 권한별 조회 허용 지점 화이트리스트. */
export interface LogisticsClaimReportBranch {
  branchCode: string;
  branchName: string;
}

const BASE = '/api/v1/admin/suggestions/logistics-claim-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 물류 클레임 보고서 지점 셀렉터 옵션 조회.
 *
 * 전사 권한자는 전 지점, 그 외는 본인 지점 1건. 프론트는 응답 길이로 단일/다중을 판별한다.
 */
export async function fetchLogisticsClaimReportBranches(): Promise<LogisticsClaimReportBranch[]> {
  const res = await client.get<ApiResponse<LogisticsClaimReportBranch[]>>(`${BASE}/branches`);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('지점 목록', res));
  return res.data.data;
}

/** 물류 클레임 조회 (category='물류 클레임' + period 기간). CUSTOM 이면 start/end 필요. branchCode 지정 시 그 지점(등록 사원 소속)으로 좁힘. */
export async function fetchLogisticsClaimReport(
  period: LogisticsClaimReportPeriod,
  startDate?: string,
  endDate?: string,
  branchCode?: string,
): Promise<LogisticsClaimReportResponse> {
  const res = await client.get<ApiResponse<LogisticsClaimReportResponse>>(BASE, {
    params: { period, startDate, endDate, branchCode: branchCode || undefined },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('물류 클레임', res));
  return res.data.data;
}

/** 물류 클레임 엑셀 다운로드. branchCode 지정 시 그 지점(등록 사원 소속)으로 좁힘. */
export async function exportLogisticsClaimReport(
  period: LogisticsClaimReportPeriod,
  startDate?: string,
  endDate?: string,
  branchCode?: string,
): Promise<void> {
  await downloadExcel(`${BASE}/export`, `물류클레임보고서_${period}.xlsx`, {
    params: { period, startDate, endDate, branchCode: branchCode || undefined },
  });
}
