import client from './client';
import { AxiosError } from 'axios';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface PromotionEmployeeRaw {
  id: number;
  promotion_id: number;
  employee_sfid: string | null;
  employee_name: string | null;
  schedule_date: string | null;
  work_status: string | null;
  work_type1: string | null;
  work_type3: string | null;
  work_type4: string | null;
  professional_promotion_team: string | null;
  schedule_id: number | null;
  promo_close_by_tm: boolean;
  base_price: number | null;
  daily_target_count: number | null;
  target_amount: number | null;
  actual_amount: number | null;
  primary_product_amount: number | null;
  primary_sales_quantity: number | null;
  primary_sales_price: number | null;
  other_sales_amount: number | null;
  other_sales_quantity: number | null;
  s3_image_unique_key: string | null;
}

// --- Frontend interfaces (camelCase) ---

export interface PromotionEmployee {
  id: number;
  promotionId: number;
  employeeSfid: string | null;
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
  employee_sfid?: string | null;
  schedule_date?: string | null;
  work_status?: string | null;
  work_type1?: string | null;
  work_type3?: string | null;
  work_type4?: string | null;
  professional_promotion_team?: string | null;
  base_price?: number | null;
  daily_target_count?: number | null;
}

// --- Batch update interfaces ---

export interface BatchUpdatePromotionEmployeeItem {
  id: number;
  employee_sfid: string | null;
  schedule_date: string;
  work_status: string;
  work_type1: string;
  work_type3: string | null;
  work_type4?: string | null;
  professional_promotion_team?: string | null;
  base_price?: number | null;
  daily_target_count?: number | null;
  target_amount?: number | null;
  actual_amount?: number | null;
}

export interface BatchUpdatePromotionEmployeeRequest {
  items: BatchUpdatePromotionEmployeeItem[];
}

interface BatchUpdatePromotionEmployeeResponseRaw {
  updated_count: number;
  items: PromotionEmployeeRaw[];
}

export interface BatchUpdatePromotionEmployeeResponse {
  updatedCount: number;
  items: PromotionEmployee[];
}

export interface BatchItemError {
  item_index: number;
  employee_sfid: string | null;
  error_code: string;
  message: string;
}

export interface BatchValidationErrorResponse {
  error_code: string;
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

// --- Mapper ---

function mapPromotionEmployee(raw: PromotionEmployeeRaw): PromotionEmployee {
  return {
    id: raw.id,
    promotionId: raw.promotion_id,
    employeeSfid: raw.employee_sfid,
    employeeName: raw.employee_name,
    scheduleDate: raw.schedule_date,
    workStatus: raw.work_status,
    workType1: raw.work_type1,
    workType3: raw.work_type3,
    workType4: raw.work_type4,
    professionalPromotionTeam: raw.professional_promotion_team,
    scheduleId: raw.schedule_id,
    promoCloseByTm: raw.promo_close_by_tm,
    basePrice: raw.base_price,
    dailyTargetCount: raw.daily_target_count,
    targetAmount: raw.target_amount,
    actualAmount: raw.actual_amount,
    primaryProductAmount: raw.primary_product_amount,
    primarySalesQuantity: raw.primary_sales_quantity,
    primarySalesPrice: raw.primary_sales_price,
    otherSalesAmount: raw.other_sales_amount,
    otherSalesQuantity: raw.other_sales_quantity,
    s3ImageUniqueKey: raw.s3_image_unique_key,
  };
}

// --- Confirm response interfaces ---

interface PromotionConfirmResponseRaw {
  promotion_id: number;
  total_employees: number;
  upserted_schedules: number;
}

export interface PromotionConfirmResponse {
  promotionId: number;
  totalEmployees: number;
  upsertedSchedules: number;
}

// --- API functions ---

export async function fetchPromotionEmployees(promotionId: number): Promise<PromotionEmployee[]> {
  const res = await client.get<ApiResponse<PromotionEmployeeRaw[]>>(
    `/api/v1/admin/promotions/${promotionId}/employees`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사조원 목록 조회에 실패했습니다');
  }
  return res.data.data.map(mapPromotionEmployee);
}

export async function createPromotionEmployee(
  promotionId: number,
  data?: PromotionEmployeeFormData,
): Promise<PromotionEmployee> {
  const res = await client.post<ApiResponse<PromotionEmployeeRaw>>(
    `/api/v1/admin/promotions/${promotionId}/employees`,
    data ?? {},
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사조원 등록에 실패했습니다');
  }
  return mapPromotionEmployee(res.data.data);
}

export async function updatePromotionEmployee(
  id: number,
  data: PromotionEmployeeFormData,
): Promise<PromotionEmployee> {
  const res = await client.put<ApiResponse<PromotionEmployeeRaw>>(
    `/api/v1/admin/promotion-employees/${id}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사조원 수정에 실패했습니다');
  }
  return mapPromotionEmployee(res.data.data);
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
  const res = await client.post<ApiResponse<PromotionConfirmResponseRaw>>(
    `/api/v1/admin/promotions/${promotionId}/confirm`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '스케줄 확정에 실패했습니다');
  }
  const raw = res.data.data;
  return {
    promotionId: raw.promotion_id,
    totalEmployees: raw.total_employees,
    upsertedSchedules: raw.upserted_schedules,
  };
}

export async function batchUpdatePromotionEmployees(
  promotionId: number,
  data: BatchUpdatePromotionEmployeeRequest,
): Promise<BatchUpdatePromotionEmployeeResponse> {
  try {
    const res = await client.put<ApiResponse<BatchUpdatePromotionEmployeeResponseRaw>>(
      `/api/v1/admin/promotions/${promotionId}/employees/batch`,
      data,
    );
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.message || '행사조원 일괄 수정에 실패했습니다');
    }
    return {
      updatedCount: res.data.data.updated_count,
      items: res.data.data.items.map(mapPromotionEmployee),
    };
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 400) {
      const errData = err.response.data as BatchValidationErrorResponse;
      if (errData.error_code === 'BATCH_VALIDATION_FAILED') {
        throw new BatchValidationError(errData);
      }
    }
    throw err;
  }
}
