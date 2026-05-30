import client from './client';
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

/** 전문행사조 확정 인원 조회 (isConfirmed=true, 전사). */
export async function fetchPptConfirmedReport(): Promise<PptConfirmedReportResponse> {
  const res = await client.get<ApiResponse<PptConfirmedReportResponse>>(BASE);
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('전문행사조 확정 인원', res));
  return res.data.data;
}

/** 전문행사조 확정 인원 엑셀 다운로드. */
export async function exportPptConfirmedReport(): Promise<void> {
  const res = await client.get(`${BASE}/export`, { responseType: 'blob' });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = '전문행사조확정인원.xlsx';
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
