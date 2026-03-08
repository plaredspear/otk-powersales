import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface AccountListRaw {
  content: AccountItemRaw[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

interface AccountItemRaw {
  id: number;
  external_key: string | null;
  name: string | null;
  abc_type: string | null;
  branch_code: string | null;
  branch_name: string | null;
  employee_code: string | null;
  address1: string | null;
  phone: string | null;
  account_status_name: string | null;
}

// --- Frontend interfaces (camelCase) ---

export interface FetchAccountsParams {
  keyword?: string;
  abcType?: string;
  branchCode?: string;
  accountStatusName?: string;
  page?: number;
  size?: number;
}

export interface Account {
  id: number;
  externalKey: string | null;
  name: string | null;
  abcType: string | null;
  branchCode: string | null;
  branchName: string | null;
  employeeCode: string | null;
  address1: string | null;
  phone: string | null;
  accountStatusName: string | null;
}

export interface AccountListData {
  content: Account[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// --- Mapper ---

function mapAccountList(raw: AccountListRaw): AccountListData {
  return {
    content: raw.content.map((item) => ({
      id: item.id,
      externalKey: item.external_key,
      name: item.name,
      abcType: item.abc_type,
      branchCode: item.branch_code,
      branchName: item.branch_name,
      employeeCode: item.employee_code,
      address1: item.address1,
      phone: item.phone,
      accountStatusName: item.account_status_name,
    })),
    page: raw.page,
    size: raw.size,
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
  };
}

// --- API function ---

export async function fetchAccounts(params: FetchAccountsParams): Promise<AccountListData> {
  const res = await client.get<ApiResponse<AccountListRaw>>('/api/v1/admin/accounts', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 목록 조회에 실패했습니다');
  }
  return mapAccountList(res.data.data);
}
