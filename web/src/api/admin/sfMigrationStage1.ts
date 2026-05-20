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
 */

export type Stage1Status = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface Stage1CopyProgress {
  status: Stage1Status;
  startedAt: string | null;
  finishedAt: string | null;
  targetName: string | null;
  s3Bucket: string | null;
  s3Key: string | null;
  processedRows: number;
  filteredOut: number;
  insertedRows: number;
  errors: string[];
}

export interface Stage1CopyRequest {
  targetName: string;
  s3Bucket: string;
  s3Key: string;
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
  try {
    const res = await client.post<ApiResponse<Stage1CopyProgress>>(
      '/api/v1/admin/sf-migration/stage1/copy-from-s3',
      req,
    );
    if (!res.data.success || !res.data.data) {
      throw new Error(res.data.message || 'Stage 1 적재 실행 요청에 실패했습니다');
    }
    return res.data.data;
  } catch (err) {
    // backend 가 RUNNING 중일 때 409 + 본문에 현재 progress 반환. 정상 진행 상태로 취급.
    if (err instanceof AxiosError && err.response?.status === 409) {
      const body = err.response.data as ApiResponse<Stage1CopyProgress> | undefined;
      if (body?.data) {
        throw new Stage1AlreadyRunningError(body.data);
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

export async function listStage1Targets(): Promise<string[]> {
  const res = await client.get<ApiResponse<string[]>>(
    '/api/v1/admin/sf-migration/stage1/targets',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Stage 1 target 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
