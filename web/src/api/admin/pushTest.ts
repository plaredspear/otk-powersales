import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * FCM push 발송 테스트 API (개발자 도구 > 외부 API 테스트 > push 발송 테스트 탭).
 *
 * 백엔드 `POST /api/v1/admin/push/test` 호출 — 입력 사번에 등록된 FCM 토큰으로 임의 제목/본문의
 * 테스트 알림을 1건 발송한다. 실제 발송 여부는 서버의 FCM 활성 설정(app.push.fcm.enabled) +
 * credential 주입 + 운영(!local) 프로필에 좌우된다. 권한 `MODIFY_ALL_DATA`(SYSTEM_ADMIN) 필요.
 */

export interface PushTestResponse {
  employeeCode: string;
  employeeName: string | null;
  tokenRegistered: boolean;
  maskedToken: string | null;
  successCount: number;
  failureCount: number;
  message: string;
}

export interface PushTestRequest {
  employeeCode: string;
  title: string;
  body: string;
}

export async function postPushTest(
  payload: PushTestRequest,
): Promise<PushTestResponse> {
  const res = await client.post<ApiResponse<PushTestResponse>>(
    '/api/v1/admin/push/test',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'push 발송 테스트에 실패했습니다');
  }
  return res.data.data;
}
