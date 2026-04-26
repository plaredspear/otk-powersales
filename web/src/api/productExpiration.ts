import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface ProductExpirationItemRaw {
  id: number;
  seq: number;
  product_name: string;
  product_code: string;
  account_name: string;
  account_code: string;
  employee_name: string;
  employee_code: string;
  expiration_date: string;
  alarm_date: string;
  d_day: number;
  status: string;
  description: string | null;
  created_at: string;
  updated_at: string;
}

interface ProductExpirationListRaw {
  content: ProductExpirationItemRaw[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

interface ProductExpirationSummaryRaw {
  total_count: number;
  expired_count: number;
  imminent_count: number;
  normal_count: number;
}

interface BatchDeleteRaw {
  deleted_count: number;
}

// --- Frontend interfaces (camelCase) ---

export interface ProductExpiration {
  id: number;
  seq: number;
  productName: string;
  productCode: string;
  accountName: string;
  accountCode: string;
  employeeName: string;
  employeeCode: string;
  expirationDate: string;
  alarmDate: string;
  dDay: number;
  status: 'EXPIRED' | 'IMMINENT' | 'NORMAL';
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductExpirationListData {
  content: ProductExpiration[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductExpirationSummary {
  totalCount: number;
  expiredCount: number;
  imminentCount: number;
  normalCount: number;
}

export interface FetchProductExpirationsParams {
  from_date?: string;
  to_date?: string;
  employee_keyword?: string;
  account_keyword?: string;
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateProductExpirationRequest {
  employee_code: string;
  account_code: string;
  product_code: string;
  expiration_date: string;
  alarm_date: string;
  description?: string;
}

export interface UpdateProductExpirationRequest {
  expiration_date: string;
  alarm_date: string;
  description?: string;
}

// --- Mappers ---

function mapItem(raw: ProductExpirationItemRaw): ProductExpiration {
  return {
    id: raw.id,
    seq: raw.seq,
    productName: raw.product_name,
    productCode: raw.product_code,
    accountName: raw.account_name,
    accountCode: raw.account_code,
    employeeName: raw.employee_name,
    employeeCode: raw.employee_code,
    expirationDate: raw.expiration_date,
    alarmDate: raw.alarm_date,
    dDay: raw.d_day,
    status: raw.status as ProductExpiration['status'],
    description: raw.description,
    createdAt: raw.created_at,
    updatedAt: raw.updated_at,
  };
}

function mapList(raw: ProductExpirationListRaw): ProductExpirationListData {
  return {
    content: raw.content.map(mapItem),
    page: raw.page,
    size: raw.size,
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
  };
}

function mapSummary(raw: ProductExpirationSummaryRaw): ProductExpirationSummary {
  return {
    totalCount: raw.total_count,
    expiredCount: raw.expired_count,
    imminentCount: raw.imminent_count,
    normalCount: raw.normal_count,
  };
}

// --- API functions ---

export async function fetchProductExpirations(params: FetchProductExpirationsParams): Promise<ProductExpirationListData> {
  const res = await client.get<ApiResponse<ProductExpirationListRaw>>('/api/v1/admin/product-expiration', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 목록 조회에 실패했습니다');
  }
  return mapList(res.data.data);
}

export async function fetchProductExpirationSummary(): Promise<ProductExpirationSummary> {
  const res = await client.get<ApiResponse<ProductExpirationSummaryRaw>>('/api/v1/admin/product-expiration/summary');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 요약 조회에 실패했습니다');
  }
  return mapSummary(res.data.data);
}

export async function createProductExpiration(payload: CreateProductExpirationRequest): Promise<ProductExpiration> {
  const res = await client.post<ApiResponse<ProductExpirationItemRaw>>('/api/v1/admin/product-expiration', payload);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 등록에 실패했습니다');
  }
  return mapItem(res.data.data);
}

export async function updateProductExpiration(id: number, payload: UpdateProductExpirationRequest): Promise<ProductExpiration> {
  const res = await client.put<ApiResponse<ProductExpirationItemRaw>>(`/api/v1/admin/product-expiration/${id}`, payload);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 수정에 실패했습니다');
  }
  return mapItem(res.data.data);
}

export async function deleteProductExpiration(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<null>>(`/api/v1/admin/product-expiration/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '유통기한 삭제에 실패했습니다');
  }
}

export async function batchDeleteProductExpirations(ids: number[]): Promise<number> {
  const res = await client.post<ApiResponse<BatchDeleteRaw>>('/api/v1/admin/product-expiration/batch-delete', { ids });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '유통기한 일괄 삭제에 실패했습니다');
  }
  return res.data.data.deleted_count;
}
