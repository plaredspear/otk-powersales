import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 기간별 클레임 보고서 대상 분류 — PACKAGING(포장불량만)/ALL(모든 클레임). */
export type ClaimPeriodReportType = 'PACKAGING' | 'ALL';

/** 기간별 클레임 보고서 1행 (23컬럼) — SF Report X3_ONLY_veg/X4_3xv 이식 (Spec #843). */
export interface ClaimPeriodReportItem {
  claimName: string | null;
  interfaceDate: string | null;
  claimDate: string | null;
  claimType1: string | null;
  branchName: string | null;
  employeeCode: string | null;
  employeeName: string | null;
  mobilePhone: string | null;
  accountName: string | null;
  detailSnsName: string | null;
  externalKey: string | null;
  productName: string | null;
  productCode: string | null;
  manufacturingDate: string | null;
  expirationDate: string | null;
  quantity: number | null;
  claimType2: string | null;
  defectDescription: string | null;
  counselNumber: string | null;
  actionStatus: string | null;
  actionCode: string | null;
  reasonType: string | null;
  actContent: string | null;
}

export interface ClaimPeriodReportResponse {
  startDate: string;
  endDate: string;
  type: ClaimPeriodReportType;
  totalQuantity: number;
  items: ClaimPeriodReportItem[];
}

/** 지점 셀렉터 옵션 — 현재 사용자 권한별 조회 허용 지점 화이트리스트. */
export interface ClaimReportBranch {
  branchCode: string;
  branchName: string;
}

const BASE = '/api/v1/admin/claims/period-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 기간별 클레임 보고서 지점 셀렉터 옵션 조회.
 *
 * 전사 권한자는 전 지점, 그 외는 본인 지점 1건. 프론트는 응답 길이로 단일/다중을 판별한다.
 */
export async function fetchClaimReportBranches(): Promise<ClaimReportBranch[]> {
  const res = await client.get<ApiResponse<ClaimReportBranch[]>>(`${BASE}/branches`);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('지점 목록', res));
  return res.data.data;
}

/** 기간별 클레임 조회 (status='전송완료' + ClaimDate 기간 + type 분기). branchCode 지정 시 그 지점(사원 소속)으로 좁힘. */
export async function fetchClaimPeriodReport(
  startDate: string,
  endDate: string,
  type: ClaimPeriodReportType,
  branchCode?: string,
): Promise<ClaimPeriodReportResponse> {
  const res = await client.get<ApiResponse<ClaimPeriodReportResponse>>(BASE, {
    params: { startDate, endDate, type, branchCode: branchCode || undefined },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('기간별 클레임', res));
  return res.data.data;
}

/** 기간별 클레임 엑셀 다운로드. branchCode 지정 시 그 지점(사원 소속)으로 좁힘. */
export async function exportClaimPeriodReport(
  startDate: string,
  endDate: string,
  type: ClaimPeriodReportType,
  branchCode?: string,
): Promise<void> {
  const typeLabel = type === 'ALL' ? '모든클레임' : '포장불량';
  await downloadExcel(`${BASE}/export`, `기간별클레임_${typeLabel}_${startDate}_${endDate}.xlsx`, {
    params: { startDate, endDate, type, branchCode: branchCode || undefined },
  });
}
