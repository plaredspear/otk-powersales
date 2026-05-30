import client from './client';
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

const BASE = '/api/v1/admin/female-employees/safety-check-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 일일 안전점검 현황 조회 (date 미지정 시 어제). */
export async function fetchSafetyCheckReport(
  date: string,
): Promise<FemaleEmployeeSafetyCheckReportResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeeSafetyCheckReportResponse>>(BASE, {
    params: { date },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('판매여사원 안전점검 현황', res));
  return res.data.data;
}

/** 일일 안전점검 현황 엑셀 다운로드. */
export async function exportSafetyCheckReport(date: string): Promise<void> {
  const res = await client.get(`${BASE}/export`, {
    params: { date },
    responseType: 'blob',
  });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `판매여사원안전점검_${date}.xlsx`;
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
