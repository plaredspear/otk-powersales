import client from './client';
import type { ApiResponse } from './types';


export interface PromotionListParams {
  keyword?: string;
  promotionType?: string;
  startDate?: string;
  endDate?: string;
  /** 거래처명/거래처코드(externalKey) 통합 like 검색 (진열스케줄마스터 정합). */
  accountName?: string;
  /** 거래처번호(Account.accountNumber) like 검색 — 거래처코드와 별개 필드. */
  accountNumber?: string;
  /** 제품유형(목록 "제품유형" 컬럼 = 대표제품 storeConditionText 파생값) like 검색. */
  category1?: string;
  /** 대표제품명(Product.name)/제품코드(Product.productCode) OR like 검색. */
  primaryProduct?: string;
  /** 배정된 행사사원의 사번(Employee.employeeCode)/성명(Employee.name) OR like EXISTS 필터. */
  employeeKeyword?: string;
  /** true 면 내가 owner 인 행사만 (SF ListView filterScope=Mine 대응). */
  ownerOnly?: boolean;
  /** 지점 스코프 — 행사 소속 지점(costCenterCode) 필터. 전사 권한자만 선택 가능(그 외는 본인 지점 자동 스코프). */
  branchCode?: string;
  page: number;
  size: number;
}

export interface PromotionListItem {
  id: number;
  promotionNumber: string;
  promotionName: string | null;
  promotionType: string | null;
  accountName: string | null;
  accountCode: string | null;
  accountStatusName: string | null;
  startDate: string;
  endDate: string;
  primaryProductName: string | null;
  primaryProductCode: string | null;
  standLocation: string | null;
  productType: string | null;
  category1: string | null;
  isClosed: boolean;
  costCenterCode: string | null;
  targetAmount: number | null;
  actualAmount: number | null;
  createdById: number | null;
  createdByName: string | null;
  remark: string | null;
  isDeleted: boolean;
  createdAt: string;
}

export interface PromotionListData {
  content: PromotionListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PromotionDetail {
  id: number;
  promotionNumber: string;
  promotionName: string | null;
  promotionType: string | null;
  accountId: number;
  accountName: string | null;
  accountCode: string | null;
  startDate: string;
  endDate: string;
  primaryProductId: number | null;
  primaryProductName: string | null;
  primaryProductCode: string | null;
  otherProduct: string | null;
  message: string | null;
  standLocation: string | null;
  costCenterCode: string | null;
  productType: string | null;
  category1: string | null;
  targetAmount: number | null;
  actualAmount: number | null;
  isClosed: boolean;
  remark: string | null;
  isDeleted: boolean;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionFormData {
  promotionType: string;
  accountId: number;
  startDate: string;
  endDate: string;
  primaryProductId: number;
  otherProduct?: string | null;
  message?: string | null;
  standLocation: string;
  remark?: string | null;
}

// --- Form-Meta interfaces ---


export interface PromotionFormMeta {
  promotionTypes: { value: string; name: string }[];
  standLocations: { value: string; name: string }[];
}


// --- API functions ---

/** 행사마스터 목록 화면 지점 셀렉터 옵션. */
export interface PromotionBranch {
  branchCode: string;
  branchName: string;
}

/**
 * 행사마스터 목록 화면 지점 셀렉터 옵션 조회.
 *
 * 전사 권한자는 전 지점, 그 외는 본인 지점 1건을 반환한다(권한별 화이트리스트).
 * 프론트는 응답 길이로 단일/다중을 판별한다(단일이면 Tag 표시, 다중이면 Select).
 */
export async function fetchPromotionBranches(): Promise<PromotionBranch[]> {
  const res = await client.get<ApiResponse<PromotionBranch[]>>('/api/v1/admin/promotions/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPromotionFormMeta(): Promise<PromotionFormMeta> {
  const res = await client.get<ApiResponse<PromotionFormMeta>>('/api/v1/admin/promotions/form-meta');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 폼 메타 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchPromotions(params: PromotionListParams): Promise<PromotionListData> {
  const queryParams: Record<string, string | number> = {
    page: params.page,
    size: params.size,
  };
  if (params.keyword) queryParams.keyword = params.keyword;
  if (params.promotionType) queryParams.promotionType = params.promotionType;
  if (params.startDate) queryParams.startDate = params.startDate;
  if (params.endDate) queryParams.endDate = params.endDate;
  if (params.accountName) queryParams.accountName = params.accountName;
  if (params.accountNumber) queryParams.accountNumber = params.accountNumber;
  if (params.category1) queryParams.category1 = params.category1;
  if (params.primaryProduct) queryParams.primaryProduct = params.primaryProduct;
  if (params.employeeKeyword) queryParams.employeeKeyword = params.employeeKeyword;
  if (params.ownerOnly) queryParams.ownerOnly = 'true';
  if (params.branchCode) queryParams.branchCode = params.branchCode;

  const res = await client.get<ApiResponse<PromotionListData>>('/api/v1/admin/promotions', {
    params: queryParams,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사마스터 목록 엑셀 다운로드 쿼리 파라미터 빌더 (page/size 제외, 목록과 동일 검색 조건).
 * 실제 다운로드는 공통 `downloadExcel`/`useExcelDownload` 가 수행한다.
 */
export function promotionExportParams(
  params: Omit<PromotionListParams, 'page' | 'size'>,
): Record<string, string> {
  const queryParams: Record<string, string> = {};
  if (params.keyword) queryParams.keyword = params.keyword;
  if (params.promotionType) queryParams.promotionType = params.promotionType;
  if (params.startDate) queryParams.startDate = params.startDate;
  if (params.endDate) queryParams.endDate = params.endDate;
  if (params.accountName) queryParams.accountName = params.accountName;
  if (params.accountNumber) queryParams.accountNumber = params.accountNumber;
  if (params.category1) queryParams.category1 = params.category1;
  if (params.primaryProduct) queryParams.primaryProduct = params.primaryProduct;
  if (params.employeeKeyword) queryParams.employeeKeyword = params.employeeKeyword;
  if (params.ownerOnly) queryParams.ownerOnly = 'true';
  if (params.branchCode) queryParams.branchCode = params.branchCode;
  return queryParams;
}

export async function fetchPromotion(id: number): Promise<PromotionDetail> {
  const res = await client.get<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createPromotion(data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetail>>('/api/v1/admin/promotions', data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updatePromotion(id: number, data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.put<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deletePromotion(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/promotions/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '행사마스터 삭제에 실패했습니다');
  }
}

/**
 * 행사마스터 복제 (폼 방식 — UC-11).
 *
 * 원본 sourceId 의 검증/CC 자동 채움/대표상품 1건 생성 (T1/T2/T7) 을 재사용. 신규 행사사원 11개 이력성 필드 초기화.
 */
export async function clonePromotion(sourceId: number, data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${sourceId}/clone`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 복제에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사마스터 자식 포함 복제 — 1클릭 (UC-12).
 *
 * 원본 필드 전체 + 행사사원 5필드만 복사 (담당자/근무유형). body 없음.
 */
export async function cloneWithChildren(sourceId: number): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetail>>(`/api/v1/admin/promotions/${sourceId}/clone-with-children`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 자식 포함 복제에 실패했습니다');
  }
  return res.data.data;
}
