import client from './client';

// --- Raw interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface SafetyCheckStatusRaw {
  date: string;
  total_count: number;
  submitted_count: number;
  not_submitted_count: number;
  members: MemberStatusRaw[];
}

interface MemberStatusRaw {
  id: number;
  employee_code: string;
  employee_name: string;
  account_code: string | null;
  account_name: string | null;
  submitted: boolean;
  submitted_at: string | null;
  start_time: string | null;
  equipments: EquipmentStatusRaw[];
  yes_count: number;
  no_count: number;
  precautions: string | null;
  precaution_count: number;
  work_report_status: string | null;
}

interface EquipmentStatusRaw {
  seq_num: number;
  label: string;
  answer: string;
}

// --- Frontend interfaces (camelCase) ---

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

// --- Mapper ---

function mapEquipment(raw: EquipmentStatusRaw): EquipmentStatus {
  return {
    seqNum: raw.seq_num,
    label: raw.label,
    answer: raw.answer,
  };
}

function mapMember(raw: MemberStatusRaw): MemberStatus {
  return {
    id: raw.id,
    employeeCode: raw.employee_code,
    employeeName: raw.employee_name,
    accountCode: raw.account_code,
    accountName: raw.account_name,
    submitted: raw.submitted,
    submittedAt: raw.submitted_at,
    startTime: raw.start_time,
    equipments: raw.equipments.map(mapEquipment),
    yesCount: raw.yes_count,
    noCount: raw.no_count,
    precautions: raw.precautions,
    precautionCount: raw.precaution_count,
    workReportStatus: raw.work_report_status,
  };
}

function mapSafetyCheckStatus(raw: SafetyCheckStatusRaw): SafetyCheckStatusData {
  return {
    date: raw.date,
    totalCount: raw.total_count,
    submittedCount: raw.submitted_count,
    notSubmittedCount: raw.not_submitted_count,
    members: raw.members.map(mapMember),
  };
}

// --- API functions ---

export async function fetchSafetyCheckStatus(date?: string): Promise<SafetyCheckStatusData> {
  const params = date ? { date } : undefined;
  const res = await client.get<ApiResponse<SafetyCheckStatusRaw>>(
    '/api/v1/admin/safety-check/status',
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '안전점검 현황 조회에 실패했습니다');
  }
  return mapSafetyCheckStatus(res.data.data);
}
