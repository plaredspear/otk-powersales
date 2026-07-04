import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 여사원 근무내역 1행 (15컬럼) — SF Report `new_report_nEX` 이식 (Spec #840). */
export interface FemaleEmployeeWorkHistoryItem {
  scheduleName: string | null;
  name: string;
  employeeCode: string;
  age: number | null;
  workingDate: string | null;
  accountBranchName: string | null;
  accountBranchCode: string | null;
  accountName: string | null;
  workingType: string | null;
  workingCategory1: string | null;
  workingCategory2: string | null;
  workingCategory3: string | null;
  secondWorkType: string | null;
  isWorkReport: string | null;
  commuteDate: string | null;
}

export interface FemaleEmployeeWorkHistoryResponse {
  employeeCode: string;
  year: number;
  month: number;
  items: FemaleEmployeeWorkHistoryItem[];
}

const BASE = '/api/v1/admin/female-employees/work-history';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 여사원 개인별 근무내역 월간 조회. costCenterCodes 지정 시 그 지점(사원 소속)으로 좁힘. */
export async function fetchWorkHistory(
  employeeCode: string,
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<FemaleEmployeeWorkHistoryResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeeWorkHistoryResponse>>(BASE, {
    params: {
      employeeCode,
      year,
      month,
      ...(costCenterCodes.length > 0 ? { costCenterCodes: costCenterCodes.join(',') } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('여사원 근무내역', res));
  return res.data.data;
}

/** 여사원 근무내역 엑셀 다운로드. costCenterCodes 지정 시 그 지점(사원 소속)으로 좁힘. */
export async function exportWorkHistory(
  employeeCode: string,
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  await downloadExcel(
    `${BASE}/export`,
    `여사원근무내역_${employeeCode}_${year}${String(month).padStart(2, '0')}.xlsx`,
    {
      params: {
        employeeCode,
        year,
        month,
        ...(costCenterCodes.length > 0 ? { costCenterCodes: costCenterCodes.join(',') } : {}),
      },
    },
  );
}
