import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `InspectionListItem` */
export interface InspectionListItem {
  id: number;
  category: string;
  accountName: string;
  accountId: number;
  inspectionDate: string;
  fieldType: string;
  fieldTypeCode: string;
}

export interface InspectionPhoto {
  id: number;
  url: string;
}

/** backend `InspectionDetailResponse` */
export interface InspectionDetail {
  id: number;
  category: string;
  accountName: string;
  accountId: number;
  themeName: string;
  themeId: number;
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

export interface InspectionTheme {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
}

export interface InspectionFieldType {
  code: string;
  name: string;
}

/** backend `InspectionRegisterRequest` (@RequestPart request JSON) */
export interface InspectionRegisterRequest {
  themeId?: number;
  category?: string;
  accountId?: number;
  inspectionDate?: string; // YYYY-MM-DD
  fieldTypeCode?: string;
  description?: string;
  productCode?: string;
  competitorName?: string;
  competitorActivity?: string;
  competitorTasting?: boolean;
  competitorProductName?: string;
  competitorProductPrice?: number;
  competitorSalesQuantity?: number;
}

export interface InspectionListParams {
  fromDate: string;
  toDate: string;
  accountId?: number;
  category?: string;
}

export async function fetchInspections(params: InspectionListParams): Promise<InspectionListItem[]> {
  const res = await client.get<ApiResponse<InspectionListItem[]>>('/api/v1/mobile/inspections', {
    params,
  });
  return unwrap(res, '현장점검 목록 조회에 실패했습니다');
}

export async function fetchInspectionDetail(inspectionId: number): Promise<InspectionDetail> {
  const res = await client.get<ApiResponse<InspectionDetail>>(
    `/api/v1/mobile/inspections/${inspectionId}`
  );
  return unwrap(res, '점검 상세 조회에 실패했습니다');
}

export async function fetchInspectionThemes(): Promise<InspectionTheme[]> {
  const res = await client.get<ApiResponse<InspectionTheme[]>>('/api/v1/mobile/inspections/themes');
  return unwrap(res, '테마 조회에 실패했습니다');
}

export async function fetchInspectionFieldTypes(): Promise<InspectionFieldType[]> {
  const res = await client.get<ApiResponse<InspectionFieldType[]>>(
    '/api/v1/mobile/inspections/field-types'
  );
  return unwrap(res, '활동유형 조회에 실패했습니다');
}

export async function registerInspection(
  request: InspectionRegisterRequest,
  photos: File[]
): Promise<InspectionListItem> {
  const form = new FormData();
  form.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  photos.forEach((p) => form.append('photos', p));
  const res = await client.post<ApiResponse<InspectionListItem>>(
    '/api/v1/mobile/inspections',
    form
  );
  return unwrap(res, '현장점검 등록에 실패했습니다');
}
