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

export interface Product {
  id: number;
  productCode: string | null;
  name: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  standardPrice: number | null;
  unit: string | null;
  storageCondition: string | null;
  productStatus: string | null;
  launchDate: string | null;
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
