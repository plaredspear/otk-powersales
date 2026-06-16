import client from './client';
import type { ApiResponse } from './types';

export interface StaffReviewSyncTestInput {
  /** 조회 기준 일자 (YYYYMMDD, 예: '20260410'). SF Request Body 의 MOD_DT. */
  modDt: string;
}

/** SF 사원평가 마스터 조회 테스트 결과 (SF 응답 원형). */
export interface StaffReviewSyncTestResult {
  success: boolean;
  resultCode: string | null;
  resultMsg: string | null;
  /** SF 응답 본문(raw JSON). 사원평가 마스터 목록이 이 안에 담겨 온다. */
  rawResponse: string | null;
  /** SF 로 전송한 요청 body JSON ({ "MOD_DT": "..." }). */
  requestPayload: string;
}

/**
 * SF `IF_SendStaffReviewToPWS` 사원평가 마스터 조회를 테스트 호출한다 (개발자 도구 — 외부 API 테스트).
 *
 * 기준 일자(MOD_DT) 하나를 SF 로 POST 하면 SF 가 해당 일자(수정일 기준)로 변경된 사원평가 마스터 목록을
 * 응답하는 SF → PWS 조회 인터페이스. 신규 DB 에는 저장하지 않고 SF 응답 원형만 반환한다.
 */
export async function testStaffReviewSync(
  input: StaffReviewSyncTestInput,
): Promise<StaffReviewSyncTestResult> {
  const res = await client.post<ApiResponse<StaffReviewSyncTestResult>>(
    '/api/v1/admin/staff-review/sync/test',
    input,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(
      res.data.message || 'SF 사원평가 마스터 조회 테스트에 실패했습니다',
    );
  }
  return res.data.data;
}
