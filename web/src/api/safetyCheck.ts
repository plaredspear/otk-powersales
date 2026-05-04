import client from './client';
import type { ApiResponse } from './types';

// --- Raw interfaces (snake_case from backend) ---


export interface SafetyCheckStatusData {
  date: string;
  totalCount: number;
  submittedCount: number;
  notSubmittedCount: number;
  members: MemberStatus[];
}

export interface MemberStatus {
  id: number;
  employeeCode: string;
  employeeName: string;
  accountCode: string | null;
  accountName: string | null;
  submitted: boolean;
  submittedAt: string | null;
  startTime: string | null;
  equipments: EquipmentStatus[];
  yesCount: number;
  noCount: number;
  precautions: string | null;
  precautionCount: number;
  workReportStatus: string | null;
}

export interface EquipmentStatus {
  seqNum: number;
  label: string;
  answer: string;
}


// --- API functions ---

export async function fetchSafetyCheckStatus(date?: string): Promise<SafetyCheckStatusData> {
  const params = date ? { date } : undefined;
  const res = await client.get<ApiResponse<SafetyCheckStatusData>>(
    '/api/v1/admin/safety-check/status',
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '안전점검 현황 조회에 실패했습니다');
  }
  return res.data.data;
}
