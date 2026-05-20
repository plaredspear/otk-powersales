import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * SF 데이터 마이그레이션 Stage 2 admin API (1회성 cut-over 도구).
 *
 * 백엔드 `/api/v1/admin/sf-migration/stage2/**` 호출 — `SF_MIGRATION_RUN` 권한 필요
 * (`SYSTEM_ADMIN` role 만 보유).
 */

export type FkResolveStatus = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface FkResolveTableResult {
  label: string;
  rowsAffected: number;
}

export interface FkResolveProgress {
  status: FkResolveStatus;
  startedAt: string | null;
  finishedAt: string | null;
  totalTables: number;
  completedTables: number;
  currentTable: string | null;
  currentTableChunk: number;
  currentTableTotalChunks: number;
  totalRowsAffected: number;
  tableResults: FkResolveTableResult[];
  errors: string[];
}

export async function startFkResolve(): Promise<FkResolveProgress> {
  const res = await client.post<ApiResponse<FkResolveProgress>>(
    '/api/v1/admin/sf-migration/stage2/fk',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'FK Resolve 실행 요청에 실패했습니다');
  }
  return res.data.data;
}

export async function getFkResolveProgress(): Promise<FkResolveProgress> {
  const res = await client.get<ApiResponse<FkResolveProgress>>(
    '/api/v1/admin/sf-migration/stage2/fk/progress',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'FK Resolve 진행 상태 조회에 실패했습니다');
  }
  return res.data.data;
}
