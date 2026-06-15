import client from '@/api/client';
import type { ApiResponse } from '../types';
import type { FkResolveProgress } from './sfMigration';

/**
 * Heroku 데이터 마이그레이션 Stage 2 admin API (1회성 cut-over 도구).
 *
 * 백엔드 `/api/v1/admin/heroku-migration/stage2/**` 호출.
 *
 * Stage 2 FK Resolve 는 Stage 1 적재 완료 + SF 데이터 마이그레이션(employee/account/product)
 * 선행 적재 후, 자연키→serial id (패턴 A) + 부모 FK (패턴 B) 를 일괄 처리한다.
 */

export type HerokuFkResolveStatus =
  | 'IDLE'
  | 'RUNNING'
  | 'COMPLETED'
  | 'COMPLETED_WITH_WARNINGS'
  | 'FAILED';

export interface HerokuFkTableResult {
  table: string;
  column: string;
  rowsAffected: number;
}

export interface HerokuFkUnmatched {
  table: string;
  column: string;
  naturalKey: string;
  unmatchedCount: number;
}

export interface HerokuFkResolveProgress {
  status: HerokuFkResolveStatus;
  startedAt: string | null;
  finishedAt: string | null;
  totalTables: number;
  completedTables: number;
  currentTable: string | null;
  totalRowsAffected: number;
  tableResults: HerokuFkTableResult[];
  unmatched: HerokuFkUnmatched[];
  errors: string[];
}

/**
 * Heroku Stage 2 FK Resolve 실행.
 *
 * `/api/v1/admin/heroku-migration/stage2/fk` POST. 패턴 A/B FK 일괄 해소를 트리거하고
 * 시작 시점의 progress 를 반환.
 */
export async function startHerokuFkResolve(): Promise<HerokuFkResolveProgress> {
  const res = await client.post<ApiResponse<HerokuFkResolveProgress>>(
    '/api/v1/admin/heroku-migration/stage2/fk',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'FK Resolve 실행 요청에 실패했습니다');
  }
  return res.data.data;
}

/**
 * Heroku Stage 2 FK Resolve 진행 상태 조회.
 *
 * `/api/v1/admin/heroku-migration/stage2/fk/progress` GET. ApiResponse 의 data 언래핑.
 */
export async function getHerokuFkResolveProgress(): Promise<HerokuFkResolveProgress> {
  const res = await client.get<ApiResponse<HerokuFkResolveProgress>>(
    '/api/v1/admin/heroku-migration/stage2/fk/progress',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'FK Resolve 진행 상태 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * sfid FK Resolve (Heroku 전용 sfid 테이블 — safety_check_submission / product_expiration).
 *
 * `@HerokuOnly` 이지만 `_sfid` 값이 진짜 SF Id 라, sfid resolve 엔진(SF Stage 2)을 재사용한다.
 * 따라서 진행 상태 타입은 SF 의 `FkResolveProgress` (chunk 단위 진행 포함) 를 그대로 공유한다.
 * SF FK Resolve 와 동일한 progress 빈을 공유하므로, SF 페이지/본 페이지 중 한쪽이 실행 중이면
 * 다른 쪽도 RUNNING 으로 보이고 중복 실행이 차단된다.
 */

/**
 * Heroku sfid FK Resolve 대상 테이블 목록 (단일 실행 드롭다운용). 정렬된 테이블명 배열.
 */
export async function getHerokuSfidFkResolvableTables(): Promise<string[]> {
  const res = await client.get<ApiResponse<string[]>>(
    '/api/v1/admin/heroku-migration/stage2/sfid-fk/tables',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'sfid FK Resolve 테이블 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * Heroku sfid FK Resolve 실행. `tableName` 미지정 시 Heroku sfid 대상 전체, 지정 시 1개만 처리.
 */
export async function startHerokuSfidFkResolve(
  tableName?: string,
): Promise<FkResolveProgress> {
  const res = await client.post<ApiResponse<FkResolveProgress>>(
    '/api/v1/admin/heroku-migration/stage2/sfid-fk',
    undefined,
    tableName ? { params: { tableName } } : undefined,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'sfid FK Resolve 실행 요청에 실패했습니다');
  }
  return res.data.data;
}

export async function getHerokuSfidFkResolveProgress(): Promise<FkResolveProgress> {
  const res = await client.get<ApiResponse<FkResolveProgress>>(
    '/api/v1/admin/heroku-migration/stage2/sfid-fk/progress',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'sfid FK Resolve 진행 상태 조회에 실패했습니다');
  }
  return res.data.data;
}
