import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface PromotionListRaw {
  content: PromotionListItemRaw[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

interface PromotionListItemRaw {
  id: number;
  promotion_number: string;
  promotion_name: string | null;
  promotion_type_id: number | null;
  promotion_type_name: string | null;
  account_name: string | null;
  start_date: string;
  end_date: string;
  target_amount: number | null;
  actual_amount: number | null;
  category: string | null;
  product_type: string | null;
  branch_name: string | null;
  is_closed: boolean;
  cost_center_code: string | null;
  remark: string | null;
  is_deleted: boolean;
  created_at: string;
}

interface PromotionDetailRaw {
  id: number;
  promotion_number: string;
  promotion_name: string | null;
  promotion_type_id: number | null;
  promotion_type_name: string | null;
  account_id: number;
  account_name: string | null;
  start_date: string;
  end_date: string;
  primary_product_id: number | null;
  primary_product_name: string | null;
  other_product: string | null;
  message: string | null;
  stand_location: string | null;
  target_amount: number | null;
  actual_amount: number | null;
  cost_center_code: string | null;
  category: string | null;
  product_type: string | null;
  branch_name: string | null;
  is_closed: boolean;
  remark: string | null;
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

// --- Frontend interfaces (camelCase) ---

export interface PromotionListParams {
  keyword?: string;
  promotionTypeId?: number;
  category?: string;
  startDate?: string;
  endDate?: string;
  page: number;
  size: number;
}

export interface PromotionListItem {
  id: number;
  promotionNumber: string;
  promotionName: string | null;
  promotionTypeId: number | null;
  promotionTypeName: string | null;
  accountName: string | null;
  startDate: string;
  endDate: string;
  targetAmount: number | null;
  actualAmount: number | null;
  category: string | null;
  productType: string | null;
  branchName: string | null;
  isClosed: boolean;
  costCenterCode: string | null;
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
  promotionTypeId: number | null;
  promotionTypeName: string | null;
  accountId: number;
  accountName: string | null;
  startDate: string;
  endDate: string;
  primaryProductId: number | null;
  primaryProductName: string | null;
  otherProduct: string | null;
  message: string | null;
  standLocation: string | null;
  targetAmount: number | null;
  actualAmount: number | null;
  costCenterCode: string | null;
  category: string | null;
  productType: string | null;
  branchName: string | null;
  isClosed: boolean;
  remark: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionFormData {
  promotion_type_id: number;
  account_id: number;
  start_date: string;
  end_date: string;
  primary_product_id?: number | null;
  other_product?: string | null;
  message?: string | null;
  stand_location: string;
  remark?: string | null;
  branch_name?: string | null;
}

// --- Form-Meta interfaces ---

interface PromotionFormMetaRaw {
  promotion_types: { id: number; name: string }[];
  stand_locations: { value: string; name: string }[];
}

export interface PromotionFormMeta {
  promotionTypes: { id: number; name: string }[];
  standLocations: { value: string; name: string }[];
}

// --- Mappers ---

function mapPromotionList(raw: PromotionListRaw): PromotionListData {
  return {
    content: raw.content.map((item) => ({
      id: item.id,
      promotionNumber: item.promotion_number,
      promotionName: item.promotion_name,
      promotionTypeId: item.promotion_type_id,
      promotionTypeName: item.promotion_type_name,
      accountName: item.account_name,
      startDate: item.start_date,
      endDate: item.end_date,
      targetAmount: item.target_amount,
      actualAmount: item.actual_amount,
      category: item.category,
      productType: item.product_type,
      branchName: item.branch_name,
      isClosed: item.is_closed,
      remark: item.remark,
      costCenterCode: item.cost_center_code,
      isDeleted: item.is_deleted,
      createdAt: item.created_at,
    })),
    page: raw.page,
    size: raw.size,
    totalElements: raw.total_elements,
    totalPages: raw.total_pages,
  };
}

function mapPromotionDetail(raw: PromotionDetailRaw): PromotionDetail {
  return {
    id: raw.id,
    promotionNumber: raw.promotion_number,
    promotionName: raw.promotion_name,
    promotionTypeId: raw.promotion_type_id,
    promotionTypeName: raw.promotion_type_name,
    accountId: raw.account_id,
    accountName: raw.account_name,
    startDate: raw.start_date,
    endDate: raw.end_date,
    primaryProductId: raw.primary_product_id,
    primaryProductName: raw.primary_product_name,
    otherProduct: raw.other_product,
    message: raw.message,
    standLocation: raw.stand_location,
    targetAmount: raw.target_amount,
    actualAmount: raw.actual_amount,
    costCenterCode: raw.cost_center_code,
    category: raw.category,
    productType: raw.product_type,
    branchName: raw.branch_name,
    isClosed: raw.is_closed,
    remark: raw.remark,
    isDeleted: raw.is_deleted,
    createdAt: raw.created_at,
    updatedAt: raw.updated_at,
  };
}

function mapFormMeta(raw: PromotionFormMetaRaw): PromotionFormMeta {
  return {
    promotionTypes: raw.promotion_types,
    standLocations: raw.stand_locations,
  };
}

// --- API functions ---

export async function fetchPromotionFormMeta(): Promise<PromotionFormMeta> {
  const res = await client.get<ApiResponse<PromotionFormMetaRaw>>('/api/v1/admin/promotions/form-meta');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 폼 메타 조회에 실패했습니다');
  }
  return mapFormMeta(res.data.data);
}

export async function fetchPromotions(params: PromotionListParams): Promise<PromotionListData> {
  const queryParams: Record<string, string | number> = {
    page: params.page,
    size: params.size,
  };
  if (params.keyword) queryParams.keyword = params.keyword;
  if (params.promotionTypeId) queryParams.promotionTypeId = params.promotionTypeId;
  if (params.category) queryParams.category = params.category;
  if (params.startDate) queryParams.startDate = params.startDate;
  if (params.endDate) queryParams.endDate = params.endDate;

  const res = await client.get<ApiResponse<PromotionListRaw>>('/api/v1/admin/promotions', {
    params: queryParams,
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 목록 조회에 실패했습니다');
  }
  return mapPromotionList(res.data.data);
}

export async function fetchPromotion(id: number): Promise<PromotionDetail> {
  const res = await client.get<ApiResponse<PromotionDetailRaw>>(`/api/v1/admin/promotions/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 상세 조회에 실패했습니다');
  }
  return mapPromotionDetail(res.data.data);
}

export async function createPromotion(data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.post<ApiResponse<PromotionDetailRaw>>('/api/v1/admin/promotions', data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 등록에 실패했습니다');
  }
  return mapPromotionDetail(res.data.data);
}

export async function updatePromotion(id: number, data: PromotionFormData): Promise<PromotionDetail> {
  const res = await client.put<ApiResponse<PromotionDetailRaw>>(`/api/v1/admin/promotions/${id}`, data);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '행사마스터 수정에 실패했습니다');
  }
  return mapPromotionDetail(res.data.data);
}

export async function deletePromotion(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/promotions/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '행사마스터 삭제에 실패했습니다');
  }
}
