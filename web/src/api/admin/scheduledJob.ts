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
