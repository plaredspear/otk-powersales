import client from './client';
import type { ApiResponse } from './types';

/**
 * 거래처유형별 환산인원 현황 보고서 variant — SF Report 5변형 (Spec #847).
 * 백엔드 ConvertedHeadcountReportVariant enum 값과 1:1.
 */
export type ConvertedHeadcountReportVariant =
  | 'PERMANENT_TEMP_ALL' // 1-1 상시,임시 전체
  | 'PERMANENT_ONLY_EXCL_CONSIGN' // 1-2 상시 (위탁농협 제외)
  | 'TEMP_ALL' // 1-4 임시 전체
  | 'TEMP_ONLY_EXCL_CONSIGN' // 1-5 임시 전체 (위탁농협 제외)
  | 'TEAM2_PERMANENT_TEMP_ALL'; // (2팀)2-1 상시,임시 전체

/** 환산인원 집계 1행 — 구분 × 근무유형1 × 지점 × 연월 × SUM(환산인원). */
export interface ConvertedHeadcountReportRow {
  accountType: string | null;
  workingCategory1: string | null;
  branchName: string | null;
  yearMonth: string | null;
  convertedHeadcount: number;
}

/** 구분(거래처유형) 그룹 — 소계 보유. */
export interface ConvertedHeadcountReportGroup {
  accountType: string;
  subtotalHeadcount: number;
  rows: ConvertedHeadcountReportRow[];
}

export interface ConvertedHeadcountReportResult {
  variant: string;
  year: string;
  month: string;
  groups: ConvertedHeadcountReportGroup[];
  totalHeadcount: number;
}

const BASE = '/api/v1/admin/female-employees/converted-headcount-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/** 거래처유형별 환산인원 현황 조회. */
export async function fetchConvertedHeadcountReport(
  variant: ConvertedHeadcountReportVariant,
  year: string,
  month: string,
): Promise<ConvertedHeadcountReportResult> {
  const res = await client.get<ApiResponse<ConvertedHeadcountReportResult>>(`${BASE}/${variant}`, {
    params: { year, month },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(failureMessage('거래처유형별 환산인원 현황', res));
  }
  return res.data.data;
}

/** 거래처유형별 환산인원 현황 엑셀 다운로드. */
export async function exportConvertedHeadcountReport(
  variant: ConvertedHeadcountReportVariant,
  year: string,
  month: string,
): Promise<void> {
  const res = await client.get(`${BASE}/${variant}/export`, {
    params: { year, month },
    responseType: 'blob',
  });
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `거래처유형별환산인원_${year}-${month}.xlsx`;
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
