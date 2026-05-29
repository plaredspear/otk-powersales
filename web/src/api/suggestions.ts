import client from './client';
import type { ApiResponse } from './types';

// ─────────────────────────────────────────────────────
// 타입 정의
// ─────────────────────────────────────────────────────

export type SuggestionCategory = 'LOGISTICS_CLAIM' | 'NEW_PRODUCT' | 'EXISTING_PRODUCT';

export type SuggestionActionStatus = 'UNCONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'DUPLICATE_RECEPTION';

export type SuggestionStatus = 'DRAFT' | 'SUBMITTED' | 'COMPLETED' | 'WITHDRAWN';

export interface SuggestionListParams {
  startDate?: string;
  endDate?: string;
  category?: SuggestionCategory;
  employeeName?: string;
  accountCode?: string;
  actionStatus?: SuggestionActionStatus;
  productCode?: string;
  page?: number;
  size?: number;
}

export interface SuggestionListItem {
  id: number;
  proposalNumber: string;
  category: SuggestionCategory;
  categoryName: string;
  title: string;
  content: string;
  employeeName: string | null;
  employeeCode: string | null;
  orgName: string | null;
  accountName: string | null;
  accountCode: string | null;
  productName: string | null;
  productCode: string | null;
  productCategory: string | null;
  claimType: string | null;
  claimDate: string | null;
  responsibleLogisticsCenter: string | null;
  logisticsResponsibility: string | null;
  carNumber: string | null;
  actionStatus: SuggestionActionStatus | null;
  actionStatusName: string | null;
  createdAt: string;
}

export interface SuggestionListData {
  content: SuggestionListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SuggestionAttachment {
  id: number;
  s3Url: string | null;
  fileName: string | null;
  sortOrder: number;
}

export interface SuggestionDetail {
  id: number;
  proposalNumber: string;
  category: SuggestionCategory;
  categoryName: string;
  title: string;
  content: string;
  productCode: string | null;
  productName: string | null;
  sapAccountCode: string | null;
  accountId: number | null;
  accountName: string | null;
  accountCode: string | null;
  employeeId: number | null;
  employeeName: string | null;
  employeeCode: string | null;
  orgCostCenterCode: string | null;
  claimType: string | null;
  claimTypeMeasures: string | null;
  claimDate: string | null;
  carNumber: string | null;
  logisticsResponsibility: string | null;
  receptionLogisticsCenter: string | null;
  responsibleLogisticsCenter: string | null;
  actionStatus: SuggestionActionStatus | null;
  actionStatusName: string | null;
  duplicateProposalNum: string | null;
  status: SuggestionStatus;
  createdAt: string;
  attachments: SuggestionAttachment[];
}

export interface SuggestionCreatePayload {
  category: SuggestionCategory;
  title: string;
  content: string;
  productCode?: string;
  accountId?: number;
  sapAccountCode?: string;
  claimType?: string;
  claimDate?: string;
  carNumber?: string;
  logisticsResponsibility?: string;
  duplicateProposalNum?: string;
  actionStatus?: SuggestionActionStatus;
  employeeId?: number;
}

export type SuggestionUpdatePayload = Omit<SuggestionCreatePayload, 'employeeId' | 'productCode' | 'accountId' | 'sapAccountCode'>;

export interface SuggestionCreateResponseData {
  id: number;
  proposalNumber: string;
  attachments: SuggestionAttachment[];
}

// ─────────────────────────────────────────────────────
// API 호출 함수
// ─────────────────────────────────────────────────────

export async function fetchSuggestions(params: SuggestionListParams): Promise<SuggestionListData> {
  const res = await client.get<ApiResponse<SuggestionListData>>('/api/v1/admin/suggestions', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제안사항 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchSuggestionDetail(id: number): Promise<SuggestionDetail> {
  const res = await client.get<ApiResponse<SuggestionDetail>>(`/api/v1/admin/suggestions/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제안사항 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createSuggestion(
  payload: SuggestionCreatePayload,
  photos: File[] = [],
): Promise<SuggestionCreateResponseData> {
  const form = new FormData();
  form.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  photos.forEach((file) => form.append('photos', file));
  const res = await client.post<ApiResponse<SuggestionCreateResponseData>>('/api/v1/admin/suggestions', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제안사항 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateSuggestion(id: number, payload: SuggestionUpdatePayload): Promise<SuggestionDetail> {
  const res = await client.put<ApiResponse<SuggestionDetail>>(`/api/v1/admin/suggestions/${id}`, payload);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제안사항 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteSuggestion(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<null>>(`/api/v1/admin/suggestions/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '제안사항 삭제에 실패했습니다');
  }
}

export async function uploadSuggestionPhotos(id: number, photos: File[]): Promise<SuggestionAttachment[]> {
  const form = new FormData();
  photos.forEach((file) => form.append('photos', file));
  const res = await client.post<ApiResponse<SuggestionAttachment[]>>(
    `/api/v1/admin/suggestions/${id}/photos`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '사진 업로드에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteSuggestionPhoto(suggestionId: number, photoId: number): Promise<void> {
  const res = await client.delete<ApiResponse<null>>(
    `/api/v1/admin/suggestions/${suggestionId}/photos/${photoId}`,
  );
  if (!res.data.success) {
    throw new Error(res.data.message || '사진 삭제에 실패했습니다');
  }
}
