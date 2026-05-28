import client from './client';
import type { ApiResponse } from './types';


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


// --- API function ---

export async function fetchAccounts(params: FetchAccountsParams): Promise<AccountListData> {
  const res = await client.get<ApiResponse<AccountListData>>('/api/v1/admin/accounts', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사마스터 등록/수정 화면의 거래처 lookup search — promotion 권한 보유자 호출용.
 *
 * Account READ 권한 없이도 호출 가능 (SF lookup search 메커니즘 정합). lookupFilter +
 * sharing rule 평가는 백엔드가 동일하게 적용.
 */
export async function fetchAccountsForPromotionLookup(
  params: Pick<FetchAccountsParams, 'keyword' | 'page' | 'size'>,
): Promise<AccountListData> {
  const res = await client.get<ApiResponse<AccountListData>>('/api/v1/admin/accounts/lookup', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 검색에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 물류 클레임 등록/수정 화면의 거래처 lookup search — suggestion 권한 보유자 호출용.
 *
 * Account READ 권한 없이도 호출 가능 (SF Claim__c.AccId__c Lookup 메커니즘 정합).
 */
export async function fetchAccountsForClaimLookup(
  params: Pick<FetchAccountsParams, 'keyword' | 'page' | 'size'>,
): Promise<AccountListData> {
  const res = await client.get<ApiResponse<AccountListData>>('/api/v1/admin/accounts/lookup-for-claim', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 검색에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 유통기한 관리 / 재고조회 화면의 거래처 lookup search — product 권한 보유자 호출용.
 *
 * Account READ 권한 없이도 호출 가능 (Heroku 단독 / 신규 기능, SF 매핑 없음).
 */
export async function fetchAccountsForProductLookup(
  params: Pick<FetchAccountsParams, 'keyword' | 'page' | 'size'>,
): Promise<AccountListData> {
  const res = await client.get<ApiResponse<AccountListData>>('/api/v1/admin/accounts/lookup-for-product', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 검색에 실패했습니다');
  }
  return res.data.data;
}

// --- 신규 거래처 등록 (Spec #640 P2-W) ---

export interface AdminAccountCreateRequest {
  name: string;
  employeeCode: string;
}

export interface AdminAccountCreateResponseData {
  id: number;
  name: string;
  accountGroup: string;
  employeeCode: string;
  branchCode: string | null;
  branchName: string | null;
}

/**
 * 관리자 웹 신규 거래처 등록 (Spec #640 P1-B 엔드포인트 호출).
 *
 * Backend `POST /api/v1/admin/accounts` (`ACCOUNT_WRITE` 권한 필요).
 * 에러는 axios 가 throw — 호출 측에서 `error.response.data.error.code` 분기.
 */
export async function createAdminAccount(
  payload: AdminAccountCreateRequest,
): Promise<AdminAccountCreateResponseData> {
  const res = await client.post<ApiResponse<AdminAccountCreateResponseData>>(
    '/api/v1/admin/accounts',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 등록에 실패했습니다');
  }
  return res.data.data;
}

// --- 거래처 수정 (Spec #643 P2-W) ---

/**
 * 관리자 웹 거래처 수정 요청 페이로드 (Spec #643 P2-W).
 *
 * Backend `PUT /api/v1/admin/accounts/{id}` (`ACCOUNT_WRITE` 권한 필요).
 *
 * **PUT 부분 갱신 시맨틱 (Q-E)**:
 * - 모든 필드 optional. 보내지 않은 필드는 보존 (Backend 단 null = 미포함 처리).
 * - `employeeCode = ""` 는 null 동등 — Backend 단 변경 안 함 시맨틱.
 *
 * **SAP 동기 키 필드 (silent ignore — Q-C)**: DTO 정의 자체에 미포함 (`external_key`/`werk*`/
 * `sales_dept_*`/`division_*`/`logistics_*`/`branch_cost_center`/`is_deleted`/`latitude`/`longitude` 등).
 */
export interface AdminAccountUpdateRequest {
  name?: string;
  address1?: string;
  address2?: string;
  phone?: string;
  mobilePhone?: string;
  representative?: string;
  email?: string;
  zipCode?: string;
  industry?: string;
  description?: string;
  website?: string;
  fax?: string;
  businessNumber?: string;
  closingTime1?: string;
  closingTime2?: string;
  closingTime3?: string;
  accountNumber?: string;
  site?: string;
  accountSource?: string;
  mapCoordinate?: string;
  rating?: string;
  ownership?: string;
  accountStatusName?: string;
  accountStatusCode?: string;
  businessType?: string;
  businessCategory?: string;
  businessLicenseNumber?: string;
  consignmentAcc?: string;
  distribution?: string;
  accountType?: string;
  freezerType?: string;
  freezerInstalled?: boolean;
  firstInstalled?: string;
  orderEndTime?: string;
  remainingCredit?: string;
  totalCredit?: string;
  annualRevenue?: string;
  numberOfEmployees?: number;
  employeeCode?: string;
  branchCode?: string;
  branchName?: string;
  abcType?: string;
  abcTypeCode?: string;
}

export interface AdminAccountUpdateResponseData {
  id: number;
  name: string | null;
  accountGroup: string | null;
  employeeCode: string | null;
  branchCode: string | null;
  branchName: string | null;
  address1: string | null;
  address2: string | null;
  zipCode: string | null;
  phone: string | null;
  mobilePhone: string | null;
  representative: string | null;
  email: string | null;
  fax: string | null;
  website: string | null;
  industry: string | null;
  description: string | null;
  businessNumber: string | null;
  businessLicenseNumber: string | null;
  businessType: string | null;
  businessCategory: string | null;
  abcType: string | null;
  abcTypeCode: string | null;
  accountType: string | null;
  accountStatusName: string | null;
  accountStatusCode: string | null;
  accountNumber: string | null;
  site: string | null;
  accountSource: string | null;
  mapCoordinate: string | null;
  rating: string | null;
  ownership: string | null;
  freezerInstalled: boolean | null;
  freezerType: string | null;
  firstInstalled: string | null;
  orderEndTime: string | null;
  closingTime1: string | null;
  closingTime2: string | null;
  closingTime3: string | null;
  remainingCredit: string | null;
  totalCredit: string | null;
  annualRevenue: string | null;
  numberOfEmployees: number | null;
  consignmentAcc: string | null;
  distribution: string | null;
}

/**
 * 관리자 웹 거래처 수정 (Spec #643 P1-B 엔드포인트 호출).
 *
 * Backend `PUT /api/v1/admin/accounts/{id}` (`ACCOUNT_WRITE` 권한 필요).
 * - 200 OK: 수정 성공 + 응답 body 에 갱신 후 entity 매핑
 * - 400 ACCOUNT_NAME_BLANK / ACCOUNT_NAME_PREFIX_REQUIRED: name 검증 실패
 * - 404 ACCOUNT_NOT_FOUND / EMPLOYEE_NOT_FOUND
 * - 409 ACCOUNT_NAME_DUPLICATE
 *
 * 에러는 axios 가 throw — 호출 측에서 `error.response.data.error.code` 분기.
 */
export async function updateAdminAccount(
  id: number,
  payload: AdminAccountUpdateRequest,
): Promise<AdminAccountUpdateResponseData> {
  const res = await client.put<ApiResponse<AdminAccountUpdateResponseData>>(
    `/api/v1/admin/accounts/${id}`,
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처 수정에 실패했습니다');
  }
  return res.data.data;
}

// --- 거래처 삭제 (Spec #642 P2-W) ---

/**
 * 관리자 웹 거래처 삭제 (Spec #642 P1-B 엔드포인트 호출).
 *
 * Backend `DELETE /api/v1/admin/accounts/{id}` (`ACCOUNT_DELETE` 권한 필요).
 * - 200 OK: 삭제 성공 (`is_deleted=true` soft-delete)
 * - 409 ACCOUNT_DELETE_BLOCKED_SAP_SYNCED: SAP 동기 거래처(`external_key IS NOT NULL`) 차단
 * - 404 ACCOUNT_NOT_FOUND: 미존재 또는 이미 삭제됨
 *
 * 에러는 axios 가 throw — 호출 측에서 `error.response.data.error.code` 분기.
 */
export async function deleteAdminAccount(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<null>>(`/api/v1/admin/accounts/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '거래처 삭제에 실패했습니다');
  }
}
