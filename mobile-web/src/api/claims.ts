import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `ClaimListItemResponse` */
export interface ClaimListItem {
  claimId: number;
  accountName: string | null;
  productName: string | null;
  productCode: string | null;
  categoryValue: string | null;
  categoryLabel: string | null;
  subcategoryValue: string | null;
  subcategoryLabel: string | null;
  defectQuantity: number | null;
  status: string;
  statusLabel: string;
  createdAt: string;
}

export interface ClaimPhoto {
  photoId: number;
  url: string;
  originalFileName: string | null;
}

/** backend `ClaimDetailResponse` */
export interface ClaimDetail {
  claimId: number;
  accountName: string | null;
  productName: string | null;
  productCode: string | null;
  dateType: string | null;
  dateTypeLabel: string | null;
  date: string | null;
  categoryValue: string | null;
  categoryLabel: string | null;
  subcategoryValue: string | null;
  subcategoryLabel: string | null;
  defectDescription: string | null;
  defectQuantity: number | null;
  purchaseAmount: number | null;
  purchaseMethodName: string | null;
  requestTypeName: string | null;
  status: string;
  statusLabel: string;
  createdAt: string;
  photos: ClaimPhoto[];
}

export interface ClaimListParams {
  startDate?: string;
  endDate?: string;
}

export async function fetchClaims(params: ClaimListParams = {}): Promise<ClaimListItem[]> {
  const res = await client.get<ApiResponse<ClaimListItem[]>>('/api/v1/mobile/claims', { params });
  return unwrap(res, '클레임 목록 조회에 실패했습니다');
}

export async function fetchClaimDetail(claimId: number): Promise<ClaimDetail> {
  const res = await client.get<ApiResponse<ClaimDetail>>(`/api/v1/mobile/claims/${claimId}`);
  return unwrap(res, '클레임 상세 조회에 실패했습니다');
}

// --- 클레임 등록 폼 메타 + 생성 ---

export interface ClaimFormCategory {
  id: string;
  name: string;
  subcategories: { id: string; name: string }[];
}

export interface ClaimFormData {
  categories: ClaimFormCategory[];
  purchaseMethods: { code: string; name: string }[];
  requestTypes: { code: string; name: string }[];
}

export async function fetchClaimFormData(): Promise<ClaimFormData> {
  const res = await client.get<ApiResponse<ClaimFormData>>('/api/v1/mobile/claims/form-data');
  return unwrap(res, '클레임 폼 데이터 조회에 실패했습니다');
}

/** backend `ClaimCreateRequest` (@ModelAttribute) 필드 */
export interface ClaimCreateFields {
  accountId?: number;
  productCode?: string;
  dateType?: string;
  date?: string;
  claimType1?: string;
  claimType2?: string;
  defectDescription?: string;
  defectQuantity?: number;
  purchaseAmount?: number;
  purchaseMethodCode?: string;
  /** ";" 구분 multipicklist value (최대 4) */
  requestTypeCode?: string;
}

export interface ClaimCreatePhotos {
  defectPhoto: File;
  labelPhoto: File;
  receiptPhoto?: File | null;
}

export async function createClaim(
  fields: ClaimCreateFields,
  photos: ClaimCreatePhotos
): Promise<{ claimId: number }> {
  const form = new FormData();
  Object.entries(fields).forEach(([k, v]) => {
    if (v !== undefined && v !== null) form.append(k, String(v));
  });
  form.append('defectPhoto', photos.defectPhoto);
  form.append('labelPhoto', photos.labelPhoto);
  if (photos.receiptPhoto) form.append('receiptPhoto', photos.receiptPhoto);
  const res = await client.post<ApiResponse<{ claimId: number }>>('/api/v1/mobile/claims', form);
  return unwrap(res, '클레임 등록에 실패했습니다');
}
