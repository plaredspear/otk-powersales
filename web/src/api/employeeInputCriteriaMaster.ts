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

export interface AccountCategoryOption {
  id: number;
  accountCode: string;
  name: string;
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

export async function fetchAccountCategoryOptions(): Promise<AccountCategoryOption[]> {
  const res = await client.get<ApiResponse<AccountCategoryOption[]>>(
    `${BASE}/account-categories`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처유형마스터 조회에 실패했습니다');
  }
  return res.data.data;
}
