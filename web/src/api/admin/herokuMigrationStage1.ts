import { AxiosError } from 'axios';
import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Heroku 데이터 마이그레이션 Stage 1 admin API (1회성 cut-over 도구).
 *
 * 백엔드 `/api/v1/admin/heroku-migration/stage1/**` 호출.
 *
 * Stage 1 은 TablePlus export CSV (S3 업로드된 상태) 를 PostgreSQL COPY 로 직접 적재한다.
 * 대용량 entity (safetycheck__workschedule__member 91만행, product_favorites 38만행) 의 적재는
 * 수 분 소요. SF 와 달리 적재 전 TRUNCATE 여부 (`reset`) 와 PK 미매칭 row 카운트
 * (`unmatchedRows`) 를 함께 다룬다.
 *
 * 두 가지 모드:
 *  - SINGLE: target 1개를 골라 적재.
 *  - BATCH : 등록된 모든 entity 를 의존성 순서대로 일괄 적재. 1개 실패 시 즉시 중단.
 */

export type HerokuStage1Status = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';
export type HerokuStage1Mode = 'SINGLE' | 'BATCH';
export type HerokuStage1EntityStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'SKIPPED';

export interface HerokuStage1EntityResult {
  targetName: string;
  status: HerokuStage1EntityStatus;
  s3Key: string | null;
  processedRows: number;
  filteredOut: number;
  insertedRows: number;
  unmatchedRows: number;
  errorMessage: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface HerokuStage1CopyProgress {
  status: HerokuStage1Status;
  mode: HerokuStage1Mode;
  startedAt: string | null;
  finishedAt: string | null;
  targetName: string | null;
  s3Bucket: string | null;
  s3Key: string | null;
  processedRows: number;
  filteredOut: number;
  insertedRows: number;
  unmatchedRows: number;
  errors: string[];
  entityResults: HerokuStage1EntityResult[];
}

export interface HerokuStage1CopyRequest {
  targetName: string;
  s3Bucket: string;
  /** CSV 공통 prefix (예: "heroku-migration/input"). 파일명은 target 의 csvFileName 으로 backend 가 자동 조립. */
  s3KeyPrefix: string;
  /** 적재 전 대상 테이블 TRUNCATE 여부. 기본 true. */
  reset: boolean;
  /** sample 적재 상한 (CSV row 기준). 미지정 시 전체 적재. */
  maxRows?: number;
}

export interface HerokuStage1CopyAllRequest {
  s3Bucket: string;
  s3KeyPrefix: string;
  /** 적재 전 각 entity 테이블 TRUNCATE 여부. 기본 true. */
  reset: boolean;
  /** entity 별 sample 적재 상한 (CSV row 기준). 미지정 시 전체 적재. */
  maxRows?: number;
}

export class HerokuStage1AlreadyRunningError extends Error {
  constructor(public readonly progress: HerokuStage1CopyProgress) {
    super('이미 실행 중입니다');
    this.name = 'HerokuStage1AlreadyRunningError';
  }
}

/**
 * Heroku Stage 1 단일 target 적재 실행.
 *
 * `/api/v1/admin/heroku-migration/stage1/copy-from-s3` POST. backend RUNNING 중이면
 * 409 + 현재 progress 를 본문에 담아 `HerokuStage1AlreadyRunningError` 로 변환.
 */
export async function startHerokuStage1Copy(
  req: HerokuStage1CopyRequest,
): Promise<HerokuStage1CopyProgress> {
  return postAndHandle409(
    '/api/v1/admin/heroku-migration/stage1/copy-from-s3',
    req,
    'Stage 1 적재 실행 요청에 실패했습니다',
  );
}

/**
 * Heroku Stage 1 전체 entity 일괄 적재 실행.
 *
 * `/api/v1/admin/heroku-migration/stage1/copy-all-from-s3` POST. 의존성 순서대로 순차 적재,
 * 1개 실패 시 즉시 중단. RUNNING 중이면 `HerokuStage1AlreadyRunningError`.
 */
export async function startHerokuStage1CopyAll(
  req: HerokuStage1CopyAllRequest,
): Promise<HerokuStage1CopyProgress> {
  return postAndHandle409(
    '/api/v1/admin/heroku-migration/stage1/copy-all-from-s3',
    req,
    'Stage 1 일괄 적재 실행 요청에 실패했습니다',
  );
}

async function postAndHandle409<TReq>(
  url: string,
  body: TReq,
  failureMessage: string,
): Promise<HerokuStage1CopyProgress> {
  try {
    const res = await client.post<ApiResponse<HerokuStage1CopyProgress>>(url, body);
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.message || failureMessage);
    }
    return res.data.data;
  } catch (err) {
    // backend 가 RUNNING 중일 때 409 + 본문에 현재 progress 반환. 정상 진행 상태로 취급.
    if (err instanceof AxiosError && err.response?.status === 409) {
      const ar = err.response.data as ApiResponse<HerokuStage1CopyProgress> | undefined;
      if (ar?.data) {
        throw new HerokuStage1AlreadyRunningError(ar.data);
      }
    }
    throw err;
  }
}

/**
 * Heroku Stage 1 진행 상태 조회.
 *
 * `/api/v1/admin/heroku-migration/stage1/copy-from-s3/progress` GET. ApiResponse 의 data 언래핑.
 */
export async function getHerokuStage1CopyProgress(): Promise<HerokuStage1CopyProgress> {
  const res = await client.get<ApiResponse<HerokuStage1CopyProgress>>(
    '/api/v1/admin/heroku-migration/stage1/copy-from-s3/progress',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 진행 상태 조회에 실패했습니다');
  }
  return res.data.data;
}

export interface HerokuStage1Target {
  targetName: string;
  /** Heroku export CSV 파일명 (예: "product_favorites.csv"). prefix 와 합쳐 최종 S3 key 조립. */
  csvFileName: string;
}

export interface HerokuStage1Defaults {
  /** 운영 S3 bucket (backend 의 S3_BUCKET 환경 속성). 미설정 시 빈 문자열. */
  s3Bucket: string;
  /** CSV 공통 prefix (예: "heroku-migration/input"). */
  s3KeyPrefix: string;
}

/**
 * Heroku Stage 1 기본값 (S3 bucket / prefix) 조회.
 *
 * `/api/v1/admin/heroku-migration/stage1/defaults` GET. backend 환경값으로 폼 프리필에 사용.
 */
export async function getHerokuStage1Defaults(): Promise<HerokuStage1Defaults> {
  const res = await client.get<ApiResponse<HerokuStage1Defaults>>(
    '/api/v1/admin/heroku-migration/stage1/defaults',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 기본값 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * Heroku Stage 1 적재 대상 target 목록 조회.
 *
 * `/api/v1/admin/heroku-migration/stage1/targets` GET. 단일 적재 모드의 select 옵션 공급.
 */
export async function listHerokuStage1Targets(): Promise<HerokuStage1Target[]> {
  const res = await client.get<ApiResponse<HerokuStage1Target[]>>(
    '/api/v1/admin/heroku-migration/stage1/targets',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 target 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
