import client from './client';
import type { ApiResponse } from './types';

export type ValidStatusFilter = 'ALL' | 'VALID' | 'PLANNED' | 'ENDED';

export type TypeOfWork1 = '진열';

export interface EmployeeInputCriteriaMaster {
  id: number;
  name: string | null;
  categoryId: number | null;
  categoryCode: string | null;
  categoryName: string | null;
  typeOfWork1: TypeOfWork1 | null;
  startDate: string | null;
  endDate: string | null;
  confirmed: boolean;
  boundary: string | null;
  fixed1PersonStandardAmount: string | null;
  bifurcationHalfPersonStandard: string | null;
  fixed1PersonMinAmountInRealmRange: string | null;
  bifurcationHalfPersonMinAmountInRealmRange: string | null;
  accountCategorizedCode: string | null;
  validData: '유효' | '예정' | '종료' | null;
}

export interface EmployeeInputCriteriaMasterRequest {
  categoryId: number;
  typeOfWork1?: TypeOfWork1 | null;
  startDate: string;
  endDate?: string | null;
  boundary: string;
  fixed1PersonStandardAmount: string;
  bifurcationHalfPersonStandard: string;
}

// --- Form-Meta interfaces ---

export interface AccountCategoryOption {
  value: number;
  accountCode: string;
  name: string;
}

export interface TypeOfWork1Option {
  value: TypeOfWork1;
  name: string;
}

export interface EmployeeInputCriteriaMasterFormMeta {
  accountCategories: AccountCategoryOption[];
  typeOfWork1Options: TypeOfWork1Option[];
}

// --- List-Meta interfaces ---

export type EmployeeInputCriteriaMasterFilterType = 'TEXT' | 'SELECT' | 'DATE';

export interface EmployeeInputCriteriaMasterFilterOption {
  value: string;
  label: string;
}

export interface EmployeeInputCriteriaMasterFilterMeta {
  key: string;
  type: EmployeeInputCriteriaMasterFilterType;
  options?: EmployeeInputCriteriaMasterFilterOption[] | null;
}

export interface EmployeeInputCriteriaMasterListDefaults {
  status: ValidStatusFilter;
}

export interface EmployeeInputCriteriaMasterListMeta {
  filters: EmployeeInputCriteriaMasterFilterMeta[];
  defaults: EmployeeInputCriteriaMasterListDefaults;
}

const BASE = '/api/v1/admin/employee-input-criteria-masters';

export async function fetchEmployeeInputCriteriaMasters(
  status: ValidStatusFilter,
): Promise<EmployeeInputCriteriaMaster[]> {
  const res = await client.get<ApiResponse<EmployeeInputCriteriaMaster[]>>(
    `${BASE}?status=${status}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchEmployeeInputCriteriaMaster(
  id: number,
): Promise<EmployeeInputCriteriaMaster> {
  const res = await client.get<ApiResponse<EmployeeInputCriteriaMaster>>(`${BASE}/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createEmployeeInputCriteriaMaster(
  data: EmployeeInputCriteriaMasterRequest,
): Promise<EmployeeInputCriteriaMaster> {
  const res = await client.post<ApiResponse<EmployeeInputCriteriaMaster>>(BASE, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateEmployeeInputCriteriaMaster(
  id: number,
  data: EmployeeInputCriteriaMasterRequest,
): Promise<EmployeeInputCriteriaMaster> {
  const res = await client.put<ApiResponse<EmployeeInputCriteriaMaster>>(`${BASE}/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function confirmEmployeeInputCriteriaMaster(
  id: number,
): Promise<EmployeeInputCriteriaMaster> {
  const res = await client.post<ApiResponse<EmployeeInputCriteriaMaster>>(
    `${BASE}/${id}/confirm`,
    {},
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '확정에 실패했습니다');
  }
  return res.data.data;
}

export async function bulkConfirmEmployeeInputCriteriaMasters(
  ids: number[],
): Promise<EmployeeInputCriteriaMaster[]> {
  const res = await client.post<ApiResponse<EmployeeInputCriteriaMaster[]>>(
    `${BASE}/bulk-confirm`,
    { ids },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일괄 확정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteEmployeeInputCriteriaMaster(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`${BASE}/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 삭제에 실패했습니다');
  }
}

/**
 * 폼(등록/수정 모달) 렌더링용 메타.
 *
 * 구분(거래처유형마스터) 옵션 + 근무형태1 옵션을 한 번에 로드한다.
 * 기존 `/account-categories` lookup 을 대체하며, 근무형태1 프론트 하드코딩 상수를 제거한다.
 */
export async function fetchEmployeeInputCriteriaMasterFormMeta(): Promise<EmployeeInputCriteriaMasterFormMeta> {
  const res = await client.get<ApiResponse<EmployeeInputCriteriaMasterFormMeta>>(
    `${BASE}/form-meta`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 폼 메타 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 목록 화면 조회 조건 로드.
 *
 * 상태 필터(전체/유효/예정/종료) 옵션과 기본값을 반환한다. 기존 프론트 하드코딩 상수를 대체한다.
 */
export async function fetchEmployeeInputCriteriaMasterListMeta(): Promise<EmployeeInputCriteriaMasterListMeta> {
  const res = await client.get<ApiResponse<EmployeeInputCriteriaMasterListMeta>>(`${BASE}/meta`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '진열사원 투입기준 마스터 조회 조건 로드에 실패했습니다');
  }
  return res.data.data;
}
