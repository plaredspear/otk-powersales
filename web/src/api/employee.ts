import client from './client';
import type { UserRole } from '@/constants/userRole';
import type { PPTTeamType } from '@/constants/pptTeamType';
import type { ApiResponse } from './types';


export interface FetchEmployeesParams {
  status?: string;
  costCenterCode?: string;
  keyword?: string;
  role?: UserRole;
  page?: number;
  size?: number;
}

/**
 * 여사원 현황 페이지 전용 — role 은 backend 에서 WOMAN 으로 강제되므로 제외.
 */
export interface FetchWomanEmployeesParams {
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
  role: UserRole | null;
  roleLabel: string | null;
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
  role: UserRole | null;
  roleLabel: string | null;
  appLoginActive: boolean | null;
  lockingFlag: boolean | null;
  professionalPromotionTeam: PPTTeamType | null;
  agreementFlag: boolean | null;

  // 근무 정보
  crmWorkType: string | null;
  crmWorkStartDate: string | null;
  totalAnnualLeave: string | null;
  usedAnnualLeave: string | null;
}

export interface EmployeeUpdateRequest {
  status?: string;
  role?: UserRole;
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
  role?: UserRole;
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

export async function fetchWomanEmployees(params: FetchWomanEmployeesParams): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListData>>('/api/v1/admin/women-employees', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '여사원 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchEmployee(employeeId: number): Promise<EmployeeDetail> {
  const res = await client.get<ApiResponse<EmployeeDetail>>(`/api/v1/admin/employees/${employeeId}`);
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

export interface EmployeePermissionProfileSummary {
  profileName: string;
  viewAllData: boolean;
  modifyAllData: boolean;
  viewAllUsers: boolean;
  manageUsers: boolean;
  apiEnabled: boolean;
}

export interface AssignedPermissionSet {
  assignmentId: number;
  permissionSetFlagsId: number;
  permissionSetName: string;
  permissionSetSfid: string;
  viewAllData: boolean;
  modifyAllData: boolean;
}

export interface EntityPermissionRow {
  entity: string;
  canRead: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

export interface EmployeePermissionInspection {
  employeeCode: string;
  userId: number;
  username: string;
  profile: EmployeePermissionProfileSummary | null;
  permissionSets: AssignedPermissionSet[];
  entityMatrix: EntityPermissionRow[];
  systemPermissions: string[];
}

export async function fetchEmployeePermissions(employeeId: number): Promise<EmployeePermissionInspection> {
  const res = await client.get<ApiResponse<EmployeePermissionInspection>>(
    `/api/v1/admin/employees/${employeeId}/permissions`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 권한 조회에 실패했습니다');
  }
  return res.data.data;
}
