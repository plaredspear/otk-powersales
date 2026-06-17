import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 여사원 배치 점검 1행 (21컬럼) — SF Report `new_report_4Ic` 이식 (Spec #839). */
export interface FemaleEmployeePlacementCheckItem {
  workingDate: string | null;
  orgName: string | null;
  employeeCode: string;
  jikwee: string | null;
  name: string;
  professionalPromotionTeam: string | null;
  employmentStatus: string | null;
  accountType: string | null;
  accountName: string | null;
  accountBranchCode: string | null;
  accountBranchName: string | null;
  workingCategory1: string | null;
  workingCategory2: string | null;
  workingCategory3: string | null;
  secondWorkType: string | null;
  workingCategory5: string | null;
  commuteDate: string | null;
  isWorkReport: string | null;
  startDate: string | null;
  age: number | null;
  yearsOfService: number | null;
}

export interface FemaleEmployeePlacementCheckResponse {
  year: number;
  month: number;
  items: FemaleEmployeePlacementCheckItem[];
}

const BASE = '/api/v1/admin/female-employees/placement-check';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 여사원 배치 점검 월간 조회. */
export async function fetchPlacementCheck(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<FemaleEmployeePlacementCheckResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeePlacementCheckResponse>>(BASE, {
    params: {
      year,
      month,
      ...(costCenterCodes.length > 0 ? { costCenterCodes: costCenterCodes.join(',') } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('여사원 배치 점검', res));
  return res.data.data;
}

/** 여사원 배치 점검 엑셀 다운로드. */
export async function exportPlacementCheck(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  await downloadExcel(`${BASE}/export`, `여사원배치점검_${year}${String(month).padStart(2, '0')}.xlsx`, {
    params: {
      year,
      month,
      ...(costCenterCodes.length > 0 ? { costCenterCodes: costCenterCodes.join(',') } : {}),
    },
  });
}
