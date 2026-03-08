import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface ProductListRaw {
  content: ProductItemRaw[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

interface ProductItemRaw {
  id: number;
  product_code: string | null;
  name: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  standard_price: number | null;
  unit: string | null;
  storage_condition: string | null;
  product_status: string | null;
  launch_date: string | null;
}

// --- Frontend interfaces (camelCase) ---

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

// --- Mappers ---

function mapProductList(raw: ProductListRaw): ProductListData {
  return {
    content: raw.content.map((item) => ({
      id: item.id,
      productCode: item.product_code,
      name: item.name,
      category1: item.category1,
      category2: item.category2,
      category3: item.category3,
      standardPrice: item.standard_price,
      unit: item.unit,
      storageCondition: item.storage_condition,
      productStatus: item.product_status,
      launchDate: item.launch_date,
    })),
    page: raw.page,
    size: raw.size,
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
  };
}

// --- API functions ---

export async function fetchProducts(params: FetchProductsParams): Promise<ProductListData> {
  const res = await client.get<ApiResponse<ProductListRaw>>('/api/v1/admin/products', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '제품 목록 조회에 실패했습니다');
  }
  return mapProductList(res.data.data);
}

export async function fetchProductCategories(): Promise<CategoryTree[]> {
  const res = await client.get<ApiResponse<CategoryTree[]>>('/api/v1/admin/products/categories');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '카테고리 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
