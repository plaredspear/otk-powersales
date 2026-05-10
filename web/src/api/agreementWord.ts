import client from './client';
import type { ApiResponse } from './types';

/**
 * 관리자 약관 등록 / 활성 약관 조회 API. (Spec #658 P2-W)
 *
 * Backend `AdminAgreementWordController` (`POST /api/v1/admin/agreement-words`,
 * `GET /api/v1/admin/agreement-words/active`) 호출. 응답 `data` 필드를 언래핑하여 반환한다.
 * `active` / `activeDate` 는 입력 차단 (Backend `@AssertFalse` / `@Null`) — 본 모듈은
 * 입력 채널 자체를 노출하지 않는다 (cycle batch #654 가 토글 단독 권한자).
 */

export interface AdminAgreementWordCreateRequest {
  name: string;
  contents: string;
  afterActiveDate: string;
}

export interface AdminAgreementWordCreateResponse {
  agreementWordId: number;
  name: string;
  afterActiveDate: string | null;
  active: boolean;
  activeDate: string | null;
  createdAt: string;
}

export interface AdminAgreementWordActiveResponse {
  agreementWordId: number;
  name: string;
  contents: string;
  activeDate: string | null;
  afterActiveDate: string | null;
}

export async function createAgreementWord(
  payload: AdminAgreementWordCreateRequest,
): Promise<AdminAgreementWordCreateResponse> {
  const res = await client.post<ApiResponse<AdminAgreementWordCreateResponse>>(
    '/api/v1/admin/agreement-words',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '약관 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchActiveAgreementWord(): Promise<AdminAgreementWordActiveResponse | null> {
  const res = await client.get<ApiResponse<AdminAgreementWordActiveResponse | null>>(
    '/api/v1/admin/agreement-words/active',
  );
  return res.data.data ?? null;
}
