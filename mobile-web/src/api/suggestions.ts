import client from './client';
import { unwrap, type ApiResponse, type SpringPage } from './types';

/**
 * 제안/물류클레임 통합 도메인 (backend `SuggestionController`).
 * 물류클레임 목록/상세는 category === 'LOGISTICS_CLAIM' 항목으로 표현된다.
 */
export const SUGGESTION_CATEGORY = {
  SUGGESTION: 'SUGGESTION',
  LOGISTICS_CLAIM: 'LOGISTICS_CLAIM',
} as const;

/** backend `SuggestionListItem` */
export interface SuggestionListItem {
  id: number;
  proposalNumber: string;
  category: string;
  categoryName: string;
  title: string;
  createdAt: string;
}

/** backend `SuggestionResponse` (상세) */
export interface SuggestionDetail {
  id: number;
  proposalNumber: string;
  category: string;
  categoryName: string;
  title: string;
  content: string;
  productCode: string | null;
  sapAccountCode: string | null;
  accountId: number | null;
  employeeId: number | null;
  claimType: string | null;
  claimTypeMeasures: string | null;
  claimDate: string | null;
  carNumber: string | null;
  logisticsResponsibility: string | null;
  receptionLogisticsCenter: string | null;
  responsibleLogisticsCenter: string | null;
  actionStatus: string | null;
  duplicateProposalNum: string | null;
  status: string;
  createdAt: string;
  attachments: SuggestionAttachment[];
}

export interface SuggestionAttachment {
  id: number;
  url?: string;
  fileName?: string;
  [key: string]: unknown;
}

export async function fetchSuggestions(
  page = 0,
  size = 20
): Promise<SpringPage<SuggestionListItem>> {
  const res = await client.get<ApiResponse<SpringPage<SuggestionListItem>>>(
    '/api/v1/mobile/suggestions',
    { params: { page, size } }
  );
  return unwrap(res, '목록 조회에 실패했습니다');
}

/**
 * 물류클레임 목록 — 통합 제안 목록에서 LOGISTICS_CLAIM 만 필터.
 *
 * ⚠️ 결정 필요(T1): 현재 backend `listMine` 은 category 필터 파라미터가 없어 클라이언트
 * 필터링한다. 항목 수가 많아지면 backend 에 category 필터 파라미터 추가 검토.
 */
export async function fetchLogisticsClaims(
  page = 0,
  size = 50
): Promise<SuggestionListItem[]> {
  const pageData = await fetchSuggestions(page, size);
  return pageData.content.filter((s) => s.category === SUGGESTION_CATEGORY.LOGISTICS_CLAIM);
}

export async function fetchSuggestionDetail(suggestionId: number): Promise<SuggestionDetail> {
  const res = await client.get<ApiResponse<SuggestionDetail>>(
    `/api/v1/mobile/suggestions/${suggestionId}`
  );
  return unwrap(res, '상세 조회에 실패했습니다');
}

/** backend `SuggestionCreateRequest` (@RequestPart request JSON) */
export interface SuggestionCreateRequest {
  category?: string;
  title?: string;
  content?: string;
  productCode?: string;
  accountId?: number;
  sapAccountCode?: string;
  claimType?: string;
  claimDate?: string;
  carNumber?: string;
  logisticsResponsibility?: string;
  duplicateProposalNum?: string;
  actionStatus?: string;
}

export async function createSuggestion(
  request: SuggestionCreateRequest,
  photos: File[]
): Promise<{ id: number }> {
  const form = new FormData();
  form.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  photos.forEach((p) => form.append('photos', p));
  const res = await client.post<ApiResponse<{ id: number }>>('/api/v1/mobile/suggestions', form);
  return unwrap(res, '제안 등록에 실패했습니다');
}
