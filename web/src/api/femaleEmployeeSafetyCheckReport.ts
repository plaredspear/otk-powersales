import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 판매여사원 일일 안전점검 현황 1행 (24컬럼) — SF Report new_report_wce/oJO 이식 (Spec #841). */
export interface FemaleEmployeeSafetyCheckReportItem {
  employeeCode: string;
  ladyName: string;
  employeeOrgName: string | null;
  accountType: string | null;
  accountBranchCode: string | null;
  accountName: string | null;
  workingCategory1: string | null;
  checkTime: string | null;
  isWorkReport: string | null;
  hrCode: string | null;
  equipment1: string | null;
  equipment2: string | null;
  equipment3: string | null;
  equipment4: string | null;
  equipment5: string | null;
  equipment6: string | null;
  equipment7: string | null;
  equipment8: string | null;
  equipment9: string | null;
  precaution: string | null;
  precautionChk: number | null;
  workingCategory2: string | null;
  workingCategory3: string | null;
  secondWorkType: string | null;
  commuteDate: string | null;
}

export interface FemaleEmployeeSafetyCheckReportResponse {
  date: string;
  items: FemaleEmployeeSafetyCheckReportItem[];
}

/** 지점 셀렉터 옵션 — 현재 사용자 권한별 조회 허용 지점 화이트리스트. */
export interface ReportBranch {
  branchCode: string;
  branchName: string;
}

const BASE = '/api/v1/admin/female-employees/safety-check-report';
/** 여사원 보고서 화면 공용 지점 셀렉터 옵션 엔드포인트 (안전점검·환산인원 공유, team_member_schedule READ 가드). */
const BRANCHES_BASE = '/api/v1/admin/female-employees/report-branches';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 여사원 보고서 화면 지점 셀렉터 옵션 조회.
 *
 * 전사 권한자는 전 지점, 그 외는 본인 지점 1건. 프론트는 응답 길이로 단일/다중을 판별해 UI 를 분기한다.
 */
export async function fetchReportBranches(): Promise<ReportBranch[]> {
  const res = await client.get<ApiResponse<ReportBranch[]>>(BRANCHES_BASE);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('지점 목록', res));
  return res.data.data;
}

/** 일일 안전점검 현황 조회 (date 미지정 시 어제). branchCode 지정 시 그 지점으로 좁힘. */
export async function fetchSafetyCheckReport(
  date: string,
  branchCode?: string,
): Promise<FemaleEmployeeSafetyCheckReportResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeeSafetyCheckReportResponse>>(BASE, {
    params: { date, branchCode: branchCode || undefined },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('판매여사원 안전점검 현황', res));
  return res.data.data;
}

/** 일일 안전점검 현황 엑셀 다운로드. branchCode 지정 시 그 지점으로 좁힘. */
export async function exportSafetyCheckReport(date: string, branchCode?: string): Promise<void> {
  await downloadExcel(`${BASE}/export`, `판매여사원안전점검_${date}.xlsx`, {
    params: { date, branchCode: branchCode || undefined },
  });
}
