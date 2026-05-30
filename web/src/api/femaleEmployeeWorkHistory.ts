import client from './client';
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

/** 여사원 개인별 근무내역 월간 조회. */
export async function fetchWorkHistory(
  employeeCode: string,
  year: number,
  month: number,
): Promise<FemaleEmployeeWorkHistoryResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeeWorkHistoryResponse>>(BASE, {
    params: { employeeCode, year, month },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('여사원 근무내역', res));
  return res.data.data;
}

/** 여사원 근무내역 엑셀 다운로드. */
export async function exportWorkHistory(
  employeeCode: string,
  year: number,
  month: number,
): Promise<void> {
  const res = await client.get(`${BASE}/export`, {
    params: { employeeCode, year, month },
    responseType: 'blob',
  });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `여사원근무내역_${employeeCode}_${year}${String(month).padStart(2, '0')}.xlsx`;
  if (contentDisposition) {
    const utfMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
    if (utfMatch) {
      filename = decodeURIComponent(utfMatch[1]);
    } else {
      const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
      if (match) filename = decodeURIComponent(match[1]);
    }
  }
  const blob = new Blob([res.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
