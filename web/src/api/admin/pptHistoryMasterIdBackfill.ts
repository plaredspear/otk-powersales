import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 개발자 도구 — 전문행사조 이력 원인 마스터 FK(master_id) 백필 API.
 *
 * SF 이관 이력에는 마스터 참조 필드가 없어 FK 가 비어 있다. 그대로 두면 마스터 삭제 / 핵심필드 수정
 * 가드가 이관분 마스터를 보호하지 못해, 삭제 시 사원 전문행사조만 근거 없이 남는다.
 *
 * - preview: `VIEW_ALL_DATA`
 * - execute: `MODIFY_ALL_DATA` (SYSTEM_ADMIN)
 */

export interface PPTHistoryBackfillPreview {
  /** 후보가 정확히 1건이라 채울 수 있는 이력 수. */
  backfillable: number;
  /** 후보가 둘 이상이라 대상에서 제외되는 이력 수. */
  ambiguous: number;
}

export interface PPTHistoryBackfillResult {
  /** 이번 실행에서 실제로 master_id 가 채워진 건수. */
  updated: number;
  /** 실행 후에도 남은 단일 매칭 대상 건수 (0 이면 완료). */
  remaining: number;
  /** 후보 다중이라 계속 남는 건수 — 재실행해도 줄지 않는다. */
  ambiguous: number;
}

export async function previewPPTHistoryBackfill(): Promise<PPTHistoryBackfillPreview> {
  const res = await client.get<ApiResponse<PPTHistoryBackfillPreview>>(
    '/api/v1/admin/ppt-history/master-id-backfill/preview',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '백필 대상 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function executePPTHistoryBackfill(limit?: number): Promise<PPTHistoryBackfillResult> {
  const res = await client.post<ApiResponse<PPTHistoryBackfillResult>>(
    '/api/v1/admin/ppt-history/master-id-backfill/execute',
    null,
    { params: limit ? { limit } : undefined },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '백필 실행에 실패했습니다');
  }
  return res.data.data;
}
