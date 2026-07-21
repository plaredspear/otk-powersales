import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 판매여사원 일일 안전점검 현황 (RPA용) 1행 (24컬럼) — SF Report new_report_xdB 이식 (Spec #842). */
export interface FemaleEmployeeSafetyCheckRpaItem {
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
  scheduleName: string | null;
}

export interface FemaleEmployeeSafetyCheckRpaResponse {
  date: string;
  items: FemaleEmployeeSafetyCheckRpaItem[];
}

const BASE = '/api/v1/admin/female-employees/safety-check-report-rpa';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 일일 안전점검 현황 (RPA) 조회 (date 미지정 시 어제). branchCode 선택 시 그 지점(사원 소속)으로 좁힘. */
export async function fetchSafetyCheckReportRpa(
  date: string,
  branchCode?: string,
): Promise<FemaleEmployeeSafetyCheckRpaResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeeSafetyCheckRpaResponse>>(BASE, {
    params: { date, branchCode: branchCode || undefined },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('판매여사원 안전점검 현황 (RPA)', res));
  return res.data.data;
}

/** 일일 안전점검 현황 (RPA) 엑셀 다운로드. branchCode 지정 시 그 지점(사원 소속)으로 좁힘. */
export async function exportSafetyCheckReportRpa(date: string, branchCode?: string): Promise<void> {
  await downloadExcel(`${BASE}/export`, `판매여사원안전점검RPA_${date}.xlsx`, {
    params: { date, branchCode: branchCode || undefined },
  });
}
