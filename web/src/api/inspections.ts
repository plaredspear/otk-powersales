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
