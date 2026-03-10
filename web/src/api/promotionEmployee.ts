import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface PromotionEmployeeRaw {
  id: number;
  promotion_id: number;
  employee_sfid: string;
  employee_name: string | null;
  schedule_date: string;
  work_status: string;
  work_type1: string;
  work_type3: string;
  work_type4: string | null;
  professional_promotion_team: string | null;
  schedule_id: number | null;
  promo_close_by_tm: boolean;
  base_price: number | null;
  daily_target_count: number | null;
}

// --- Frontend interfaces (camelCase) ---

export interface PromotionEmployee {
  id: number;
  promotionId: number;
  employeeSfid: string;
  employeeName: string | null;
  scheduleDate: string;
  workStatus: string;
  workType1: string;
  workType3: string;
  workType4: string | null;
  professionalPromotionTeam: string | null;
  scheduleId: number | null;
  promoCloseByTm: boolean;
  basePrice: number | null;
  dailyTargetCount: number | null;
}

export interface PromotionEmployeeFormData {
  employee_sfid: string;
  schedule_date: string;
  work_status: string;
  work_type1: string;
  work_type3: string;
  work_type4?: string | null;
  professional_promotion_team?: string | null;
  base_price?: number | null;
  daily_target_count?: number | null;
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
  };
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
  data: PromotionEmployeeFormData,
): Promise<PromotionEmployee> {
  const res = await client.post<ApiResponse<PromotionEmployeeRaw>>(
    `/api/v1/admin/promotions/${promotionId}/employees`,
    data,
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
