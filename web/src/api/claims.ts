import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface ClaimListRaw {
  content: ClaimListItemRaw[];
  total_elements: number;
  total_pages: number;
  page: number;
  size: number;
}

interface ClaimListItemRaw {
  claim_id: number;
  employee_name: string;
  employee_code: string;
  store_name: string | null;
  product_name: string | null;
  product_code: string | null;
  category_name: string | null;
  subcategory_name: string | null;
  defect_quantity: number | null;
  status: string;
  created_at: string;
}

interface ClaimDetailRaw {
  claim_id: number;
  employee_name: string;
  employee_code: string;
  store_name: string | null;
  product_code: string | null;
  product_name: string | null;
  date_type: string | null;
  date: string | null;
  category_name: string | null;
  subcategory_name: string | null;
  defect_description: string | null;
  defect_quantity: number | null;
  purchase_amount: number | null;
  purchase_method_name: string | null;
  request_type_name: string | null;
  status: string;
  created_at: string;
  photos: ClaimPhotoRaw[];
}

interface ClaimPhotoRaw {
  photo_id: number;
  photo_type: string;
  url: string;
  original_file_name: string | null;
}

// --- Frontend interfaces (camelCase) ---

export interface ClaimListParams {
  start_date?: string;
  end_date?: string;
  status?: string;
  employee_name?: string;
  store_name?: string;
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
  categoryName: string | null;
  subcategoryName: string | null;
  defectQuantity: number | null;
  status: string;
  createdAt: string;
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
  categoryName: string | null;
  subcategoryName: string | null;
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
  photoType: string;
  url: string;
  originalFileName: string | null;
}

// --- Mappers ---

function mapClaimList(raw: ClaimListRaw): ClaimListData {
  return {
    content: raw.content.map((item) => ({
      claimId: item.claim_id,
      employeeName: item.employee_name,
      employeeCode: item.employee_code,
      storeName: item.store_name,
      productName: item.product_name,
      productCode: item.product_code,
      categoryName: item.category_name,
      subcategoryName: item.subcategory_name,
      defectQuantity: item.defect_quantity,
      status: item.status,
      createdAt: item.created_at,
    })),
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
    page: raw.page,
    size: raw.size,
  };
}

function mapClaimDetail(raw: ClaimDetailRaw): ClaimDetail {
  return {
    claimId: raw.claim_id,
    employeeName: raw.employee_name,
    employeeCode: raw.employee_code,
    storeName: raw.store_name,
    productCode: raw.product_code,
    productName: raw.product_name,
    dateType: raw.date_type,
    date: raw.date,
    categoryName: raw.category_name,
    subcategoryName: raw.subcategory_name,
    defectDescription: raw.defect_description,
    defectQuantity: raw.defect_quantity,
    purchaseAmount: raw.purchase_amount,
    purchaseMethodName: raw.purchase_method_name,
    requestTypeName: raw.request_type_name,
    status: raw.status,
    createdAt: raw.created_at,
    photos: raw.photos.map((p) => ({
      photoId: p.photo_id,
      photoType: p.photo_type,
      url: p.url,
      originalFileName: p.original_file_name,
    })),
  };
}

// --- API functions ---

export async function fetchClaims(params: ClaimListParams): Promise<ClaimListData> {
  const res = await client.get<ApiResponse<ClaimListRaw>>('/api/v1/admin/claims', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 목록 조회에 실패했습니다');
  }
  return mapClaimList(res.data.data);
}

export async function fetchClaimDetail(claimId: number): Promise<ClaimDetail> {
  const res = await client.get<ApiResponse<ClaimDetailRaw>>(`/api/v1/admin/claims/${claimId}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '클레임 상세 조회에 실패했습니다');
  }
  return mapClaimDetail(res.data.data);
}
