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

interface Category2Node {
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

export async function downloadProductsExcel(productCodes: string[]): Promise<Blob> {
  const res = await client.post(
    '/api/v1/admin/products/export-excel',
    { productCodes },
    { responseType: 'blob' },
  );
  return res.data as Blob;
}
