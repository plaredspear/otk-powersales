import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: { code: string; message: string };
}

interface AlternativeHolidayItemRaw {
  id: number;
  employee_id: string;
  employee_name: string;
  org_name: string | null;
  actual_work_date: string;
  target_alt_holiday_date: string;
  confirm_alt_holiday_date: string | null;
  status: string;
  change_reason: string | null;
  created_by: string;
  created_at: string;
}

interface AlternativeHolidayCreateResultRaw {
  id: number;
  status: string;
}

interface AlternativeHolidayApproveResultRaw {
  id: number;
  status: string;
  confirm_alt_holiday_date: string | null;
}

interface AlternativeHolidayRejectResultRaw {
  id: number;
  status: string;
}

// --- Frontend interfaces (camelCase) ---

export interface AlternativeHolidayItem {
  id: number;
  employeeId: string;
  employeeName: string;
  orgName: string | null;
  actualWorkDate: string;
  targetAltHolidayDate: string;
  confirmAltHolidayDate: string | null;
  status: string;
  changeReason: string | null;
  createdBy: string;
  createdAt: string;
}

export interface AlternativeHolidayListParams {
  startDate: string;
  endDate: string;
  status?: string;
  employeeId?: string;
  orgCode?: string;
}

export interface CreateAlternativeHolidayPayload {
  employee_id: string;
  actual_work_date: string;
  target_alt_holiday_date: string;
}

export interface ApproveAlternativeHolidayPayload {
  confirm_alt_holiday_date?: string | null;
}

export interface RejectAlternativeHolidayPayload {
  change_reason: string;
}

// --- Mapper ---

function mapItem(raw: AlternativeHolidayItemRaw): AlternativeHolidayItem {
  return {
    id: raw.id,
    employeeId: raw.employee_id,
    employeeName: raw.employee_name,
    orgName: raw.org_name,
    actualWorkDate: raw.actual_work_date,
    targetAltHolidayDate: raw.target_alt_holiday_date,
    confirmAltHolidayDate: raw.confirm_alt_holiday_date,
    status: raw.status,
    changeReason: raw.change_reason,
    createdBy: raw.created_by,
    createdAt: raw.created_at,
  };
}

// --- API functions ---

export async function fetchAlternativeHolidays(
  params: AlternativeHolidayListParams,
): Promise<AlternativeHolidayItem[]> {
  const queryParams: Record<string, string> = {
    startDate: params.startDate,
    endDate: params.endDate,
  };
  if (params.status) queryParams.status = params.status;
  if (params.employeeId) queryParams.employeeId = params.employeeId;
  if (params.orgCode) queryParams.orgCode = params.orgCode;

  const res = await client.get<ApiResponse<AlternativeHolidayItemRaw[]>>(
    '/api/v1/admin/alternative-holidays',
    { params: queryParams },
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '목록 조회에 실패했습니다');
  }

  return res.data.data.map(mapItem);
}

export async function createAlternativeHoliday(
  payload: CreateAlternativeHolidayPayload,
): Promise<{ id: number; status: string }> {
  const res = await client.post<ApiResponse<AlternativeHolidayCreateResultRaw>>(
    '/api/v1/admin/alternative-holidays',
    payload,
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '신청에 실패했습니다');
  }

  return { id: res.data.data.id, status: res.data.data.status };
}

export async function approveAlternativeHoliday(
  id: number,
  payload: ApproveAlternativeHolidayPayload,
): Promise<{ id: number; status: string; confirmAltHolidayDate: string | null }> {
  const res = await client.post<ApiResponse<AlternativeHolidayApproveResultRaw>>(
    `/api/v1/admin/alternative-holidays/${id}/approve`,
    payload,
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '승인에 실패했습니다');
  }

  return {
    id: res.data.data.id,
    status: res.data.data.status,
    confirmAltHolidayDate: res.data.data.confirm_alt_holiday_date,
  };
}

export async function rejectAlternativeHoliday(
  id: number,
  payload: RejectAlternativeHolidayPayload,
): Promise<{ id: number; status: string }> {
  const res = await client.post<ApiResponse<AlternativeHolidayRejectResultRaw>>(
    `/api/v1/admin/alternative-holidays/${id}/reject`,
    payload,
  );

  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '반려에 실패했습니다');
  }

  return { id: res.data.data.id, status: res.data.data.status };
}
