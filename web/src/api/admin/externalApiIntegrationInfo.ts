import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 외부 API 연동 정보 조회 API (개발자 도구 — 외부 API 테스트).
 *
 * 각 외부 API 탭이 외부 시스템 endpoint / HTTP method / 인증 방식을 노출하도록 backend 가 현재 환경의
 * 실제 설정값을 조회해 반환한다. `key` 로 각 탭과 매칭한다.
 * 권한 SYSTEM(MODIFY_ALL_DATA) 필요.
 */

export interface ExternalApiIntegrationInfo {
  key: string;
  externalSystem: string;
  endpoint: string;
  httpMethod: string;
  authType: string;
  note: string;
}

export interface ExternalApiIntegrationInfoResponse {
  items: ExternalApiIntegrationInfo[];
}

export async function fetchExternalApiIntegrationInfo(): Promise<ExternalApiIntegrationInfoResponse> {
  const res = await client.get<ApiResponse<ExternalApiIntegrationInfoResponse>>(
    '/api/v1/admin/external-api/integration-info',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '외부 API 연동 정보 조회에 실패했습니다');
  }
  return res.data.data;
}
