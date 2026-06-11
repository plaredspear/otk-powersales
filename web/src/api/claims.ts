import client from './client';
import type { ApiResponse } from './types';


export interface ClaimListParams {
  startDate?: string;
  endDate?: string;
  status?: string;
  employeeName?: string;
  storeName?: string;
  page?: number;
  size?: number;
}

export interface ClaimListItem {
  claimId: number;
  employeeName: string;
  employeeCode: string;
  storeName: string | null;
  productName: string | null;
  productCode: string | null;
  categoryValue: string | null;
  categoryLabel: string | null;
  subcategoryValue: string | null;
  subcategoryLabel: string | null;
  defectQuantity: number | null;
  status: string;
  createdAt: string;
  /** 카드 뷰 배경용 대표 이미지 URL (불량 사진 우선, 없으면 첫 사진). 사진 없으면 null. */
  representativeImageUrl: string | null;
}

export interface ClaimListData {
  content: ClaimListItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ClaimDetail {
  claimId: number;
  employeeName: string;
  employeeCode: string;
  storeName: string | null;
  productCode: string | null;
  productName: string | null;
  dateType: string | null;
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
  createdAt: string;
  photos: ClaimPhoto[];
}

export interface ClaimPhoto {
  photoId: number;
  photoType: string | null;
  url: string;
  originalFileName: string | null;
}


// --- Spec #829: Admin 클레임 등록 + SF 재전송 ---

export type ClaimSendStatus = 'SENT' | 'SEND_FAILED' | 'SF_PENDING' | 'DRAFT';

export interface AdminClaimCreateInput {
  sapAccountCode: string;
  productCode: string;
  employeeCode: string;
  dateType: 'EXPIRY_DATE' | 'MANUFACTURE_DATE';
  expirationDate?: string;
  manufacturingDate?: string;
  claimDate: string;
  claimType1: string;
  claimType2: string;
  quantity: string;
  description: string;
  purchaseMethod?: 'A' | 'B' | 'C';
  amount?: string;
  requestType?: string;
  claimPhoto: File;
  partPhoto: File;
  receiptPhoto?: File;
}

export interface AdminClaimCreateResult {
  claimId: number;
  status: ClaimSendStatus;
  sfResultCode: string | null;
  sfResultMsg: string | null;
}

function appendIfDefined(form: FormData, key: string, value: unknown) {
  if (value === undefined || value === null) return;
  if (value instanceof File) {
    form.append(key, value);
  } else {
    form.append(key, String(value));
  }
}

// --- API functions ---

export async function fetchClaims(params: ClaimListParams): Promise<ClaimListData> {
  const res = await client.get<ApiResponse<ClaimListData>>('/api/v1/admin/claims', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchClaimDetail(claimId: number): Promise<ClaimDetail> {
  const res = await client.get<ApiResponse<ClaimDetail>>(`/api/v1/admin/claims/${claimId}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createClaim(input: AdminClaimCreateInput): Promise<AdminClaimCreateResult> {
  const form = new FormData();
  appendIfDefined(form, 'sapAccountCode', input.sapAccountCode);
  appendIfDefined(form, 'productCode', input.productCode);
  appendIfDefined(form, 'employeeCode', input.employeeCode);
  appendIfDefined(form, 'dateType', input.dateType);
  appendIfDefined(form, 'expirationDate', input.expirationDate);
  appendIfDefined(form, 'manufacturingDate', input.manufacturingDate);
  appendIfDefined(form, 'claimDate', input.claimDate);
  appendIfDefined(form, 'claimType1', input.claimType1);
  appendIfDefined(form, 'claimType2', input.claimType2);
  appendIfDefined(form, 'quantity', input.quantity);
  appendIfDefined(form, 'description', input.description);
  appendIfDefined(form, 'purchaseMethod', input.purchaseMethod);
  appendIfDefined(form, 'amount', input.amount);
  appendIfDefined(form, 'requestType', input.requestType);
  form.append('claimPhoto', input.claimPhoto);
  form.append('partPhoto', input.partPhoto);
  if (input.receiptPhoto) {
    form.append('receiptPhoto', input.receiptPhoto);
  }

  const res = await client.post<ApiResponse<AdminClaimCreateResult>>('/api/v1/admin/claims', form);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function resendClaim(claimId: number): Promise<AdminClaimCreateResult> {
  const res = await client.post<ApiResponse<AdminClaimCreateResult>>(`/api/v1/admin/claims/${claimId}/sf-resend`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SF 재전송에 실패했습니다');
  }
  return res.data.data;
}

/**
 * SF ClaimRegist 전송 테스트 입력 (개발자 도구 — 외부 API 테스트).
 *
 * 운영 등록([AdminClaimCreateInput])과 달리 이미지 3종이 모두 선택적이다 (SF 계약 검증 목적).
 */
export interface ClaimRegistTestInput {
  sapAccountCode: string;
  productCode: string;
  employeeCode: string;
  dateType: 'EXPIRY_DATE' | 'MANUFACTURE_DATE';
  expirationDate?: string;
  manufacturingDate?: string;
  claimDate: string;
  claimType1: string;
  claimType2: string;
  quantity: string;
  description: string;
  purchaseMethod?: string;
  amount?: string;
  requestType?: string;
  claimPhoto?: File;
  partPhoto?: File;
  receiptPhoto?: File;
}

export interface ClaimRegistTestResult {
  success: boolean;
  resultCode: string | null;
  resultMsg: string | null;
  rawResponse: string | null;
  /** SF 로 전송한 apiMap JSON (이미지 Buffer 는 길이 표기로 마스킹). */
  requestPayload: string;
}

export async function testClaimRegist(
  input: ClaimRegistTestInput,
): Promise<ClaimRegistTestResult> {
  const form = new FormData();
  appendIfDefined(form, 'sapAccountCode', input.sapAccountCode);
  appendIfDefined(form, 'productCode', input.productCode);
  appendIfDefined(form, 'employeeCode', input.employeeCode);
  appendIfDefined(form, 'dateType', input.dateType);
  appendIfDefined(form, 'expirationDate', input.expirationDate);
  appendIfDefined(form, 'manufacturingDate', input.manufacturingDate);
  appendIfDefined(form, 'claimDate', input.claimDate);
  appendIfDefined(form, 'claimType1', input.claimType1);
  appendIfDefined(form, 'claimType2', input.claimType2);
  appendIfDefined(form, 'quantity', input.quantity);
  appendIfDefined(form, 'description', input.description);
  appendIfDefined(form, 'purchaseMethod', input.purchaseMethod);
  appendIfDefined(form, 'amount', input.amount);
  appendIfDefined(form, 'requestType', input.requestType);
  appendIfDefined(form, 'claimPhoto', input.claimPhoto);
  appendIfDefined(form, 'partPhoto', input.partPhoto);
  appendIfDefined(form, 'receiptPhoto', input.receiptPhoto);

  const res = await client.post<ApiResponse<ClaimRegistTestResult>>(
    '/api/v1/admin/claim-regist/test',
    form,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'SF ClaimRegist 전송 테스트에 실패했습니다');
  }
  return res.data.data;
}
