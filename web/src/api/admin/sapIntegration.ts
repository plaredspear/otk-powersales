import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Admin SAP 연동 운영 모니터링 API.
 *
 * 백엔드 `/api/v1/admin/sap-integration/**` 호출 — `SAP_INTEGRATION_READ` 권한 필요
 * (`SYSTEM_ADMIN` role 만 보유).
 */

export type OutboundTriggerType = 'BATCH' | 'REALTIME' | 'OUTBOX';
export type SapOutboundResultCode = 'SUCCESS' | 'FAIL' | 'INVALID_RESPONSE';

export interface SapInboundCatalogItem {
  endpointPath: string;
  koreanName: string;
  requiredScope: string;
  targetEntity: string;
  controllerClass: string;
  description: string;
}

export interface SapInboundAuditRow {
  id: number;
  eventType: string;
  clientId: string | null;
  endpoint: string | null;
  httpMethod: string | null;
  clientIp: string;
  scope: string | null;
  receivedCount: number | null;
  previousCount: number | null;
  reason: string | null;
  createdAt: string;
}

export type SapInboundAuditDetail = SapInboundAuditRow;

export interface SapInboundAuditListResponse {
  items: SapInboundAuditRow[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

export interface SapInboundAuditQuery {
  clientId?: string;
  eventType?: string;
  endpoint?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface SapOutboundCatalogItem {
  interfaceId: string;
  koreanName: string;
  triggerType: OutboundTriggerType;
  senderClass: string;
  description: string;
}

export interface SapOutboundLogRow {
  id: number;
  interfaceId: string;
  endpointPath: string;
  requestCount: number;
  httpStatus: number | null;
  resultCode: SapOutboundResultCode | null;
  resultMsg: string | null;
  attemptCount: number;
  durationMs: number;
  requestedAt: string;
  completedAt: string;
}

export interface SapOutboundLogDetail extends SapOutboundLogRow {
  errorDetail: string | null;
}

export interface SapOutboundLogListResponse {
  items: SapOutboundLogRow[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

export interface SapOutboundLogQuery {
  interfaceId?: string;
  resultCode?: SapOutboundResultCode;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface SapOutboxPendingRow {
  id: number;
  domainType: string;
  aggregateId: number;
  interfaceId: string;
  status: string;
  retryCount: number;
  lastError: string | null;
  createdAt: string | null;
  sentAt: string | null;
}

export interface SapOutboxPendingListResponse {
  items: SapOutboxPendingRow[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

/** SAP 인바운드 9개 endpoint 정적 카탈로그 조회. */
export async function getSapInboundCatalog(): Promise<SapInboundCatalogItem[]> {
  const res = await client.get<ApiResponse<SapInboundCatalogItem[]>>(
    '/api/v1/admin/sap-integration/inbound/catalog',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 인바운드 카탈로그 조회에 실패했습니다');
  }
  return res.data.data;
}

/** SAP 인바운드 audit 이력 페이지 조회 (필터 조합). */
export async function getSapInboundAudits(
  query: SapInboundAuditQuery = {},
): Promise<SapInboundAuditListResponse> {
  const res = await client.get<ApiResponse<SapInboundAuditListResponse>>(
    '/api/v1/admin/sap-integration/inbound/audits',
    { params: query },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 인바운드 호출 이력 조회에 실패했습니다');
  }
  return res.data.data;
}

/** SAP 인바운드 audit 단건 상세 조회. */
export async function getSapInboundAuditDetail(id: number): Promise<SapInboundAuditDetail> {
  const res = await client.get<ApiResponse<SapInboundAuditDetail>>(
    `/api/v1/admin/sap-integration/inbound/audits/${id}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 인바운드 호출 이력 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

/** SAP 아웃바운드 7개 interface 정적 카탈로그 조회. */
export async function getSapOutboundCatalog(): Promise<SapOutboundCatalogItem[]> {
  const res = await client.get<ApiResponse<SapOutboundCatalogItem[]>>(
    '/api/v1/admin/sap-integration/outbound/catalog',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 아웃바운드 카탈로그 조회에 실패했습니다');
  }
  return res.data.data;
}

/** SAP 아웃바운드 호출 이력 페이지 조회 (필터 조합). */
export async function getSapOutboundLogs(
  query: SapOutboundLogQuery = {},
): Promise<SapOutboundLogListResponse> {
  const res = await client.get<ApiResponse<SapOutboundLogListResponse>>(
    '/api/v1/admin/sap-integration/outbound/logs',
    { params: query },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 아웃바운드 호출 이력 조회에 실패했습니다');
  }
  return res.data.data;
}

/** SAP 아웃바운드 호출 이력 단건 상세 조회 (errorDetail 포함). */
export async function getSapOutboundLogDetail(id: number): Promise<SapOutboundLogDetail> {
  const res = await client.get<ApiResponse<SapOutboundLogDetail>>(
    `/api/v1/admin/sap-integration/outbound/logs/${id}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 아웃바운드 호출 이력 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

/** SAP 아웃박스 대기 (PENDING + RETRY) 페이지 조회. */
export async function getSapOutboundOutboxPending(
  page: number = 1,
  size: number = 20,
): Promise<SapOutboxPendingListResponse> {
  const res = await client.get<ApiResponse<SapOutboxPendingListResponse>>(
    '/api/v1/admin/sap-integration/outbound/outbox-pending',
    { params: { page, size } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SAP 아웃박스 대기 큐 조회에 실패했습니다');
  }
  return res.data.data;
}
