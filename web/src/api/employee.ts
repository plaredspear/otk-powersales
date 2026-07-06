import client from './client';
import type { AppAuthority } from "@/constants/userRole";
import type { PPTTeamType } from '@/constants/pptTeamType';
import type { ApiResponse } from './types';


export interface FetchEmployeesParams {
  status?: string;
  costCenterCode?: string;
  keyword?: string;
  role?: AppAuthority;
  page?: number;
  size?: number;
}

/**
 * 여사원 현황 페이지 전용 — role 은 backend 에서 여사원+조장으로 고정되므로 제외.
 */
export interface FetchFemaleEmployeesParams {
  status?: string;
  costCenterCode?: string;
  keyword?: string;
  // 근무형태1(진열/행사) / 근무형태3(고정/격고/순회) — 최근 출근등록 1건 기준 필터.
  workType1?: string;
  workType3?: string;
  // 전문행사조 — 조명(라면세일조 등) 또는 '일반'(미배정). 미지정이면 전체.
  professionalPromotionTeam?: string;
  page?: number;
  size?: number;
}

export interface Employee {
  id: number;
  employeeCode: string;
  name: string;
  status: string | null;
  gender: string | null;
  orgName: string | null;
  costCenterCode: string | null;
  role: AppAuthority | null;
  startDate: string | null;
  endDate: string | null;
  appLoginActive: boolean | null;
  workPhone: string | null;
  jikchak: string | null;
  jikwee: string | null;
  jikgub: string | null;
  jobCode: string | null;
  appointmentDate: string | null;
  ordDetailNode: string | null;
  // SF 여사원 리스트뷰 정합 컬럼
  jikjong: string | null;
  workEmail: string | null;
  phone: string | null;
  age: string | null;
  yearsOfService: string | null;
  // 전문행사조 (라면세일조/프레시세일조_만두 등). 여사원 현황 목록 응답에서 backend 가 항상
  // 채워 보내며(미배정은 '일반'), 다른 목록 응답에는 없을 수 있어 optional.
  professionalPromotionTeam?: string;
  // 근무형태1 — 가장 최근 출근등록 1건의 근무유형1(진열/행사). 출근등록 이력이 없으면 null (UI '-').
  workType1?: string | null;
  // 근무형태3 — 가장 최근 출근등록 1건의 근무유형3(고정/격고/순회). 출근등록 이력이 없으면 null (UI '-').
  workType3?: string | null;
  // 근무거래처 — 가장 최근 출근등록 1건의 거래처명 / 거래처코드(SAP거래처코드). 이력/거래처 없으면 null.
  workAccountName?: string | null;
  workAccountCode?: string | null;
}

