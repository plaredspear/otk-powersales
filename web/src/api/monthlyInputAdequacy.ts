import client from './client';
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

export async function exportMatrix(
  year: number,
  costCenterCodes: string[],
  workingCategory3?: string,
): Promise<void> {
  const res = await client.get(`${BASE}/export`, {
    params: {
      year,
      costCenterCodes: costCenterCodes.join(','),
      ...(workingCategory3 ? { workingCategory3 } : {}),
    },
    responseType: 'blob',
  });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${year}년도_월별투입적정성.xlsx`;
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
