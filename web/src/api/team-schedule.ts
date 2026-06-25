import client from './client';
import type { ApiResponse } from './types';


export interface TeamMember {
  employeeId: number;
  employeeCode: string;
  name: string;
  /** 소속(조직명) */
  orgName?: string | null;
  /** 직위명 */
  jikwee?: string | null;
  /** 재직상태명 (재직/퇴사/휴직 등) */
  status?: string | null;
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
  accountType: string | null;
  accountBranchName: string | null;
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

export async function fetchTeamScheduleBranches(): Promise<Branch[]> {
  const res = await client.get<ApiResponse<Branch[]>>(
    '/api/v1/admin/team-schedule/branches',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}


/**
 * 여사원 일정관리 화면 초기 로드 통합 응답.
 *
 * `accounts` 채움 조건:
 * - `branchCode` 쿼리 파라미터 지정 → 해당 지점 거래처 (다중지점 사용자의 지점 선택 시점)
 * - 미지정 + 단일지점 사용자 (branches.length === 1) → 본인 지점 거래처 자동 채움
 * - 그 외 → 빈 배열
 *
 * `dailySummary` 는 accounts 가 결정된 경우 현재 월 + 해당 거래처 전체 기준 요약.
 * 마운트/지점선택 시점에 사용자 조회 없이도 캘린더 요약을 즉시 노출하기 위한 SF 정합.
 */
export interface TeamScheduleForm {
  branches: Branch[];
  members: TeamMember[];
  professionalPromotionTeams: string[];
  accounts: TeamScheduleAccount[];
  dailySummary: DailySummary[];
}

export async function fetchTeamScheduleForm(branchCode?: string): Promise<TeamScheduleForm> {
  const res = await client.get<ApiResponse<TeamScheduleForm>>(
    '/api/v1/admin/team-schedule/form',
    branchCode ? { params: { branchCode } } : undefined,
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
  branchCode?: string;
}): Promise<MonthlyScheduleWithSummary> {
  // 거래처 전체선택(549건) 시 accountIds 가 수 KB 쿼리스트링이 되어 GET URL 길이 한도를 초과해
  // 차단되던 문제로 조회를 POST + body 로 전환. 필터 ID 리스트를 body 로 운반한다.
  const res = await client.post<ApiResponse<MonthlyScheduleWithSummary>>(
    '/api/v1/admin/team-schedule/search',
    {
      from: params.from,
      to: params.to,
      employeeIds: params.employeeIds,
      accountIds: params.accountIds,
      ...(params.promotionTeams && params.promotionTeams.length > 0
        ? { promotionTeams: params.promotionTeams }
        : {}),
      ...(params.branchCode ? { branchCode: params.branchCode } : {}),
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
