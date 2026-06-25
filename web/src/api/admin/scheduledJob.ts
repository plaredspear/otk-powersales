import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Admin 스케줄 잡 실행 이력 조회 API.
 *
 * 백엔드 `/api/v1/admin/scheduled-jobs/**` 호출 — `SCHEDULED_JOB_READ` 권한 필요
 * (`SYSTEM_ADMIN` role 만 보유).
 */

export type ScheduledJobStatus = 'RUNNING' | 'SUCCESS' | 'FAILURE';

export interface ScheduledJobRun {
  id: number;
  jobName: string;
  startedAt: string;
  endedAt: string | null;
  durationMs: number | null;
  status: ScheduledJobStatus;
  errorMessage: string | null;
  metadata: string | null;
}

export interface ScheduledJobRunListResponse {
  items: ScheduledJobRun[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
}

export interface ScheduledJobSummaryResponse {
  windowFrom: string;
  windowTo: string;
  totalCount: number;
  runningCount: number;
  successCount: number;
  failureCount: number;
  distinctJobNames: string[];
}

export interface RegisteredScheduledJob {
  jobName: string;
  cron: string;
  description: string;
  /** 현재 환경에서 해당 배치가 실제로 스케줄링 활성화되어 있는지 여부 (backend 빈 등록 기준). */
  enabled: boolean;
}

export interface ScheduledJobRunsQuery {
  jobName?: string;
  status?: ScheduledJobStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export async function getScheduledJobRuns(
  query: ScheduledJobRunsQuery = {},
): Promise<ScheduledJobRunListResponse> {
  const res = await client.get<ApiResponse<ScheduledJobRunListResponse>>(
    '/api/v1/admin/scheduled-jobs/runs',
    { params: query },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '스케줄 잡 이력 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function getScheduledJobCatalog(): Promise<RegisteredScheduledJob[]> {
  const res = await client.get<ApiResponse<RegisteredScheduledJob[]>>(
    '/api/v1/admin/scheduled-jobs/catalog',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '등록된 작업 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function getScheduledJobSummary(
  windowHours: number = 24,
): Promise<ScheduledJobSummaryResponse> {
  const res = await client.get<ApiResponse<ScheduledJobSummaryResponse>>(
    '/api/v1/admin/scheduled-jobs/summary',
    { params: { windowHours } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '요약 정보 조회에 실패했습니다');
  }
  return res.data.data;
}

export interface OroraMonthlyMaterializeTriggerResponse {
  salesMonth: string;
  fetchedCount: number;
  upsertedCount: number;
  skippedAccountUnmatchedCount: number;
}

export interface ScheduledJobManualTriggerResponse {
  jobName: string;
  status: string;
}

/** 전문행사조(PPT) 마스터 배치 수동 실행 대상. expire=금일 전문행사조 마감, sync=금일 전문행사조 반영. */
export type PptMasterTriggerAction = 'expire' | 'sync';

/**
 * 전문행사조 마스터 배치를 수동 실행한다. 자동 스케줄과 동일하게 이력이 남으므로 실행 후
 * 이력 탭에서 결과를 확인할 수 있다. 사원 행사조 소속을 변경하므로 `MODIFY_ALL_DATA` 권한 필요.
 */
export async function triggerPptMaster(
  action: PptMasterTriggerAction,
): Promise<ScheduledJobManualTriggerResponse> {
  const res = await client.post<ApiResponse<ScheduledJobManualTriggerResponse>>(
    `/api/v1/admin/scheduled-jobs/ppt-master/${action}/trigger`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전문행사조 배치 수동 실행에 실패했습니다');
  }
  return res.data.data;
}

/**
 * ORORA 월매출 적재를 특정 월(`YYYYMM`)로 수동 실행한다 (`salesMonth` 미지정 시 전월).
 * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
 */
export async function triggerOroraMonthlyMaterialize(
  salesMonth?: string,
): Promise<OroraMonthlyMaterializeTriggerResponse> {
  const res = await client.post<ApiResponse<OroraMonthlyMaterializeTriggerResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-monthly/trigger',
    salesMonth ? { salesMonth } : {},
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 월매출 수동 적재에 실패했습니다');
  }
  return res.data.data;
}

/** ORORA 월매출 거래처 청크 1개의 메타 (UI 선택 표시용). */
export interface OroraMonthlyChunkInfo {
  /** 0-based 청크 번호. */
  chunkIndex: number;
  /** 청크 시작 거래처 코드 (ORORA view 원본 형식, 선행 000 포함). */
  fromAccountCode: string;
  /** 청크 끝 거래처 코드 (ORORA view 원본 형식, 선행 000 포함). */
  toAccountCode: string;
}

/** ORORA 월매출 거래처 청크 메타 응답. */
export interface OroraMonthlyChunkCatalogResponse {
  /** 전체 거래처 청크 개수. */
  chunkCount: number;
  /** 청크 1개의 거래처 코드 폭. */
  chunkSize: number;
  /** 각 청크의 index + 거래처 코드 경계. */
  chunks: OroraMonthlyChunkInfo[];
}

/**
 * ORORA 월매출 거래처 청크 메타(전체 청크 수 + 각 청크 경계)를 조회한다.
 * 정적 거래처 범위에서 산출되므로 ORORA 호출 없이 즉시 반환된다. 조회 권한(`VIEW_ALL_DATA`) 이면 충분.
 */
export async function getOroraMonthlyChunks(): Promise<OroraMonthlyChunkCatalogResponse> {
  const res = await client.get<ApiResponse<OroraMonthlyChunkCatalogResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-monthly/chunks',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 월매출 청크 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * ORORA 월매출 적재를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다
 * (`salesMonth` 미지정 시 전월). 전체 범위 실행과 달리 선택 청크의 거래처 구간만 적재한다.
 * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
 */
export async function triggerOroraMonthlyMaterializeChunk(
  chunkIndex: number,
  salesMonth?: string,
): Promise<OroraMonthlyMaterializeTriggerResponse> {
  const res = await client.post<ApiResponse<OroraMonthlyMaterializeTriggerResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-monthly/chunk/trigger',
    salesMonth ? { chunkIndex, salesMonth } : { chunkIndex },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 월매출 청크 수동 적재에 실패했습니다');
  }
  return res.data.data;
}
