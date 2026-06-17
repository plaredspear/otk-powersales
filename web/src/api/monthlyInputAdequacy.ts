import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

export interface MonthlyInputAdequacyItem {
  branchName: string;
  workingCategory3: string | null;
  employeeName: string;
  employeeCode: string;
  title: string | null;
  accountCategory: string;
  accountCategoryCode: string | null;
  accountName: string;
  accountCode: string;
  monthlySuitability: string[];
}

export interface MonthlyInputAdequacyResponse {
  year: number;
  items: MonthlyInputAdequacyItem[];
}

const BASE = '/api/v1/admin/schedules/monthly-input-adequacy';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

export async function fetchMatrix(
  year: number,
  costCenterCodes: string[],
  workingCategory3?: string,
): Promise<MonthlyInputAdequacyResponse> {
  const res = await client.get<ApiResponse<MonthlyInputAdequacyResponse>>(BASE, {
    params: {
      year,
      costCenterCodes: costCenterCodes.join(','),
      ...(workingCategory3 ? { workingCategory3 } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('월별 투입적정성', res));
  return res.data.data;
}

/** 월별 진열사원 투입적정성 매트릭스 엑셀 다운로드. */
export async function exportMatrix(
  year: number,
  costCenterCodes: string[],
  workingCategory3?: string,
): Promise<void> {
  await downloadExcel(`${BASE}/export`, `${year}년도_월별투입적정성.xlsx`, {
    params: {
      year,
      costCenterCodes: costCenterCodes.join(','),
      ...(workingCategory3 ? { workingCategory3 } : {}),
    },
  });
}
