import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * SF 데이터 마이그레이션 Stage 2 admin API (1회성 cut-over 도구).
 *
 * 백엔드 `/api/v1/admin/sf-migration/stage2/**` 호출 — `SF_MIGRATION_RUN` 권한 필요
 * (`SYSTEM_ADMIN` role 만 보유).
 */

export type FkResolveStatus =
  | 'IDLE'
  | 'RUNNING'
  | 'COMPLETED'
  | 'COMPLETED_WITH_WARNINGS'
  | 'FAILED';

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

/**
 * FK Resolve 실행. `tableName` 미지정 시 전체 테이블, 지정 시 해당 테이블 1개만 처리.
 * 단일/전체 모두 동일한 비동기 + progress 메커니즘을 공유한다.
 */
export async function startFkResolve(
  tableName?: string,
): Promise<FkResolveProgress> {
  const res = await client.post<ApiResponse<FkResolveProgress>>(
    '/api/v1/admin/sf-migration/stage2/fk',
    undefined,
    tableName ? { params: { tableName } } : undefined,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'FK Resolve 실행 요청에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 처리 가능한 테이블 목록 (단일 테이블 선택 드롭다운용). 정렬된 테이블명 배열.
 */
export async function getFkResolvableTables(): Promise<string[]> {
  const res = await client.get<ApiResponse<string[]>>(
    '/api/v1/admin/sf-migration/stage2/fk/tables',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'FK Resolve 테이블 목록 조회에 실패했습니다');
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
 * Stage 2-A — UploadFile polymorphic parent resolve.
 *
 * Stage1 이 적재한 `object_type` (SF Object__c 원본) 기준으로 `parent_type` 을 파생하고,
 * `(parent_type, record_sfid)` → `parent_id` (Long FK) 를 채운다. claim/notice/proposal/
 * site_activity 4종 polymorphic. `record_sfid` 는 SKIP_FK_PREFIXES 라 일반 FK Resolve 가
 * 건드리지 않으므로 본 substep 을 별도 1회 실행해야 첨부 이미지 조회가 연결된다.
 *
 * 동기 실행 — 보통 수 초 내 완료.
 */
export interface UploadFileParentSubstepResult {
  label: string;
  rowsAffected: number;
}

export interface UploadFileParentResponse {
  substep: string;
  results: UploadFileParentSubstepResult[];
  totalRowsAffected: number;
}

export async function runUploadFilePolymorphicParent(): Promise<UploadFileParentResponse> {
  const res = await client.post<ApiResponse<UploadFileParentResponse>>(
    '/api/v1/admin/sf-migration/stage2/upload-file-polymorphic-parent',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(
      res.data.message || 'UploadFile Parent Resolve 실행에 실패했습니다',
    );
  }
  return res.data.data;
}

/**
 * Stage 2 — 공지 본문 rtaImage <img> → placeholder 치환 (notice.contents UPDATE).
 *
 * 공지 본문에 박힌 SF rtaImage 서블릿 URL 을 만료 없는 placeholder
 * (`<img src="notice-image://{refid}" data-refid="{refid}">`) 로 치환한다. 조회 시점에 NoticeService
 * 가 data-refid 로 presigned URL 을 rewrite 하므로, 본문에는 placeholder 만 영구 저장한다.
 *
 * 공지 본문 이미지 적재(Stage1 NoticeImageUploadFile) + UploadFile Parent Resolve 완료 후 1회 실행.
 * `dryRun=true` (기본) 면 변경 대상만 집계, `false` 면 실제 UPDATE. 멱등 (이미 치환된 본문 skip).
 */
export interface NoticeRtaPlaceholderSubstepResult {
  label: string;
  rowsAffected: number;
}

export interface NoticeRtaPlaceholderResponse {
  substep: string;
  results: NoticeRtaPlaceholderSubstepResult[];
  totalRowsAffected: number;
}

export async function runNoticeRtaPlaceholder(
  dryRun: boolean,
): Promise<NoticeRtaPlaceholderResponse> {
  const res = await client.post<ApiResponse<NoticeRtaPlaceholderResponse>>(
    '/api/v1/admin/sf-migration/stage2/notice-rta-placeholder',
    undefined,
    { params: { dryRun } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(
      res.data.message || '공지 본문 placeholder 치환 실행에 실패했습니다',
    );
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

/**
 * Stage 2 — UserRole Hierarchy snapshot 재계산.
 *
 * `user_role_hierarchy_snapshot` 의 `all_subordinate_ids` (jsonb) + `depth` +
 * `ancestor_path` + `snapshot_at` 을 `user_role.parent_user_role_id` 트리 기반으로
 * 재계산. Stage1 (UserRoleHierarchySnapshot) 적재 + Stage2 fk-natural-key 실행 직후
 * 1회 호출 의무 — 미실행 시 `AdminDataScopeService` 등 권한 평가 path 에서 `depth`
 * NULL 인 row 가 Hibernate primitive setter 호출 시점에 NPE 유발.
 *
 * 응답은 Stage 2 공통 wrapper (`SfMigrationStage2Response`) — substep / results /
 * totalRowsAffected. 본 endpoint 는 row 카운트가 0 (snapshot 재계산 트리거 의미).
 */
export interface UserRoleHierarchyRecalcResponse {
  substep: string;
  results: NaturalKeyFkSubstepResult[];
  totalRowsAffected: number;
}

export async function runUserRoleHierarchyRecalc(): Promise<UserRoleHierarchyRecalcResponse> {
  const res = await client.post<ApiResponse<UserRoleHierarchyRecalcResponse>>(
    '/api/v1/admin/sf-migration/stage2/user-role-hierarchy',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(
      res.data.message || 'UserRole Hierarchy 재계산 실행에 실패했습니다',
    );
  }
  return res.data.data;
}
