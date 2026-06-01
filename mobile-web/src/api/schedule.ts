import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `WorkDayDto` */
export interface WorkDay {
  date: string; // YYYY-MM-DD
  hasWork: boolean;
  workingType: string | null;
}

/** backend `MonthlyScheduleResponse` */
export interface MonthlySchedule {
  year: number;
  month: number;
  workDays: WorkDay[];
  annualLeaveCount: number;
  substituteHolidayCount: number;
}

/** backend `DisplayWorkScheduleItemDto` */
export interface DailyScheduleAccount {
  accountId: number;
  accountName: string;
  workType1: string;
  workType2: string;
  workType3: string;
  isRegistered: boolean;
}

/** backend `ReportProgressDto` */
export interface ReportProgress {
  completed: number;
  total: number;
  workType: string;
}

/** backend `DailyScheduleResponse` */
export interface DailySchedule {
  date: string;
  dayOfWeek: string;
  memberName: string;
  employeeCode: string;
  workingType: string | null;
  reportProgress: ReportProgress;
  accounts: DailyScheduleAccount[];
}

export async function fetchMonthlySchedule(year: number, month: number): Promise<MonthlySchedule> {
  const res = await client.get<ApiResponse<MonthlySchedule>>(
    '/api/v1/mobile/mypage/schedule/monthly',
    { params: { year, month } }
  );
  return unwrap(res, '월간 일정 조회에 실패했습니다');
}

export async function fetchDailySchedule(date: string): Promise<DailySchedule> {
  const res = await client.get<ApiResponse<DailySchedule>>(
    '/api/v1/mobile/mypage/schedule/daily',
    { params: { date } }
  );
  return unwrap(res, '일간 일정 조회에 실패했습니다');
}
