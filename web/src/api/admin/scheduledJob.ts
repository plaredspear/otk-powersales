import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Admin 스케줄 잡 실행 이력 조회 API.
 *
 * 백엔드 `/api/v1/admin/scheduled-jobs/**` 호출 — `SCHEDULED_JOB_READ` 권한 필요
 * (`SYSTEM_ADMIN` role 만 보유).
 */

export type ScheduledJobStatus = 'RUNNING' | 'SUCCESS' | 'FAILURE' | 'SKIPPED';

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
  /**
   * 런타임 토글 활성 여부 (Redis). 운영 중 끄고 켜는 토글로, 빈 등록 여부인 enabled 와 별개.
   * false 면 자동 스케줄 발화 시 본문을 실행하지 않고 SKIPPED 이력만 남긴다.
   */
  runtimeEnabled: boolean;
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

/**
 * 스케줄 잡 런타임 활성/비활성 변경.
 * 변경 후 최신 카탈로그(런타임 활성 상태 포함)를 반환한다. `MODIFY_ALL_DATA` 권한 필요.
 */
export async function setScheduledJobRuntimeEnabled(
  jobName: string,
  enabled: boolean,
): Promise<RegisteredScheduledJob[]> {
  const res = await client.put<ApiResponse<RegisteredScheduledJob[]>>(
    '/api/v1/admin/scheduled-jobs/toggle',
    { jobName, enabled },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '스케줄 잡 활성/비활성 변경에 실패했습니다');
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

/**
 * ORORA 매출 수동 적재 **비동기 접수** 응답.
 *
 * 적재는 서버 별도 스레드에서 수행되므로 본 응답은 "접수됨" 만 알린다. 실제 조회/적재 건수는
 * 완료 후 실행 이력(`scheduled_job_run`)에 남으며, 화면은 이력 탭 새로고침으로 진행/결과를 확인한다.
 */
export interface OroraMaterializeAcceptedResponse {
  /** 실행을 접수한 스케줄 잡 이름 (이력 탭 필터 값). */
  jobName: string;
  /** 적재 대상 매출월 (`YYYYMM`). 요청에서 미지정 시 서버가 산출한 값. */
  salesMonth: string;
  /** 접수 여부 (항상 true — 접수 실패는 예외로 전파). */
  accepted: boolean;
  /** 사용자 안내 문구. */
  message: string;
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
 * ORORA 월매출 적재를 특정 월(`YYYYMM`)로 **비동기** 수동 실행 접수한다 (`salesMonth` 미지정 시 전월).
 * 서버는 즉시 접수(202)만 반환하고 실제 적재는 백그라운드에서 수행 — 결과는 실행 이력 탭에서 확인한다.
 * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
 */
export async function triggerOroraMonthlyMaterialize(
  salesMonth?: string,
): Promise<OroraMaterializeAcceptedResponse> {
  const res = await client.post<ApiResponse<OroraMaterializeAcceptedResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-monthly/trigger',
    salesMonth ? { salesMonth } : {},
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 월매출 수동 적재 접수에 실패했습니다');
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
): Promise<OroraMaterializeAcceptedResponse> {
  const res = await client.post<ApiResponse<OroraMaterializeAcceptedResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-monthly/chunk/trigger',
    salesMonth ? { chunkIndex, salesMonth } : { chunkIndex },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 월매출 청크 수동 적재 접수에 실패했습니다');
  }
  return res.data.data;
}

/** ORORA 일매출 거래처 청크 메타 응답 (거래처 범위가 일·월 공용이라 월매출과 형태 동일). */
export interface OroraDailyChunkCatalogResponse {
  /** 전체 거래처 청크 개수. */
  chunkCount: number;
  /** 청크 1개의 거래처 코드 폭. */
  chunkSize: number;
  /** 각 청크의 index + 거래처 코드 경계. */
  chunks: OroraMonthlyChunkInfo[];
}

/**
 * ORORA 일매출 적재를 특정 월(`YYYYMM`)로 수동 실행한다 (`salesMonth` 미지정 시 당월).
 * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
 */
export async function triggerOroraDailyMaterialize(
  salesMonth?: string,
): Promise<OroraMaterializeAcceptedResponse> {
  const res = await client.post<ApiResponse<OroraMaterializeAcceptedResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-daily/trigger',
    salesMonth ? { salesMonth } : {},
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 일매출 수동 적재 접수에 실패했습니다');
  }
  return res.data.data;
}

/**
 * ORORA 일매출 거래처 청크 메타(전체 청크 수 + 각 청크 경계)를 조회한다.
 * 정적 거래처 범위에서 산출되므로 ORORA 호출 없이 즉시 반환된다. 조회 권한(`VIEW_ALL_DATA`) 이면 충분.
 */
export async function getOroraDailyChunks(): Promise<OroraDailyChunkCatalogResponse> {
  const res = await client.get<ApiResponse<OroraDailyChunkCatalogResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-daily/chunks',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 일매출 청크 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * ORORA 일매출 적재를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다
 * (`salesMonth` 미지정 시 당월). 전체 범위 실행과 달리 선택 청크의 거래처 구간만 적재한다.
 * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
 */
export async function triggerOroraDailyMaterializeChunk(
  chunkIndex: number,
  salesMonth?: string,
): Promise<OroraMaterializeAcceptedResponse> {
  const res = await client.post<ApiResponse<OroraMaterializeAcceptedResponse>>(
    '/api/v1/admin/scheduled-jobs/orora-daily/chunk/trigger',
    salesMonth ? { chunkIndex, salesMonth } : { chunkIndex },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'ORORA 일매출 청크 수동 적재 접수에 실패했습니다');
  }
  return res.data.data;
}
