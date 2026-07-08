import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 런타임 로그 레벨 관리 API (개발자 도구 > 대시보드 > 로그 레벨).
 *
 * 백엔드 `GET/POST /api/v1/admin/tools/log-levels` 호출 — Spring LoggingSystem 을 통해 메모리상
 * 로그 레벨을 조회/변경한다. 변경은 임시로, 앱 재시작 시 서버 기본값(운영=INFO)으로 복귀한다.
 * 권한: 시스템 관리자 전용 (백엔드에서 profileName='시스템 관리자' 로 가드).
 */

export interface LoggerLevel {
  name: string;
  configuredLevel: string | null;
  effectiveLevel: string | null;
}

export interface LoggerListResponse {
  availableLevels: string[];
  loggers: LoggerLevel[];
}

export async function fetchLogLevels(): Promise<LoggerListResponse> {
  const res = await client.get<ApiResponse<LoggerListResponse>>(
    '/api/v1/admin/tools/log-levels',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '로그 레벨 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 로그 레벨 변경. `level` 이 null 이면 명시 레벨을 제거해 상위 로거로부터 상속 상태로 되돌린다.
 */
export async function updateLogLevel(
  loggerName: string,
  level: string | null,
): Promise<LoggerLevel> {
  const res = await client.post<ApiResponse<LoggerLevel>>(
    '/api/v1/admin/tools/log-levels',
    { loggerName, level },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '로그 레벨 변경에 실패했습니다');
  }
  return res.data.data;
}
