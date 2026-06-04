import { AxiosError } from 'axios';
import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * SF 데이터 마이그레이션 Stage 1 admin API (1회성 cut-over 도구).
 *
 * 백엔드 `/api/v1/admin/sf-migration/stage1/**` 호출 — `SF_MIGRATION_RUN` 권한 필요
 * (`SYSTEM_ADMIN` role 만 보유).
 *
 * Stage 1 은 SF export CSV (S3 업로드된 상태) 를 PostgreSQL COPY 로 직접 적재한다.
 * 큰 entity (ErpOrderProduct ≈ 5GB) 의 적재는 수 분~수십 분 소요.
 *
 * 두 가지 모드:
 *  - SINGLE: target 1개를 골라 적재.
 *  - BATCH : 등록된 모든 entity 를 의존성 순서대로 일괄 적재. 1개 실패 시 즉시 중단.
 */

export type Stage1Status = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';
export type Stage1Mode = 'SINGLE' | 'BATCH';
export type Stage1EntityStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED';

export interface Stage1EntityResult {
  targetName: string;
  status: Stage1EntityStatus;
  s3Key: string | null;
  processedRows: number;
  filteredOut: number;
  insertedRows: number;
  errorMessage: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface Stage1CopyProgress {
  status: Stage1Status;
  mode: Stage1Mode;
  startedAt: string | null;
  finishedAt: string | null;
  targetName: string | null;
  s3Bucket: string | null;
  s3Key: string | null;
  processedRows: number;
  filteredOut: number;
  insertedRows: number;
  errors: string[];
  entityResults: Stage1EntityResult[];
}

export interface Stage1CopyRequest {
  targetName: string;
  s3Bucket: string;
  /** CSV 공통 prefix (예: "sf-migration/input"). 파일명은 target 의 csvFileName 으로 backend 가 자동 조립. */
  s3KeyPrefix: string;
  /** sample 적재 상한 (CSV row 기준). 미지정 시 전체 적재. */
  maxRows?: number;
}

export interface Stage1CopyAllRequest {
  s3Bucket: string;
  s3KeyPrefix: string;
  /** entity 별 sample 적재 상한 (CSV row 기준). 미지정 시 전체 적재. */
  maxRows?: number;
}

export class Stage1AlreadyRunningError extends Error {
  constructor(public readonly progress: Stage1CopyProgress) {
    super('이미 실행 중입니다');
    this.name = 'Stage1AlreadyRunningError';
  }
}

export async function startStage1Copy(
  req: Stage1CopyRequest,
): Promise<Stage1CopyProgress> {
  return postAndHandle409(
    '/api/v1/admin/sf-migration/stage1/copy-from-s3',
    req,
    'Stage 1 적재 실행 요청에 실패했습니다',
  );
}

export async function startStage1CopyAll(
  req: Stage1CopyAllRequest,
): Promise<Stage1CopyProgress> {
  return postAndHandle409(
    '/api/v1/admin/sf-migration/stage1/copy-all-from-s3',
    req,
    'Stage 1 일괄 적재 실행 요청에 실패했습니다',
  );
}

async function postAndHandle409<TReq>(
  url: string,
  body: TReq,
  failureMessage: string,
): Promise<Stage1CopyProgress> {
  try {
    const res = await client.post<ApiResponse<Stage1CopyProgress>>(url, body);
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.message || failureMessage);
    }
    return res.data.data;
  } catch (err) {
    // backend 가 RUNNING 중일 때 409 + 본문에 현재 progress 반환. 정상 진행 상태로 취급.
    if (err instanceof AxiosError && err.response?.status === 409) {
      const ar = err.response.data as ApiResponse<Stage1CopyProgress> | undefined;
      if (ar?.data) {
        throw new Stage1AlreadyRunningError(ar.data);
      }
    }
    throw err;
  }
}

export async function getStage1CopyProgress(): Promise<Stage1CopyProgress> {
  const res = await client.get<ApiResponse<Stage1CopyProgress>>(
    '/api/v1/admin/sf-migration/stage1/copy-from-s3/progress',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 진행 상태 조회에 실패했습니다');
  }
  return res.data.data;
}

export interface Stage1Target {
  targetName: string;
  /** SF export CSV 파일명 (예: "erp_order_products.csv"). prefix 와 합쳐 최종 S3 key 조립. */
  csvFileName: string;
}

export interface Stage1Defaults {
  /** 운영 S3 bucket (backend 의 S3_BUCKET 환경 속성). 미설정 시 빈 문자열. */
  s3Bucket: string;
  /** CSV 공통 prefix (예: "sf-migration/input"). */
  s3KeyPrefix: string;
}

export async function getStage1Defaults(): Promise<Stage1Defaults> {
  const res = await client.get<ApiResponse<Stage1Defaults>>(
    '/api/v1/admin/sf-migration/stage1/defaults',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 기본값 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function listStage1Targets(): Promise<Stage1Target[]> {
  const res = await client.get<ApiResponse<Stage1Target[]>>(
    '/api/v1/admin/sf-migration/stage1/targets',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 target 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
