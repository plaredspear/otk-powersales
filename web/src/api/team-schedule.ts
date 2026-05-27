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
  promotionId: number | null;
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

export async function fetchTeamMembers(): Promise<TeamMember[]> {
  const res = await client.get<ApiResponse<TeamMember[]>>(
    '/api/v1/admin/team-schedule/members',
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

export async function fetchProfessionalPromotionTeams(): Promise<string[]> {
  const res = await client.get<ApiResponse<string[]>>(
    '/api/v1/admin/team-schedule/professional-promotion-teams',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 여사원 일정관리 화면 초기 로드 통합 응답.
 *
 * `accounts` 는 단일지점 사용자일 때만 backend 가 채워 보낸다 (branches.length === 1).
 * 다중지점 사용자는 빈 배열이며, 사용자가 지점 드롭다운에서 선택한 시점에 별도 `/accounts` 호출로 채운다.
 */
export interface TeamScheduleForm {
  branches: Branch[];
  members: TeamMember[];
  professionalPromotionTeams: string[];
  accounts: TeamScheduleAccount[];
}

export async function fetchTeamScheduleForm(): Promise<TeamScheduleForm> {
  const res = await client.get<ApiResponse<TeamScheduleForm>>(
    '/api/v1/admin/team-schedule/form',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '여사원 일정관리 초기 데이터 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchTeamSchedules(params: {
  from: string;
  to: string;
  employeeIds: number[];
  accountIds: number[];
  promotionTeams?: string[];
}): Promise<MonthlyScheduleWithSummary> {
  const res = await client.get<ApiResponse<MonthlyScheduleWithSummary>>(
    '/api/v1/admin/team-schedule',
    {
      params: {
        from: params.from,
        to: params.to,
        employeeIds: params.employeeIds.join(','),
        accountIds: params.accountIds.join(','),
        ...(params.promotionTeams && params.promotionTeams.length > 0
          ? { promotionTeams: params.promotionTeams.join(',') }
          : {}),
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
