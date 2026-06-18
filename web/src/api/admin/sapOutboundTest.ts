import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Admin SAP outbound 테스트 API — 7개 sender 모두에 preview / send.
 *
 * 백엔드 `/api/v1/admin/sap-integration/outbound/test/**` 호출.
 * - preview: `VIEW_ALL_DATA` 권한
 * - send: `MODIFY_ALL_DATA` 권한 (SYSTEM_ADMIN)
 */

export interface SapOutboundTestPreviewResponse {
  interfaceId: string;
  endpointPath: string;
  payload: unknown;
  summary: string;
}

export interface SapOutboundTestSendResponse {
  interfaceId: string;
  success: boolean;
  message: string;
  sapOutboundLogId: number | null;
  sapOutboxId: number | null;
  result: unknown;
}

export type SapOutboundTestKind =
  | 'loan-inquiry'
  | 'order-request-detail'
  | 'order-request-cancel'
  | 'order-request-register'
  | 'attendance'
  | 'display-master'
  | 'ppt-master';

async function call<Req, Res>(
  kind: SapOutboundTestKind,
  action: 'preview' | 'send',
  body: Req,
): Promise<Res> {
  const res = await client.post<ApiResponse<Res>>(
    `/api/v1/admin/sap-integration/outbound/test/${kind}/${action}`,
    body,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP outbound 테스트 호출에 실패했습니다');
  }
  return res.data.data;
}

export function previewSapOutbound<Req extends object>(
  kind: SapOutboundTestKind,
  body: Req,
): Promise<SapOutboundTestPreviewResponse> {
  return call(kind, 'preview', body);
}

export function sendSapOutbound<Req extends object>(
  kind: SapOutboundTestKind,
  body: Req,
): Promise<SapOutboundTestSendResponse> {
  return call(kind, 'send', body);
}

/**
 * 근태(일반 출근) outbound 인터페이스(SD03130)에 조회 없이 빈 배열을 실제 SAP 으로 송신한다.
 * outbound API 의 연결/응답 정상 여부 확인 전용.
 */
export async function sendAttendanceEmpty(): Promise<SapOutboundTestSendResponse> {
  const res = await client.post<ApiResponse<SapOutboundTestSendResponse>>(
    '/api/v1/admin/sap-integration/outbound/test/attendance/send-empty',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '빈 배열 송신 호출에 실패했습니다');
  }
  return res.data.data;
}
