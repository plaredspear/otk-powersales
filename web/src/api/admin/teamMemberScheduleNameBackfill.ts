import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 개발자 도구 — 여사원일정(team_member_schedule) name 백필 API.
 *
 * 자동 채번은 신규 INSERT 부터 적용되므로, 그 이전에 name 이 비어 저장된 기존 일정에
 * SF AutoNumber 재현값(TS{00000000})을 소급 부여한다.
 *
 * - preview: `VIEW_ALL_DATA`
 * - execute: `MODIFY_ALL_DATA` (SYSTEM_ADMIN)
 */

export interface NameBackfillPreview {
  /** 채번이 필요한(name 이 비어있는) 일정 건수. */
  missing: number;
}

export interface NameBackfillResult {
  /** 이번 실행에서 조회한 대상 건수. */
  processed: number;
  /** 실제 name 이 채워진 건수. */
  updated: number;
  /** 실행 후에도 아직 name 이 비어있는 잔여 건수. */
  remaining: number;
}

export async function previewNameBackfill(): Promise<NameBackfillPreview> {
  const res = await client.get<ApiResponse<NameBackfillPreview>>(
    '/api/v1/admin/team-member-schedule/name-backfill/preview',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '백필 대상 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function executeNameBackfill(limit?: number): Promise<NameBackfillResult> {
  const res = await client.post<ApiResponse<NameBackfillResult>>(
    '/api/v1/admin/team-member-schedule/name-backfill/execute',
    null,
    { params: limit ? { limit } : undefined },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '백필 실행에 실패했습니다');
  }
  return res.data.data;
}
