import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `HomeResponse.TeamMemberScheduleInfo` */
export interface HomeSchedule {
  scheduleId: number;
  displayWorkScheduleId: number | null;
  employeeName: string;
  employeeCode: string;
  accountName: string | null;
  accountId: number | null;
  workCategory: string;
  workType: string | null;
  isCommuteRegistered: boolean;
  commuteRegisteredAt: string | null;
}

export interface HomeAttendanceSummary {
  totalCount: number;
  registeredCount: number;
}

export interface HomeExpiryAlert {
  branchName: string;
  employeeName: string;
  employeeCode: string;
  expiryCount: number;
}

export interface HomeNotice {
  id: number;
  title: string;
  category: string;
  categoryName: string;
  createdAt: string;
}

/** backend `HomeResponse` */
export interface HomeData {
  todaySchedules: HomeSchedule[];
  attendanceSummary: HomeAttendanceSummary;
  safetyCheckRequired: boolean;
  expiryAlert: HomeExpiryAlert | null;
  notices: HomeNotice[];
  currentDate: string;
}

export async function fetchHome(): Promise<HomeData> {
  const res = await client.get<ApiResponse<HomeData>>('/api/v1/mobile/home');
  return unwrap(res, '홈 데이터 조회에 실패했습니다');
}