export interface EmployeeListData {
  content: Employee[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * 사원 상세 응답 — 6개 그룹 필드 모두 노출.
 * 백엔드 EmployeeDetailResponse 와 1:1.
 */
export interface EmployeeDetail {
  // 인사 정보
  id: number;
  employeeCode: string;
  name: string;
  gender: string | null;
  status: string | null;
  birthDate: string | null;
  startDate: string | null;
  endDate: string | null;
  appointmentDate: string | null;
  origin: 'SAP' | 'MANUAL' | null;

  // 조직 정보
  costCenterCode: string | null;
  orgName: string | null;
  locationCode: string | null;
  workArea: string | null;

  // 직무 정보
  jobCode: string | null;
  jikjong: string | null;
  jikwee: string | null;
  jikchak: string | null;
  jikgub: string | null;
  workType: string | null;
  ordDetailNode: string | null;

  // 연락처
  phone: string | null;
  homePhone: string | null;
  workPhone: string | null;
  officePhone: string | null;
  workEmail: string | null;
  email: string | null;

  // 앱 설정
  role: AppAuthority | null;
  appLoginActive: boolean | null;
  lockingFlag: boolean | null;
  professionalPromotionTeam: PPTTeamType | null;
  agreementFlag: boolean | null;
  // 사용자가 마지막 로그인/리프레시 때 보고한 현재 사용 앱 버전 (미보고 시 null)
  appVersionName: string | null;
  appVersionCode: number | null;
  appPlatform: string | null;
  appVersionSeenAt: string | null;

  // 근무 정보
  crmWorkType: string | null;
  crmWorkStartDate: string | null;
  totalAnnualLeave: string | null;
  usedAnnualLeave: string | null;
}

export interface EmployeeUpdateRequest {
  status?: string;
  role?: AppAuthority;
  orgName?: string;
  costCenterCode?: string;
  workArea?: string;
  locationCode?: string;
  jobCode?: string;
  jikjong?: string;
  jikwee?: string;
  jikchak?: string;
  jikgub?: string;
  workType?: string;
  ordDetailNode?: string;
  appointmentDate?: string;
  startDate?: string;
  endDate?: string;
  homePhone?: string;
  workPhone?: string;
  officePhone?: string;
  workEmail?: string;
  email?: string;
  appLoginActive?: boolean;
  lockingFlag?: boolean;
  professionalPromotionTeam?: PPTTeamType;
}

export interface EmployeeManualRegisterRequest {
  employeeCode: string;
  name: string;
  role?: AppAuthority;
  orgName?: string;
  costCenterCode?: string;
  jobCode?: string;
  jikwee?: string;
  jikchak?: string;
  jikgub?: string;
  startDate?: string;
  homePhone?: string;
  workPhone?: string;
  workEmail?: string;
  professionalPromotionTeam?: PPTTeamType;
}


export interface EmployeeBranch {
  branchCode: string;
  branchName: string;
}

// --- API function ---

export async function fetchEmployees(params: FetchEmployeesParams): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/employees', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 사원 목록 화면 지점 셀렉터 옵션 (`GET /api/v1/admin/employees/branches`, `employee` READ 권한 필요).
 *
 * 사원 목록은 전사 조회이므로 옵션도 전 지점(전사) 목록을 반환한다 (거래처의 권한별 화이트리스트와 다름).
 */
export async function getEmployeeBranches(): Promise<EmployeeBranch[]> {
  const res = await client.get<ApiResponse<EmployeeBranch[]>>('/api/v1/admin/employees/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사상세/전문행사조 화면의 사원 lookup search — promotion 권한 보유자 호출용.
 *
 * Employee READ 권한 없이도 호출 가능 (SF PromotionEmployee__c.EmployeeId__c Lookup
 * 메커니즘 정합). 검색 범위는 서버에서 재직 사원으로 항상 고정되므로 status 파라미터를 받지 않는다.
 */
export async function fetchEmployeesForPromotionLookup(
  params: Pick<FetchEmployeesParams, 'keyword' | 'page' | 'size'>,
): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/employees/lookup', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 검색에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 거래처 등록/수정 화면의 영업담당자 lookup search — account 권한 보유자 호출용.
 *
 * Employee READ 권한 없이도 호출 가능 (Spec #640 신규 영업담당자 매핑 기능).
 */
export async function fetchEmployeesForAccountLookup(
  params: Pick<FetchEmployeesParams, 'keyword' | 'status' | 'page' | 'size'>,
): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/employees/lookup-for-account', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 검색에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 유통기한 관리 화면의 사원 lookup search — product 권한 보유자 호출용.
 *
 * Employee READ 권한 없이도 호출 가능 (Heroku 단독 기능, SF 매핑 없음).
 */
export async function fetchEmployeesForProductLookup(
  params: Pick<FetchEmployeesParams, 'keyword' | 'status' | 'page' | 'size'>,
): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/employees/lookup-for-product', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 검색에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 진열사원 스케줄 마스터 화면의 사원 lookup search — team_member_schedule 권한 보유자 호출용.
 *
 * Employee READ 권한 없이도 호출 가능 (SF TeamMemberSchedule__c.EmployeeId__c Lookup
 * 메커니즘 정합).
 */
export async function fetchEmployeesForScheduleLookup(
  params: Pick<FetchEmployeesParams, 'keyword' | 'status' | 'page' | 'size'>,
): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/employees/lookup-for-schedule', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 검색에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchFemaleEmployees(params: FetchFemaleEmployeesParams): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/female-employees', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '여사원 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 여사원 현황 화면 지점 셀렉터 옵션. */
export interface FemaleEmployeeBranch {
  branchCode: string;
  branchName: string;
}

/**
 * 여사원 현황 화면 지점 셀렉터 옵션 조회.
 *
 * backend 의 권한별 지점 화이트리스트(WomenScheduleBranchResolver)를 반환한다. 여사원 현황
 * 화면의 게이팅 권한(`female_employee`)과 동일하게 가드되는 전용 endpoint 라, 조장 등 여사원
 * 권한만 가진 직책도 접근 가능하다.
 */
export async function fetchFemaleEmployeeBranches(): Promise<FemaleEmployeeBranch[]> {
  const res = await client.get<ApiResponse<FemaleEmployeeBranch[]>>('/api/v1/admin/female-employees/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 여사원 현황 엑셀 다운로드 경로 (GET, 목록과 동일 검색 파라미터). */
export const FEMALE_EMPLOYEE_EXPORT_PATH = '/api/v1/admin/female-employees/export';

/**
 * 상세/근무이력 endpoint base path 선택자.
 *
 * 여사원 현황(`female_employee` 권한)에서 진입한 상세는 `/female-employees/*` 를,
 * 설정 사원(`employee` 권한) 에서 진입한 상세는 `/employees/*` 를 호출하여 권한 가드와 정합.
 */
function detailBasePath(isFemale: boolean): string {
  return isFemale ? '/api/v1/admin/female-employees' : '/api/v1/admin/employees';
}

/**
 * 월별 근무내역 base path 선택자 — 호출 화면의 게이팅 권한과 API 가드를 정합.
 *
 * 근무기간 조회 화면(`attend_info` 권한)은 `/attend-info/*`, 여사원 현황(`female_employee`)은
 * `/female-employees/*`, 그 외(설정 사원, `employee`)는 `/employees/*` 를 호출한다. 각 base 의
 * `{id}/work-history/monthly` 는 해당 도메인 entity READ 로 가드된다.
 */
export type WorkHistoryScope = 'employee' | 'female' | 'attendInfo';

function monthlyWorkHistoryBasePath(scope: WorkHistoryScope): string {
  switch (scope) {
    case 'female':
      return '/api/v1/admin/female-employees';
    case 'attendInfo':
      return '/api/v1/admin/attend-info';
    default:
      return '/api/v1/admin/employees';
  }
}

export async function fetchEmployee(
  employeeId: number,
  isFemale = false,
): Promise<EmployeeDetail> {
  const res = await client.get<ApiResponse<EmployeeDetail>>(
    `${detailBasePath(isFemale)}/${employeeId}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function updateEmployee(
  employeeId: number,
  request: EmployeeUpdateRequest,
): Promise<EmployeeDetail> {
  const res = await client.patch<ApiResponse<EmployeeDetail>>(
    `/api/v1/admin/employees/${employeeId}`,
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 정보 수정에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 사원 권한(role) 전용 수정.
 *
 * 일반 수정([updateEmployee]) 과 달리 origin=SAP 사원도 허용된다 — 권한 필드는 SAP 인입이
 * 갱신하지 않아 경합하지 않기 때문. AccountViewAll(전체 거래처 조회 권한) 처럼 SAP 발령으로
 * 산출되지 않는 권한을 부여하는 유일한 경로다.
 */
export async function updateEmployeeRole(
  employeeId: number,
  role: AppAuthority,
): Promise<EmployeeDetail> {
  const res = await client.patch<ApiResponse<EmployeeDetail>>(
    `/api/v1/admin/employees/${employeeId}/role`,
    { role },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 권한 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function manualRegisterEmployee(
  request: EmployeeManualRegisterRequest,
): Promise<EmployeeDetail> {
  const res = await client.post<ApiResponse<EmployeeDetail>>(
    '/api/v1/admin/employees/manual',
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 등록에 실패했습니다');
  }
  return res.data.data;
}

export interface EmployeeWorkHistoryItem {
  id: number;
  workingDate: string | null;
  workingType: string | null;
  workingCategory1: string | null;
  workingCategory3: string | null;
  workingCategory4: string | null;
  /** 전문행사조 (라면세일조/카레행사조 등) */
  professionalPromotionTeam: string | null;
  accountName: string | null;
  accountExternalKey: string | null;
  /** 거래처유형 (대형마트(3대)/체인/C.V.S 등) */
  accountType: string | null;
  /** ABC유형 */
  abcType: string | null;
  /** ABC유형코드 */
  abcTypeCode: string | null;
  /** 거래처유형 — ABC유형코드 + ABC유형 조합 (예: "6111 이마트") */
  abcTypeLabel: string | null;
  /** 거래처상태코드 (유통형태 표시용) */
  accountStatusCode: string | null;
  /** 유통형태 — 거래처상태코드 + 거래처유형 조합 (예: "02 슈퍼") */
  distributionChannelLabel: string | null;
  isClockIn: boolean;
  // 근무기간 조회(월별) 화면 확장 필드 — 최근이력 응답에서는 값이 채워질 수 있으나 미사용
  refAccountName: string | null;
  costCenterCode: string | null;
  secondWorkType: string | null;
  startTime: string | null;
  completeTime: string | null;
}

export interface EmployeeWorkHistory {
  items: EmployeeWorkHistoryItem[];
}

export async function fetchEmployeeWorkHistory(
  employeeId: number,
  limit = 10,
  isFemale = false,
): Promise<EmployeeWorkHistory> {
  const res = await client.get<ApiResponse<EmployeeWorkHistory>>(
    `${detailBasePath(isFemale)}/${employeeId}/work-history`,
    { params: { limit } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '근무이력 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 근무기간 조회(월별) — 인원 1명 × 지정 월의 근무내역(어디서/어떻게)을 일자 오름차순 조회.
 * @param yearMonth `yyyy-MM` (예: 2026-06)
 */
/** 근무기간 조회(월별) 엑셀 다운로드 경로 (GET, ?yearMonth=yyyy-MM). */
export function employeeMonthlyWorkHistoryExportPath(
  employeeId: number,
  scope: WorkHistoryScope = 'employee',
): string {
  return `${monthlyWorkHistoryBasePath(scope)}/${employeeId}/work-history/monthly/export`;
}

export async function fetchEmployeeMonthlyWorkHistory(
  employeeId: number,
  yearMonth: string,
  scope: WorkHistoryScope = 'employee',
): Promise<EmployeeWorkHistory> {
  const res = await client.get<ApiResponse<EmployeeWorkHistory>>(
    `${monthlyWorkHistoryBasePath(scope)}/${employeeId}/work-history/monthly`,
    { params: { yearMonth } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '월별 근무내역 조회에 실패했습니다');
  }
  return res.data.data;
}
