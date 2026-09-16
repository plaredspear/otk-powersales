import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/** 여사원 근무내역 1행 (15컬럼) — SF Report `new_report_nEX` 이식 (Spec #840). */
export interface FemaleEmployeeWorkHistoryItem {
  scheduleName: string | null;
  name: string;
  employeeCode: string;
  /** SF Age__c formula 정합 — "N살" 문자열, 기준 TODAY, 여사원만 (조장 등은 null) */
  age: string | null;
  workingDate: string | null;
  accountBranchName: string | null;
  /** SAP거래처코드 (레거시 `AccCode__c`) — 화면 라벨은 "거래처코드" */
  accountSapCode: string | null;
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
  /** 조회 시작일 (YYYY-MM-DD, 요청 에코) */
  startDate: string;
  /** 조회 종료일 (YYYY-MM-DD, 요청 에코) */
  endDate: string;
  items: FemaleEmployeeWorkHistoryItem[];
}

const BASE = '/api/v1/admin/female-employees/work-history';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 여사원 개인별 근무내역 기간(시작일~종료일) 조회. 서버 상한 366일.
 * costCenterCodes 지정 시 그 지점(사원 소속)으로 좁힘.
 */
export async function fetchWorkHistory(
  employeeCode: string,
  startDate: string,
  endDate: string,
  costCenterCodes: string[],
): Promise<FemaleEmployeeWorkHistoryResponse> {
  const res = await client.get<ApiResponse<FemaleEmployeeWorkHistoryResponse>>(BASE, {
    params: {
      employeeCode,
      startDate,
      endDate,
      ...(costCenterCodes.length > 0 ? { costCenterCodes: costCenterCodes.join(',') } : {}),
    },
  });
  if (!res.data.success || !res.data.data) throw new Error(failureMessage('여사원 근무내역', res));
  return res.data.data;
}

/** 여사원 근무내역 엑셀 다운로드. costCenterCodes 지정 시 그 지점(사원 소속)으로 좁힘. */
export async function exportWorkHistory(
  employeeCode: string,
  startDate: string,
  endDate: string,
  costCenterCodes: string[],
): Promise<void> {
  const compact = (d: string) => d.replace(/-/g, '');
  await downloadExcel(
    `${BASE}/export`,
    `여사원근무내역_${employeeCode}_${compact(startDate)}_${compact(endDate)}.xlsx`,
    {
      params: {
        employeeCode,
        startDate,
        endDate,
        ...(costCenterCodes.length > 0 ? { costCenterCodes: costCenterCodes.join(',') } : {}),
      },
    },
  );
}
