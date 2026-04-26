import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: { code: string; message: string };
}

interface TeamMemberRaw {
  employee_id: number;
  employee_code: string;
  name: string;
}

interface TeamScheduleAccountRaw {
  account_id: number;
  external_key: string;
  name: string;
}

interface BranchRaw {
  branch_code: string;
  branch_name: string;
}

interface TeamScheduleRaw {
  id: number;
  employee_code: string;
  employee_name: string;
  working_date: string;
  working_type: string;
  working_category1: string | null;
  working_category2: string | null;
  working_category3: string | null;
  account_id: number | null;
  account_name: string | null;
  account_external_key: string | null;
  is_clock_in: boolean;
}

interface DailySummaryRaw {
  date: string;
  display_expected: number;
  display_actual: number;
  promotion_expected: number;
  promotion_actual: number;
  annual_leave: number;
  compensatory_leave: number;
}

interface MonthlyScheduleWithSummaryRaw {
  schedules: TeamScheduleRaw[];
  daily_summary: DailySummaryRaw[];
}

// --- Frontend interfaces (camelCase) ---

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
  working_date: string;
  working_type: string;
  working_category1?: string;
  working_category2?: string;
  working_category3?: string;
  account_id?: number;
}

// --- Mappers ---

function mapMembers(raw: TeamMemberRaw[]): TeamMember[] {
  return raw.map((m) => ({
    employeeId: m.employee_id,
    employeeCode: m.employee_code,
    name: m.name,
  }));
}

function mapAccounts(raw: TeamScheduleAccountRaw[]): TeamScheduleAccount[] {
  return raw.map((a) => ({
    accountId: a.account_id,
    externalKey: a.external_key,
    name: a.name,
  }));
}

function mapBranches(raw: BranchRaw[]): Branch[] {
  return raw.map((b) => ({
    branchCode: b.branch_code,
    branchName: b.branch_name,
  }));
}

function mapSchedules(raw: TeamScheduleRaw[]): TeamSchedule[] {
  return raw.map((s) => ({
    id: s.id,
    employeeCode: s.employee_code,
    employeeName: s.employee_name,
    workingDate: s.working_date,
    workingType: s.working_type,
    workingCategory1: s.working_category1,
    workingCategory2: s.working_category2,
    workingCategory3: s.working_category3,
    accountId: s.account_id,
    accountName: s.account_name,
    accountExternalKey: s.account_external_key,
    isClockIn: s.is_clock_in,
  }));
}

function mapSummaries(raw: DailySummaryRaw[]): DailySummary[] {
  return raw.map((d) => ({
    date: d.date,
    displayExpected: d.display_expected,
    displayActual: d.display_actual,
    promotionExpected: d.promotion_expected,
    promotionActual: d.promotion_actual,
    annualLeave: d.annual_leave,
    compensatoryLeave: d.compensatory_leave,
  }));
}

// --- API functions ---

export async function fetchTeamMembers(): Promise<TeamMember[]> {
  const res = await client.get<ApiResponse<TeamMemberRaw[]>>(
    '/api/v1/admin/team-schedule/members',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '팀원 목록 조회에 실패했습니다');
  }
  return mapMembers(res.data.data);
}

export async function fetchTeamScheduleAccounts(branchCode: string): Promise<TeamScheduleAccount[]> {
  const res = await client.get<ApiResponse<TeamScheduleAccountRaw[]>>(
    '/api/v1/admin/team-schedule/accounts',
    { params: { branchCode } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 목록 조회에 실패했습니다');
  }
  return mapAccounts(res.data.data);
}

export async function fetchTeamScheduleBranches(): Promise<Branch[]> {
  const res = await client.get<ApiResponse<BranchRaw[]>>(
    '/api/v1/admin/team-schedule/branches',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return mapBranches(res.data.data);
}

export async function fetchTeamSchedules(params: {
  year: number;
  month: number;
  employeeIds: number[];
  accountIds: number[];
}): Promise<MonthlyScheduleWithSummary> {
  const res = await client.get<ApiResponse<MonthlyScheduleWithSummaryRaw>>(
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
    schedules: mapSchedules(res.data.data.schedules),
    dailySummary: mapSummaries(res.data.data.daily_summary),
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
