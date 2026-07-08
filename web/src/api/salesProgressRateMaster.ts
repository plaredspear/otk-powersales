import client from './client';
import type { ApiResponse } from './types';

export interface SalesProgressRateMasterListParams {
  keyword?: string;
  targetYear?: string;
  targetMonth?: string;
  branchCode?: string;
  page: number;
  size: number;
}

export interface SalesProgressRateMasterBranch {
  branchCode: string;
  branchName: string;
}

export interface SalesProgressRateMasterListItem {
  id: number;
  name: string | null;
  targetYear: string | null;
  targetMonth: string | null;
  accountName: string | null;
  accountBranchName: string | null;
  accountCode: string | null;
  accountType: string | null;
  rtTargetAmount: number | null;
  rmTargetAmount: number | null;
  frTargetAmount: number | null;
  foTargetAmount: number | null;
  targetSum: number;
  currentMonthSalesAmount: number | null;
  previousMonthSalesAmount: number | null;
  progressRate: number | null;
}

export interface SalesProgressRateMasterListData {
  content: SalesProgressRateMasterListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SalesProgressRateMasterDetail {
  id: number;
  name: string | null;
  targetYear: string | null;
  targetMonth: string | null;
  accountId: number | null;
  accountName: string | null;
  accountBranchName: string | null;
  accountCode: string | null;
  accountType: string | null;
  rtTargetAmount: number | null;
  rmTargetAmount: number | null;
  frTargetAmount: number | null;
  foTargetAmount: number | null;
  targetSum: number;
  targetSumAmount: number | null;
  currentMonthSalesAmount: number | null;
  previousMonthSalesAmount: number | null;
  progressRate: number | null;
  businessRate: number | null;
  externalKey: string | null;
  accountBranchView: string | null;
  createdByName: string | null;
  lastModifiedByName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export async function fetchSalesProgressRateMasters(
  params: SalesProgressRateMasterListParams,
): Promise<SalesProgressRateMasterListData> {
  const queryParams: Record<string, string | number> = {
    page: params.page,
    size: params.size,
  };
  if (params.keyword) queryParams.keyword = params.keyword;
  if (params.targetYear) queryParams.targetYear = params.targetYear;
  if (params.targetMonth) queryParams.targetMonth = params.targetMonth;
  if (params.branchCode) queryParams.branchCode = params.branchCode;

  const res = await client.get<ApiResponse<SalesProgressRateMasterListData>>(
    '/api/v1/admin/sales-progress-rate-masters',
    { params: queryParams },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처목표등록마스터 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 거래처목표등록마스터 화면 지점 셀렉터 옵션
 * (`GET /api/v1/admin/sales-progress-rate-masters/branches`, `sales_progress_rate_master` READ 권한 필요).
 *
 * 권한 주체별 조회 허용 지점 화이트리스트를 반환 (거래처/여사원 일정과 동일 출처).
 */
export async function getSalesProgressRateMasterBranches(): Promise<
  SalesProgressRateMasterBranch[]
> {
  const res = await client.get<ApiResponse<SalesProgressRateMasterBranch[]>>(
    '/api/v1/admin/sales-progress-rate-masters/branches',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchSalesProgressRateMaster(
  id: number,
): Promise<SalesProgressRateMasterDetail> {
  const res = await client.get<ApiResponse<SalesProgressRateMasterDetail>>(
    `/api/v1/admin/sales-progress-rate-masters/${id}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처목표등록마스터 조회에 실패했습니다');
  }
  return res.data.data;
}

export interface SalesProgressRateMasterSyncTestInput {
  /** 조회 기준 일자 (YYYYMMDD, 예: '20260410'). SF Request Body 의 MOD_DT. */
  modDt: string;
  /** true 면 SF 응답을 주기 sync 와 동일 경로(ExternalKey upsert)로 신규 DB 에 저장. 기본 false — 조회 전용. */
  save?: boolean;
}

/** DB 저장(upsert) 통계 — save=true 조회 시에만 응답에 담긴다. */
export interface SalesProgressRateMasterSyncSummary {
  /** SF 응답에서 파싱된 레코드 수. */
  fetched: number;
  /** 신규 INSERT 건수. */
  inserted: number;
  /** 기존 row UPDATE 건수. */
  updated: number;
  /** ExternalKey 산출 불가로 skip 된 건수. */
  skipped: number;
}

/** SF 거래처목표등록마스터 조회 테스트 결과 (SF 응답 원형). */
export interface SalesProgressRateMasterSyncTestResult {
  success: boolean;
  resultCode: string | null;
  resultMsg: string | null;
  /** SF 응답 본문(raw JSON). 거래처목표등록마스터 목록이 이 안에 담겨 온다. */
  rawResponse: string | null;
  /** SF 로 전송한 요청 body JSON ({ "MOD_DT": "..." }). */
  requestPayload: string;
  /** DB 저장(upsert) 통계. 조회 전용 요청(save=false) 또는 SF 호출 실패 시 null. */
  syncResult: SalesProgressRateMasterSyncSummary | null;
}

/**
 * SF `IF_salesprogresssend` 거래처목표등록마스터 조회를 테스트 호출한다 (개발자 도구 — 외부 API 테스트).
 *
 * 기준 일자(MOD_DT) 하나를 SF 로 POST 하면 SF 가 해당 일자 기준으로 변경된 거래처목표등록마스터 목록을
 * 응답하는 SF → PWS 조회 인터페이스. `save=true` 면 그 응답을 주기 sync 와 동일 경로(ExternalKey upsert)로
 * 신규 DB 에 저장하고 통계를 함께 반환한다. `save=false`(기본)는 조회 전용 — DB 변경 없음.
 */
export async function testSalesProgressRateMasterSync(
  input: SalesProgressRateMasterSyncTestInput,
): Promise<SalesProgressRateMasterSyncTestResult> {
  const res = await client.post<
    ApiResponse<SalesProgressRateMasterSyncTestResult>
  >('/api/v1/admin/sales-progress-rate-master/sync/test', input);
  if (!res.data.success || !res.data.data) {
    throw new Error(
      res.data.message || 'SF 거래처목표등록마스터 조회 테스트에 실패했습니다',
    );
  }
  return res.data.data;
}
