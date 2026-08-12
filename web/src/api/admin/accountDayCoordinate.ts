import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 이동매장 좌표 예외 API (개발자 도구 > 대시보드 > 이동매장 좌표 예외).
 *
 * 백엔드 `GET/POST/DELETE /api/v1/admin/tools/account-day-coordinate` 호출 — 요일에 따라 영업
 * 위치가 바뀌는 거래처의 출근등록 GPS 검증 기준 좌표를 런타임에 조정한다. 설정은 Redis 에 지속
 * 저장되어 앱 재시작 후에도 유지되며, 값이 없으면 백엔드 코드 기본값이 적용된다.
 * 권한: 시스템 관리자 전용 (백엔드에서 profileName='시스템 관리자' 로 가드).
 */

/** 요일 코드 — 백엔드 `java.time.DayOfWeek` 이름과 1:1. */
export type DayOfWeekCode =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const DAY_OF_WEEK_LABELS: Record<DayOfWeekCode, string> = {
  MONDAY: '월요일',
  TUESDAY: '화요일',
  WEDNESDAY: '수요일',
  THURSDAY: '목요일',
  FRIDAY: '금요일',
  SATURDAY: '토요일',
  SUNDAY: '일요일',
};

export interface AccountDayCoordinateResponse {
  /** 예외 대상 거래처코드 (고정). */
  externalKey: string;
  dayOfWeek: DayOfWeekCode;
  latitude: number;
  longitude: number;
  label: string;
  /** Redis 저장값이 있는지. false 면 코드 기본값이 적용 중. */
  customized: boolean;
  defaultDayOfWeek: DayOfWeekCode;
  defaultLatitude: number;
  defaultLongitude: number;
  defaultLabel: string;
}

export interface UpdateAccountDayCoordinateParams {
  dayOfWeek: DayOfWeekCode;
  latitude: number;
  longitude: number;
  /** 서버 로그 추적 단서라 필수. 구분자(`|`)/줄바꿈은 서버에서 거부한다. */
  label: string;
}

const BASE_URL = '/api/v1/admin/tools/account-day-coordinate';

export async function fetchAccountDayCoordinate(): Promise<AccountDayCoordinateResponse> {
  const res = await client.get<ApiResponse<AccountDayCoordinateResponse>>(BASE_URL);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '이동매장 좌표 예외 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function updateAccountDayCoordinate(
  params: UpdateAccountDayCoordinateParams,
): Promise<AccountDayCoordinateResponse> {
  const res = await client.post<ApiResponse<AccountDayCoordinateResponse>>(BASE_URL, params);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '이동매장 좌표 예외 변경에 실패했습니다');
  }
  return res.data.data;
}

/** 저장값을 지워 코드 기본값으로 되돌린다. */
export async function resetAccountDayCoordinate(): Promise<AccountDayCoordinateResponse> {
  const res = await client.delete<ApiResponse<AccountDayCoordinateResponse>>(BASE_URL);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '이동매장 좌표 예외 초기화에 실패했습니다');
  }
  return res.data.data;
}
