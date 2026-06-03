import client from './client';
import { AxiosError } from 'axios';
import type { ApiResponse } from './types';


export interface PromotionEmployee {
  id: number;
  name: string | null;
  promotionId: number;
  employeeId: number | null;
  employeeCode: string | null;
  employeeName: string | null;
  scheduleDate: string | null;
  workStatus: string | null;
  workType1: string | null;
  workType3: string | null;
  workType4: string | null;
  professionalPromotionTeam: string | null;
  scheduleId: number | null;
  promoCloseByTm: boolean;
  basePrice: number | null;
  dailyTargetCount: number | null;
  targetAmount: number | null;
  actualAmount: number | null;
  primaryProductAmount: number | null;
  primarySalesQuantity: number | null;
  primarySalesPrice: number | null;
  otherSalesAmount: number | null;
  otherSalesQuantity: number | null;
  s3ImageUniqueKey: string | null;
}

export interface PromotionEmployeeFormData {
  employeeId?: number | null;
  scheduleDate?: string | null;
  workStatus?: string | null;
  workType1?: string | null;
  workType3?: string | null;
  workType4?: string | null;
  professionalPromotionTeam?: string | null;
  basePrice?: number | null;
  dailyTargetCount?: number | null;
}

// --- Batch update interfaces ---

export interface BatchUpdatePromotionEmployeeItem {
  id: number;
  employeeId: number | null;
  scheduleDate: string;
  workStatus: string;
  workType1: string;
  workType3: string | null;
  workType4?: string | null;
  professionalPromotionTeam?: string | null;
  basePrice?: number | null;
  dailyTargetCount?: number | null;
  targetAmount?: number | null;
  actualAmount?: number | null;
  primaryProductAmount?: number | null;
  primarySalesQuantity?: number | null;
  otherSalesAmount?: number | null;
  otherSalesQuantity?: number | null;
  s3ImageUniqueKey?: string | null;
  promoCloseByTm?: boolean;
}

export interface BatchUpdatePromotionEmployeeRequest {
  items: BatchUpdatePromotionEmployeeItem[];
}


export interface BatchUpdatePromotionEmployeeResponse {
  updatedCount: number;
  items: PromotionEmployee[];
}

export interface BatchItemError {
  itemIndex: number;
  employeeId: number | null;
  errorCode: string;
  message: string;
}

export interface BatchValidationErrorResponse {
  errorCode: string;
  message: string;
  detail: {
    errors: BatchItemError[];
  };
}

export class BatchValidationError extends Error {
  errors: BatchItemError[];

  constructor(response: BatchValidationErrorResponse) {
    super(response.message);
    this.errors = response.detail.errors;
  }
}


// --- Confirm response interfaces ---


export interface PromotionConfirmResponse {
  promotionId: number;
  totalEmployees: number;
  upsertedSchedules: number;
}

// --- API functions ---

export async function fetchPromotionEmployees(promotionId: number): Promise<PromotionEmployee[]> {
  const res = await client.get<ApiResponse<PromotionEmployee[]>>(
    `/api/v1/admin/promotions/${promotionId}/employees`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사조원 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createPromotionEmployee(
  promotionId: number,
  data?: PromotionEmployeeFormData,
): Promise<PromotionEmployee> {
  const res = await client.post<ApiResponse<PromotionEmployee>>(
    `/api/v1/admin/promotions/${promotionId}/employees`,
    data ?? {},
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사조원 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updatePromotionEmployee(
  id: number,
  data: PromotionEmployeeFormData,
): Promise<PromotionEmployee> {
  const res = await client.put<ApiResponse<PromotionEmployee>>(
    `/api/v1/admin/promotion-employees/${id}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사조원 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deletePromotionEmployee(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/promotion-employees/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '행사조원 삭제에 실패했습니다');
  }
}

export async function confirmPromotionSchedule(
  promotionId: number,
): Promise<PromotionConfirmResponse> {
  try {
    const res = await client.post<ApiResponse<PromotionConfirmResponse>>(
      `/api/v1/admin/promotions/${promotionId}/confirm`,
    );
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.message || '스케줄 확정에 실패했습니다');
    }
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 400) {
      const errorMessage = (err.response.data as ApiResponse<unknown>)?.error?.message;
      throw new Error(errorMessage || '스케줄 확정에 실패했습니다');
    }
    throw err;
  }
}

export async function batchUpdatePromotionEmployees(
  promotionId: number,
  data: BatchUpdatePromotionEmployeeRequest,
): Promise<BatchUpdatePromotionEmployeeResponse> {
  try {
    const res = await client.put<ApiResponse<BatchUpdatePromotionEmployeeResponse>>(
      `/api/v1/admin/promotions/${promotionId}/employees/batch`,
      data,
    );
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.message || '행사조원 일괄 수정에 실패했습니다');
    }
    return res.data.data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 400) {
      const errData = err.response.data as BatchValidationErrorResponse;
      if (errData.errorCode === 'BATCH_VALIDATION_FAILED') {
        throw new BatchValidationError(errData);
      }
    }
    throw err;
  }
}
