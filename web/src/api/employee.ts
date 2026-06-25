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
 * 여사원 현황 페이지 전용 — role 은 backend 에서 WOMAN 으로 강제되므로 제외.
 */
export interface FetchFemaleEmployeesParams {
  status?: string;
  costCenterCode?: string;
  keyword?: string;
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


// --- API function ---

export async function fetchEmployees(params: FetchEmployeesParams): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/employees', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 목록 조회에 실패했습니다');
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
export async function fetchEmployeeMonthlyWorkHistory(
  employeeId: number,
  yearMonth: string,
  isFemale = false,
): Promise<EmployeeWorkHistory> {
  const res = await client.get<ApiResponse<EmployeeWorkHistory>>(
    `${detailBasePath(isFemale)}/${employeeId}/work-history/monthly`,
    { params: { yearMonth } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '월별 근무내역 조회에 실패했습니다');
  }
  return res.data.data;
}
