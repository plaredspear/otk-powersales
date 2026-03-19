import client from './client';

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface HolidayMasterRaw {
  id: number;
  holiday_date: string;
  name: string;
  type: string;
}

export interface HolidayMaster {
  id: number;
  holidayDate: string;
  name: string;
  type: string;
}

export interface HolidayMasterRequest {
  holiday_date: string;
  name: string;
  type: string;
}

function mapHolidayMaster(raw: HolidayMasterRaw): HolidayMaster {
  return {
    id: raw.id,
    holidayDate: raw.holiday_date,
    name: raw.name,
    type: raw.type,
  };
}

export async function fetchHolidayMasters(year: number): Promise<HolidayMaster[]> {
  const res = await client.get<ApiResponse<HolidayMasterRaw[]>>(
    `/api/v1/admin/holiday-masters?year=${year}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공휴일 목록 조회에 실패했습니다');
  }
  return res.data.data.map(mapHolidayMaster);
}

export async function createHolidayMaster(data: HolidayMasterRequest): Promise<HolidayMaster> {
  const res = await client.post<ApiResponse<HolidayMasterRaw>>(
    '/api/v1/admin/holiday-masters',
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공휴일 등록에 실패했습니다');
  }
  return mapHolidayMaster(res.data.data);
}

export async function updateHolidayMaster(
  id: number,
  data: HolidayMasterRequest,
): Promise<HolidayMaster> {
  const res = await client.put<ApiResponse<HolidayMasterRaw>>(
    `/api/v1/admin/holiday-masters/${id}`,
    data,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공휴일 수정에 실패했습니다');
  }
  return mapHolidayMaster(res.data.data);
}

export async function deleteHolidayMaster(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/holiday-masters/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '공휴일 삭제에 실패했습니다');
  }
}
