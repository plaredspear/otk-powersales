import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface EmployeeListRaw {
  content: EmployeeItemRaw[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

interface EmployeeItemRaw {
  sfid: string | null;
  employee_id: string;
  name: string;
  status: string | null;
  org_name: string | null;
  cost_center_code: string | null;
  app_authority: string | null;
  start_date: string | null;
  app_login_active: boolean | null;
  work_phone: string | null;
}

// --- Frontend interfaces (camelCase) ---

export interface FetchEmployeesParams {
  status?: string;
  costCenterCode?: string;
  keyword?: string;
  appAuthority?: string;
  page?: number;
  size?: number;
}

export interface Employee {
  sfid: string | null;
  employeeId: string;
  name: string;
  status: string | null;
  orgName: string | null;
  costCenterCode: string | null;
  appAuthority: string | null;
  startDate: string | null;
  appLoginActive: boolean | null;
  workPhone: string | null;
}

export interface EmployeeListData {
  content: Employee[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// --- Mapper ---

function mapEmployeeList(raw: EmployeeListRaw): EmployeeListData {
  return {
    content: raw.content.map((item) => ({
      sfid: item.sfid,
      employeeId: item.employee_id,
      name: item.name,
      status: item.status,
      orgName: item.org_name,
      costCenterCode: item.cost_center_code,
      appAuthority: item.app_authority,
      startDate: item.start_date,
      appLoginActive: item.app_login_active,
      workPhone: item.work_phone,
    })),
    page: raw.page,
    size: raw.size,
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
  };
}

// --- API function ---

export async function fetchEmployees(params: FetchEmployeesParams): Promise<EmployeeListData> {
  const res = await client.get<ApiResponse<EmployeeListRaw>>('/api/v1/admin/employees', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사원 목록 조회에 실패했습니다');
  }
  return mapEmployeeList(res.data.data);
}
