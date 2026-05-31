import client from './client';
import type { ApiResponse } from './types';

// ─────────────────────────────────────────────────────
// 타입 정의
// ─────────────────────────────────────────────────────

/** 현장점검 분류 (자사/경쟁사) */
export type InspectionCategory = 'OWN' | 'COMPETITOR';

/** 현장유형 코드 (본매대/행사매대/시식/기타) */
export type InspectionFieldTypeCode = 'MAIN_SHELF' | 'EVENT_SHELF' | 'TASTING' | 'ETC';

export interface InspectionListParams {
  startDate?: string;
  endDate?: string;
  category?: InspectionCategory;
  fieldType?: InspectionFieldTypeCode;
  employeeName?: string;
  accountCode?: string;
  page?: number;
  size?: number;
}

export interface InspectionListItem {
  id: number;
  category: string;
  accountName: string;
  themeName: string;
  employeeName: string;
  employeeOrgName: string | null;
  inspectionDate: string;
  fieldType: string;
  fieldTypeCode: string;
  createdAt: string;
}

export interface InspectionListData {
  content: InspectionListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface InspectionPhoto {
  id: number;
  url: string;
}

export interface InspectionDetail {
  id: number;
  category: string;
  accountName: string;
  accountId: number;
  themeName: string;
  themeId: number;
  employeeId: number;
  employeeName: string;
  employeeOrgName: string | null;
  inspectionDate: string;
  fieldType: string;
  fieldTypeCode: string;
  description: string | null;
  productCode: string | null;
  productName: string | null;
  competitorName: string | null;
  competitorActivity: string | null;
  competitorTasting: boolean | null;
  competitorProductName: string | null;
  competitorProductPrice: number | null;
  competitorSalesQuantity: number | null;
  photos: InspectionPhoto[];
  createdAt: string;
}

// ─────────────────────────────────────────────────────
// API 호출 함수
// ─────────────────────────────────────────────────────

export async function fetchInspections(params: InspectionListParams): Promise<InspectionListData> {
  const res = await client.get<ApiResponse<InspectionListData>>('/api/v1/admin/inspections', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '현장점검 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchInspectionDetail(id: number): Promise<InspectionDetail> {
  const res = await client.get<ApiResponse<InspectionDetail>>(`/api/v1/admin/inspections/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '현장점검 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

// ─────────────────────────────────────────────────────
// 등록 (admin 수동 등록 — SF SiteActivity New 폼 동등)
// ─────────────────────────────────────────────────────

export interface CreateInspectionRequest {
  themeId: number;
  accountId: number;
  employeeId: number;
  inspectionDate: string;
  category: InspectionCategory;
  fieldTypeCode: InspectionFieldTypeCode;
  description?: string | null;
  productCode?: string | null;
  competitorName?: string | null;
  competitorActivity?: string | null;
  competitorTasting?: boolean | null;
  competitorProductName?: string | null;
  competitorProductPrice?: number | null;
  competitorSalesQuantity?: number | null;
}

export interface InspectionMutationResult {
  id: number;
  name: string | null;
}

export async function createInspection(
  request: CreateInspectionRequest,
  photos: File[],
): Promise<InspectionMutationResult> {
  const formData = new FormData();
  formData.append(
    'request',
    new Blob([JSON.stringify(request)], { type: 'application/json' }),
  );
  photos.forEach((file) => formData.append('photos', file));
  const res = await client.post<ApiResponse<InspectionMutationResult>>(
    '/api/v1/admin/inspections',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '현장점검 등록에 실패했습니다');
  }
  return res.data.data;
}

/** 수정 요청 — 등록과 동일 필드 셋 (사진 변경은 미지원, 본문 수정만). */
export type UpdateInspectionRequest = CreateInspectionRequest;

export async function updateInspection(
  id: number,
  request: UpdateInspectionRequest,
): Promise<InspectionMutationResult> {
  const res = await client.put<ApiResponse<InspectionMutationResult>>(
    `/api/v1/admin/inspections/${id}`,
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '현장점검 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteInspection(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/inspections/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '현장점검 삭제에 실패했습니다');
  }
}
