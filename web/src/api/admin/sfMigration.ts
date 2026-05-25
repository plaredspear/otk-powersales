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

/**
 * Stage 2-A Natural Key FK Resolve substep — sfid prefix 가 아닌 자연 키
 * (developer_name / name / 외부 sfid 컬럼) 기반 FK id 채움.
 *
 * `fk` substep 직후 1회 호출 — `permission_set_flags` / `permission_set_assignment` /
 * `profile_flags` / `sharing_rule_target` 등의 FK 컬럼을 채워 `SfPermissionResolver` 가
 * 권한 비트를 평탄화할 수 있게 한다.
 *
 * 동기 실행 — `NATURAL_KEY_FK_MAPPINGS` 9 entry + 전용 분기 2건 일괄 UPDATE.
 */
export interface NaturalKeyFkSubstepResult {
  label: string;
  rowsAffected: number;
}

export interface NaturalKeyFkResponse {
  substep: string;
  results: NaturalKeyFkSubstepResult[];
  totalRowsAffected: number;
}

export async function runNaturalKeyFkResolve(): Promise<NaturalKeyFkResponse> {
  const res = await client.post<ApiResponse<NaturalKeyFkResponse>>(
    '/api/v1/admin/sf-migration/stage2/fk-natural-key',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Natural Key FK Resolve 실행에 실패했습니다');
  }
  return res.data.data;
}

/**
 * Stage 2-B (derived 캐시 동기화) substep — User.cost_center_code 동기화 1건만 운영.
 *
 * Employee.role / Employee.professional_promotion_team / User.profile_type 변환은 폐기됨
 * (SF picklist value 가 곧 저장값 — 변환 substep 자체가 불요).
 */
export type PicklistColumn = 'user_cost_center_code';

export interface PicklistSubstepResult {
  label: string;
  rowsAffected: number;
}

export interface PicklistResponse {
  substep: string;
  results: PicklistSubstepResult[];
  totalRowsAffected: number;
}

export async function runPicklistColumn(column: PicklistColumn): Promise<PicklistResponse> {
  const res = await client.post<ApiResponse<PicklistResponse>>(
    `/api/v1/admin/sf-migration/stage2/picklist/${column}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || `Picklist (${column}) 실행에 실패했습니다`);
  }
  return res.data.data;
}
