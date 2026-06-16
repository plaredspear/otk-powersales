import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Admin 외부 API 호출 이력 조회 API (개발자 도구 — 외부 API 테스트 > 호출 이력).
 *
 * 백엔드 `/api/v1/admin/external-api/**` 호출 — SYSTEM(VIEW_ALL_DATA) 권한 필요.
 * `external_api_log` (SAP/SF/Naver outbound HTTP 호출 공통 로그) 를 조회한다.
 */

export type ExternalApiTargetSystem = 'SAP' | 'SF' | 'NAVER';

export interface ExternalApiLogRow {
  id: number;
  targetSystem: ExternalApiTargetSystem;
  endpointKey: string | null;
  httpMethod: string;
  uri: string;
  httpStatus: number | null;
  success: boolean;
  durationMs: number;
  requestedAt: string;
  completedAt: string;
}

export interface ExternalApiLogDetail extends ExternalApiLogRow {
  errorDetail: string | null;
}

export interface ExternalApiLogListResponse {
  items: ExternalApiLogRow[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

export interface ExternalApiLogQuery {
  targetSystem?: ExternalApiTargetSystem;
  endpointKey?: string;
  success?: boolean;
  httpMethod?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

/** 외부 API 호출 이력 페이지 조회 (필터 조합). */
export async function getExternalApiLogs(
  query: ExternalApiLogQuery = {},
): Promise<ExternalApiLogListResponse> {
  const res = await client.get<ApiResponse<ExternalApiLogListResponse>>(
    '/api/v1/admin/external-api/logs',
    { params: query },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '외부 API 호출 이력 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 외부 API 호출 이력 단건 상세 조회 (errorDetail 포함). */
export async function getExternalApiLogDetail(id: number): Promise<ExternalApiLogDetail> {
  const res = await client.get<ApiResponse<ExternalApiLogDetail>>(
    `/api/v1/admin/external-api/logs/${id}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '외부 API 호출 이력 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

/** 필터 셀렉터용 endpoint key 목록 조회. */
export async function getExternalApiLogKeys(): Promise<string[]> {
  const res = await client.get<ApiResponse<string[]>>(
    '/api/v1/admin/external-api/log-keys',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '외부 API 호출 이력 key 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
