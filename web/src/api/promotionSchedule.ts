import client from './client';
import type { PPTTeamType } from '@/constants/pptTeamType';
import type { ApiResponse } from './types';

// --- ApiResponse wrapper ---

// --- Raw response interfaces (snake_case from backend) ---


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
  scheduleId: number;
  accountId: number;
  workingDate: string;
  workingCategory1: WorkingCategory1;
  workingCategory3: WorkingCategory3;
  workingCategory4?: string | null;
}

export interface PromotionScheduleBulkUpdateRequest {
  items: PromotionScheduleBulkUpdateItem[];
}

export interface PromotionScheduleBulkUpdateResult {
  updatedCount: number;
  scheduleIds: number[];
}

export interface PromotionScheduleBulkDeleteRequest {
  scheduleIds: number[];
}

export interface PromotionScheduleBulkDeleteResult {
  deletedCount: number;
}

export interface FetchPromotionSchedulesParams {
  startDate?: string;
  endDate?: string;
}


// --- API functions ---

export async function fetchPromotionSchedules(
  promotionId: number,
  params: FetchPromotionSchedulesParams = {},
): Promise<PromotionScheduleList> {
  const queryParams: Record<string, string> = {};
  if (params.startDate) queryParams.startDate = params.startDate;
  if (params.endDate) queryParams.endDate = params.endDate;

  const res = await client.get<ApiResponse<PromotionScheduleList>>(
    `/api/v1/admin/promotions/${promotionId}/schedules`,
    { params: queryParams },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사 일정 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function bulkUpdatePromotionSchedules(
  promotionId: number,
  data: PromotionScheduleBulkUpdateRequest,
): Promise<PromotionScheduleBulkUpdateResult> {
  const res = await client.put<ApiResponse<PromotionScheduleBulkUpdateResult>>(
    `/api/v1/admin/promotions/${promotionId}/schedules/bulk`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일정 일괄 변경에 실패했습니다');
  }
  return res.data.data;
}

export async function bulkDeletePromotionSchedules(
  promotionId: number,
  data: PromotionScheduleBulkDeleteRequest,
): Promise<PromotionScheduleBulkDeleteResult> {
  const res = await client.delete<ApiResponse<PromotionScheduleBulkDeleteResult>>(
    `/api/v1/admin/promotions/${promotionId}/schedules/bulk`,
    { data },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '일정 일괄 삭제에 실패했습니다');
  }
  return res.data.data;
}
