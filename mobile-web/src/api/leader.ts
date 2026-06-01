import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `LeaderTeamMemberListResponse` */
export interface LeaderTeamMember {
  id: number;
  employeeCode: string;
  name: string;
  status: string | null;
  costCenterCode: string | null;
}

/** backend `LeaderAccountListResponse` */
export interface LeaderAccount {
  id: number;
  name: string | null;
  address1: string | null;
  branchCode: string | null;
  accountGroup: string | null;
  accountType: string | null;
}

/** backend `LeaderDailyWorkerItem` */
export interface LeaderDailyWorker {
  scheduleId: number;
  employeeId: number | null;
  employeeName: string;
  employeeCode: string;
  accountName: string;
  accountCode: string;
  workingCategory1: string | null;
  workingCategory2: string | null;
  workingCategory3: string | null;
  attended: boolean;
}

export interface LeaderDailyEmployee {
  employeeId: number | null;
  employeeName: string;
  employeeCode: string;
}

export interface LeaderDailyStatusSummary {
  displayTotal: number;
  displayAttended: number;
  eventTotal: number;
  eventAttended: number;
  annualLeaveCount: number;
}

/** backend `LeaderDailyStatusResponse` */
export interface LeaderDailyStatus {
  date: string;
  summary: LeaderDailyStatusSummary;
  displayWorkers: LeaderDailyWorker[];
  eventWorkers: LeaderDailyWorker[];
  annualLeaveWorkers: LeaderDailyEmployee[];
}

/** backend `LeaderScheduleCreateRequest` */
export interface LeaderScheduleCreateRequest {
  targetEmployeeId?: number;
  workingDate: string; // YYYY-MM-DD
  workingType: string;
  workingCategory2: string;
  workingCategory3: string;
}

export async function fetchTeamMembers(): Promise<LeaderTeamMember[]> {
  const res = await client.get<ApiResponse<LeaderTeamMember[]>>('/api/v1/mobile/leader/team-members');
  return unwrap(res, '여사원 목록 조회에 실패했습니다');
}

export async function fetchLeaderAccounts(keyword?: string): Promise<LeaderAccount[]> {
  const res = await client.get<ApiResponse<LeaderAccount[]>>('/api/v1/mobile/leader/accounts', {
    params: keyword ? { keyword } : undefined,
  });
  return unwrap(res, '거래처 조회에 실패했습니다');
}

export async function fetchLeaderDailyStatus(date: string): Promise<LeaderDailyStatus> {
  const res = await client.get<ApiResponse<LeaderDailyStatus>>('/api/v1/mobile/leader/daily-status', {
    params: { date },
  });
  return unwrap(res, '일별 현황 조회에 실패했습니다');
}

export async function createTeamMemberSchedule(
  request: LeaderScheduleCreateRequest
): Promise<{ scheduleId: number }> {
  const res = await client.post<ApiResponse<{ scheduleId: number }>>(
    '/api/v1/mobile/leader/team-member-schedule',
    request
  );
  return unwrap(res, '일정 등록에 실패했습니다');
}

/** 진열 일정변경 — 거래처 변경 (PUT). */
export async function updateTeamMemberScheduleAccount(
  scheduleId: number,
  accountId: number
): Promise<void> {
  const res = await client.put<ApiResponse<unknown>>(
    `/api/v1/mobile/leader/team-member-schedule/${scheduleId}`,
    { accountId }
  );
  if (!res.data.success) throw new Error(res.data.error?.message || '일정 변경에 실패했습니다');
}

export async function deleteTeamMemberSchedule(scheduleId: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(
    `/api/v1/mobile/leader/team-member-schedule/${scheduleId}`
  );
  if (!res.data.success) throw new Error(res.data.error?.message || '일정 삭제에 실패했습니다');
}
