import client from './client';
import type { PPTTeamType } from '@/constants/pptTeamType';

// --- ApiResponse wrapper ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: {
    code: string;
    message: string;
    missing_ids?: number[];
  };
}

// --- Raw response interfaces (snake_case from backend) ---

interface PromotionScheduleItemRaw {
  schedule_id: number;
  working_date: string;
  account_id: number;
  account_code: string | null;
  account_name: string;
  working_category1: string | null;
  working_category3: string | null;
  working_category4: string | null;
}

interface PromotionScheduleMemberRaw {
  promotion_employee_id: number;
  employee_id: number;
  employee_number: string;
  employee_name: string;
  professional_promotion_team: string | null;
  schedules: PromotionScheduleItemRaw[];
}

interface PromotionScheduleListResponseRaw {
  promotion_id: number;
  promotion_name: string | null;
  schedule_period: { start_date: string; end_date: string };
  members: PromotionScheduleMemberRaw[];
  total_member_count: number;
  total_schedule_count: number;
}

interface PromotionScheduleBulkUpdateResponseRaw {
  updated_count: number;
  schedule_ids: number[];
}

interface PromotionScheduleBulkDeleteResponseRaw {
  deleted_count: number;
}

// --- Frontend interfaces (camelCase) ---

export type WorkingCategory1 = '행사' | '진열';
export type WorkingCategory3 = '고정' | '순회' | '격고';

export interface PromotionScheduleItem {
  scheduleId: number;
  workingDate: string;
  accountId: number;
  accountCode: string | null;
  accountName: string;
  workingCategory1: string | null;
  workingCategory3: string | null;
  workingCategory4: string | null;
}

export interface PromotionScheduleMember {
  promotionEmployeeId: number;
  employeeId: number;
  employeeNumber: string;
  employeeName: string;
  professionalPromotionTeam: PPTTeamType | null;
  schedules: PromotionScheduleItem[];
}

export interface PromotionScheduleList {
  promotionId: number;
  promotionName: string | null;
  schedulePeriod: { startDate: string; endDate: string };
  members: PromotionScheduleMember[];
  totalMemberCount: number;
  totalScheduleCount: number;
}

export interface PromotionScheduleBulkUpdateItem {
  schedule_id: number;
  account_id: number;
  working_date: string;
  working_category1: WorkingCategory1;
  working_category3: WorkingCategory3;
  working_category4?: string | null;
}

export interface PromotionScheduleBulkUpdateRequest {
  items: PromotionScheduleBulkUpdateItem[];
}

export interface PromotionScheduleBulkUpdateResult {
  updatedCount: number;
  scheduleIds: number[];
}

export interface PromotionScheduleBulkDeleteRequest {
  schedule_ids: number[];
}

export interface PromotionScheduleBulkDeleteResult {
  deletedCount: number;
}

export interface FetchPromotionSchedulesParams {
  startDate?: string;
  endDate?: string;
}

// --- Mappers ---

function mapItem(raw: PromotionScheduleItemRaw): PromotionScheduleItem {
  return {
    scheduleId: raw.schedule_id,
    workingDate: raw.working_date,
    accountId: raw.account_id,
    accountCode: raw.account_code,
    accountName: raw.account_name,
    workingCategory1: raw.working_category1,
    workingCategory3: raw.working_category3,
    workingCategory4: raw.working_category4,
  };
}

function mapMember(raw: PromotionScheduleMemberRaw): PromotionScheduleMember {
  return {
    promotionEmployeeId: raw.promotion_employee_id,
    employeeId: raw.employee_id,
    employeeNumber: raw.employee_number,
    employeeName: raw.employee_name,
    professionalPromotionTeam: raw.professional_promotion_team as PPTTeamType | null,
    schedules: raw.schedules.map(mapItem),
  };
}

function mapList(raw: PromotionScheduleListResponseRaw): PromotionScheduleList {
  return {
    promotionId: raw.promotion_id,
    promotionName: raw.promotion_name,
    schedulePeriod: {
      startDate: raw.schedule_period.start_date,
      endDate: raw.schedule_period.end_date,
    },
    members: raw.members.map(mapMember),
    totalMemberCount: raw.total_member_count,
    totalScheduleCount: raw.total_schedule_count,
  };
}

// --- API functions ---

export async function fetchPromotionSchedules(
  promotionId: number,
  params: FetchPromotionSchedulesParams = {},
): Promise<PromotionScheduleList> {
  const queryParams: Record<string, string> = {};
  if (params.startDate) queryParams.startDate = params.startDate;
  if (params.endDate) queryParams.endDate = params.endDate;

  const res = await client.get<ApiResponse<PromotionScheduleListResponseRaw>>(
    `/api/v1/admin/promotions/${promotionId}/schedules`,
    { params: queryParams },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사 일정 조회에 실패했습니다');
  }
  return mapList(res.data.data);
}

export async function bulkUpdatePromotionSchedules(
  promotionId: number,
  data: PromotionScheduleBulkUpdateRequest,
): Promise<PromotionScheduleBulkUpdateResult> {
  const res = await client.put<ApiResponse<PromotionScheduleBulkUpdateResponseRaw>>(
    `/api/v1/admin/promotions/${promotionId}/schedules/bulk`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일정 일괄 변경에 실패했습니다');
  }
  return {
    updatedCount: res.data.data.updated_count,
    scheduleIds: res.data.data.schedule_ids,
  };
}

export async function bulkDeletePromotionSchedules(
  promotionId: number,
  data: PromotionScheduleBulkDeleteRequest,
): Promise<PromotionScheduleBulkDeleteResult> {
  const res = await client.delete<ApiResponse<PromotionScheduleBulkDeleteResponseRaw>>(
    `/api/v1/admin/promotions/${promotionId}/schedules/bulk`,
    { data },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일정 일괄 삭제에 실패했습니다');
  }
  return {
    deletedCount: res.data.data.deleted_count,
  };
}
