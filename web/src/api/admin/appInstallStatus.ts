import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 앱 설치 현황 API (개발자 도구 > 대시보드 > 기능 활성화).
 *
 * 백엔드 `GET /api/v1/admin/tools/app-install/uninstalled-female-staff` 호출 — 앱을 한 번도 사용한
 * 흔적이 없는(= 미설치 추정) 여사원 인원과 안내 대상 모수를 반환한다.
 * 권한: 시스템 관리자 전용 (백엔드에서 profileName='시스템 관리자' 로 가드).
 */

export interface AppUninstalledFemaleStaffSummary {
  /** 앱 사용 흔적이 없는 여사원 수. 엑셀 다운로드 행 수와 동일. */
  uninstalledCount: number;
  /** 안내 대상 모수 — 재직 + 앱 로그인 활성 여사원(여사원 현황 화면과 동일 모수). */
  targetCount: number;
}

/**
 * 앱 미설치 추정 여사원 집계 조회.
 *
 * ApiResponse 의 data 만 언래핑하여 반환. 실패 시 서버 메시지로 Error throw.
 */
export async function fetchUninstalledFemaleStaffSummary(): Promise<AppUninstalledFemaleStaffSummary> {
  const res = await client.get<ApiResponse<AppUninstalledFemaleStaffSummary>>(
    '/api/v1/admin/tools/app-install/uninstalled-female-staff',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '앱 미설치 현황 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 미설치 추정 여사원 명단 엑셀 다운로드 경로 (사번 / 이름 / 지점명). */
export const UNINSTALLED_FEMALE_STAFF_EXPORT_PATH =
  '/api/v1/admin/tools/app-install/uninstalled-female-staff/export';
