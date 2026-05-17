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
