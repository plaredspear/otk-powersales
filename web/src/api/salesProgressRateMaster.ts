import client from './client';
import type { ApiResponse } from './types';

export interface SalesProgressRateMasterListParams {
  keyword?: string;
  targetYear?: string;
  targetMonth?: string;
  page: number;
  size: number;
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

  const res = await client.get<ApiResponse<SalesProgressRateMasterListData>>(
    '/api/v1/admin/sales-progress-rate-masters',
    { params: queryParams },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '거래처목표등록마스터 목록 조회에 실패했습니다');
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
}

/**
 * SF `IF_salesprogresssend` 거래처목표등록마스터 조회를 테스트 호출한다 (개발자 도구 — 외부 API 테스트).
 *
 * 기준 일자(MOD_DT) 하나를 SF 로 POST 하면 SF 가 해당 일자 기준으로 변경된 거래처목표등록마스터 목록을
 * 응답하는 SF → PWS 조회 인터페이스. 신규 DB 에는 저장하지 않고 SF 응답 원형만 반환한다.
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
