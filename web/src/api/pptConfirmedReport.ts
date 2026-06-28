import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 전문행사조 확정 인원 1행 (6컬럼) — SF Report new_report_swJ 이식 (Spec #846). */
export interface PptConfirmedReportItem {
  branchName: string | null;
  fullName: string | null;
  employeeNumber: string | null;
  accountName: string | null;
  accountCode: string | null;
  professionalPromotionTeam: string | null;
}

export interface PptConfirmedReportResponse {
  items: PptConfirmedReportItem[];
}

const BASE = '/api/v1/admin/ppt-masters/confirmed-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 전문행사조 확정 인원 조회 (isConfirmed=true). `branchCode` 지정 시 해당 지점만. */
export async function fetchPptConfirmedReport(
  branchCode?: string,
): Promise<PptConfirmedReportResponse> {
  const res = await client.get<ApiResponse<PptConfirmedReportResponse>>(BASE, {
    params: branchCode ? { branchCode } : undefined,
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('전문행사조 확정 인원', res));
  return res.data.data;
}

/** 전문행사조 확정 인원 엑셀 다운로드. `branchCode` 지정 시 해당 지점만. */
export async function exportPptConfirmedReport(branchCode?: string): Promise<void> {
  await downloadExcel(`${BASE}/export`, '전문행사조확정인원.xlsx', {
    params: branchCode ? { branchCode } : undefined,
  });
}
