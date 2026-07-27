import client from './client';
import type { ApiResponse } from './types';


export interface FetchProductsParams {
  keyword?: string;
  category1?: string;
  category2?: string;
  category3?: string;
  productStatus?: string;
  page?: number;
  size?: number;
}

export interface ProductDetail {
  id: number;
  productCode: string | null;
  name: string | null;
  barcode: string | null;
  logisticsBarcode: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  categoryCode1: string | null;
  categoryCode2: string | null;
  categoryCode3: string | null;
  unit: string | null;
  orderingUnit: string | null;
  conversionQuantity: number | null;
  boxReceivingQuantity: number | null;
  standardUnitPrice: number | null;
  superTax: number | null;
  launchDate: string | null;
  storageCondition: string | null;
  productStatus: string | null;
  productType: string | null;
  shelfLife: string | null;
  shelfLifeUnit: string | null;
  tasteGift: string | null;
  productFeatures: string | null;
  sellingPoint: string | null;
  purpose: string | null;
  targetAccountType: string | null;
  allergen: string | null;
  crossContamination: string | null;
  imgRefPathFront: string | null;
  imgRefPathBack: string | null;
  pallet: number | null;
  manufacture: string | null;
  manufactureDetail: string | null;
  claimManagement: string | null;
  createdAt: string;
  lastModifiedAt: string;
  barcodes: ProductBarcodeItem[];
}

export interface ProductBarcodeItem {
  id: number;
  barcode: string | null;
  unit: string | null;
  sortOrder: string | null;
  productName: string | null;
}

export interface InventorySearchRequest {
  accountId: number;
  productCodes: string[];
  deliveryRequestDate: string;
}

export interface InventorySearchResultItem {
  productCode: string;
  productName: string | null;
  unit: string | null;
  conversionQuantity: number;
  supplyLimitQuantity: number;
  unitPrice: number;
  message: string | null;
}

export interface InventorySearchResponse {
  results: InventorySearchResultItem[];
}

export interface Product {
  id: number;
  productCode: string | null;
  name: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  standardUnitPrice: number | null;
  unit: string | null;
  storageCondition: string | null;
  productStatus: string | null;
  launchDate: string | null;
  superTax: number | null;
  shelfLife: string | null;
  shelfLifeUnit: string | null;
  tasteGift: string | null;
  lastModifiedAt: string | null;
}

export interface ProductListData {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CategoryTree {
  category1: string;
  children: Category2Node[];
}

export interface Category2Node {
  category2: string;
  children: string[];
}


// --- API functions ---

export async function fetchProducts(params: FetchProductsParams): Promise<ProductListData> {
  const res = await client.get<ApiResponse<ProductListData>>('/api/v1/admin/products', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제품 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사마스터 등록/수정 화면의 제품 lookup search — promotion 권한 보유자 호출용.
 *
 * Product READ 권한 없이도 호출 가능 (SF lookup search 메커니즘 정합).
 */
export async function fetchProductsForPromotionLookup(
  params: Pick<
    FetchProductsParams,
    'keyword' | 'category1' | 'category2' | 'category3' | 'productStatus' | 'page' | 'size'
  >,
): Promise<ProductListData> {
  const res = await client.get<ApiResponse<ProductListData>>('/api/v1/admin/products/lookup', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제품 검색에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 행사마스터 제품 고급 검색 모달의 필터 드롭다운 옵션 (`GET /api/v1/admin/products/lookup-filter-options`).
 *
 * 거래처 고급 검색의 `fetchPromotionLookupFilterOptions` 와 동일 패턴 — 모달 오픈 시점에만 호출한다.
 * `/categories` 는 product READ 권한 가드라 행사마스터 권한만 가진 사용자가 403 이 되므로 재사용 불가.
 */
export interface ProductLookupFilterOptions {
  categories: CategoryTree[];
  /** 제품상태 선택지 — SF picklist 가 현재 '-' 1개뿐이라 선택지도 1개다. */
  productStatuses: string[];
}

export async function fetchProductLookupFilterOptions(): Promise<ProductLookupFilterOptions> {
  const res = await client.get<ApiResponse<ProductLookupFilterOptions>>(
    '/api/v1/admin/products/lookup-filter-options',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제품 검색 필터 옵션 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 물류 클레임 등록/수정 화면의 제품 lookup search — suggestion 권한 보유자 호출용.
 *
 * Product READ 권한 없이도 호출 가능 (SF Claim__c.ProductId__c Lookup 메커니즘 정합).
 */
export async function fetchProductsForClaimLookup(
  params: Pick<FetchProductsParams, 'keyword' | 'page' | 'size'>,
): Promise<ProductListData> {
  const res = await client.get<ApiResponse<ProductListData>>('/api/v1/admin/products/lookup-for-claim', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제품 검색에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchProductCategories(): Promise<CategoryTree[]> {
  const res = await client.get<ApiResponse<CategoryTree[]>>('/api/v1/admin/products/categories');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '카테고리 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchProductDetail(productCode: string): Promise<ProductDetail> {
  const res = await client.get<ApiResponse<ProductDetail>>(
    `/api/v1/admin/products/${encodeURIComponent(productCode)}`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제품 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function searchInventory(
  request: InventorySearchRequest,
): Promise<InventorySearchResponse> {
  const res = await client.post<ApiResponse<InventorySearchResponse>>(
    '/api/v1/admin/products/inventory-search',
    request,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '재고조회에 실패했습니다');
  }
  return res.data.data;
}

/** 제품 목록 엑셀 다운로드 엔드포인트 — 조회 조건 결과 전체가 대상 (화면 체크 선택과 무관). */
export const PRODUCTS_EXPORT_EXCEL_PATH = '/api/v1/admin/products/export-excel';
