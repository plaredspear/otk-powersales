import client from './client';
import type { ApiResponse } from './types';


export interface TeamMember {
  employeeId: number;
  employeeCode: string;
  name: string;
}

export interface TeamScheduleAccount {
  accountId: number;
  externalKey: string;
  name: string;
}

export interface Branch {
  branchCode: string;
  branchName: string;
}

export interface TeamSchedule {
  id: number;
  employeeCode: string;
  employeeName: string;
  workingDate: string;
  workingType: string;
  workingCategory1: string | null;
  workingCategory2: string | null;
  workingCategory3: string | null;
  accountId: number | null;
  accountName: string | null;
  accountExternalKey: string | null;
  isClockIn: boolean;
}

export interface DailySummary {
  date: string;
  displayExpected: number;
  displayActual: number;
  promotionExpected: number;
  promotionActual: number;
  annualLeave: number;
  compensatoryLeave: number;
}

export interface MonthlyScheduleWithSummary {
  schedules: TeamSchedule[];
  dailySummary: DailySummary[];
}

export interface TeamScheduleUpdateRequest {
  workingDate: string;
  workingType: string;
  workingCategory1?: string;
  workingCategory2?: string;
  workingCategory3?: string;
  accountId?: number;
}


// --- API functions ---

export async function fetchTeamMembers(branchCode?: string): Promise<TeamMember[]> {
  const res = await client.get<ApiResponse<TeamMember[]>>(
    '/api/v1/admin/team-schedule/members',
    branchCode ? { params: { branchCode } } : undefined,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '팀원 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchTeamScheduleAccounts(branchCode: string): Promise<TeamScheduleAccount[]> {
  const res = await client.get<ApiResponse<TeamScheduleAccount[]>>(
    '/api/v1/admin/team-schedule/accounts',
    { params: { branchCode } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchTeamScheduleBranches(): Promise<Branch[]> {
  const res = await client.get<ApiResponse<Branch[]>>(
    '/api/v1/admin/team-schedule/branches',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchTeamSchedules(params: {
  year: number;
  month: number;
  employeeIds: number[];
  accountIds: number[];
}): Promise<MonthlyScheduleWithSummary> {
  const res = await client.get<ApiResponse<MonthlyScheduleWithSummary>>(
    '/api/v1/admin/team-schedule',
    {
      params: {
        year: params.year,
        month: params.month,
        employeeIds: params.employeeIds.join(','),
        accountIds: params.accountIds.join(','),
      },
    },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '팀 일정 조회에 실패했습니다');
  }
  return {
    schedules: res.data.data.schedules,
    dailySummary: res.data.data.dailySummary,
  };
}

export async function updateTeamSchedule(id: number, data: TeamScheduleUpdateRequest): Promise<void> {
  const res = await client.put<ApiResponse<null>>(
    `/api/v1/admin/team-schedule/${id}`,
    data,
  );
  if (!res.data.success) {
    throw new Error(res.data.message || '일정 수정에 실패했습니다');
  }
}

export async function deleteTeamSchedule(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<null>>(
    `/api/v1/admin/team-schedule/${id}`,
  );
  if (!res.data.success) {
    throw new Error(res.data.message || '일정 삭제에 실패했습니다');
  }
}
