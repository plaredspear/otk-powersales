import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface NoticeListRaw {
  content: NoticeSummaryRaw[];
  total_count: number;
  total_pages: number;
  current_page: number;
  size: number;
}

interface NoticeSummaryRaw {
  id: number;
  category: string;
  category_name: string;
  title: string;
  created_at: string;
}

interface NoticeDetailRaw {
  id: number;
  category: string;
  category_name: string;
  title: string;
  content: string;
  branch: string | null;
  branch_code: string | null;
  created_at: string;
  images: NoticeImageRaw[];
}

interface NoticeImageRaw {
  id: number;
  url: string;
  sort_order: number;
}

interface NoticeFormMetaRaw {
  categories: Array<{ code: string; name: string }>;
  branches: Array<{ branch_code: string; branch_name: string }>;
}

// --- Frontend interfaces (camelCase) ---

export interface NoticeListParams {
  category?: string;
  search?: string;
  page: number;
  size: number;
}

export interface NoticeSummary {
  id: number;
  category: string;
  categoryName: string;
  title: string;
  createdAt: string;
}

export interface NoticeListData {
  content: NoticeSummary[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export interface NoticeDetail {
  id: number;
  category: string;
  categoryName: string;
  title: string;
  content: string;
  branch: string | null;
  branchCode: string | null;
  createdAt: string;
  images: NoticeImage[];
}

export interface NoticeImage {
  id: number;
  url: string;
  sortOrder: number;
}

export interface NoticeFormData {
  title: string;
  category: string;
  content: string;
  branch: string | null;
  branch_code: string | null;
}

export interface NoticeFormMeta {
  categories: CategoryOption[];
  branches: BranchOption[];
}

export interface CategoryOption {
  code: string;
  name: string;
}

export interface BranchOption {
  branchCode: string;
  branchName: string;
}

// --- Mappers ---

function mapNoticeList(raw: NoticeListRaw): NoticeListData {
  return {
    content: raw.content.map((item) => ({
      id: item.id,
      category: item.category,
      categoryName: item.category_name,
      title: item.title,
      createdAt: item.created_at,
    })),
    totalCount: raw.total_count,
    totalPages: raw.total_pages,
    currentPage: raw.current_page,
    size: raw.size,
  };
}

function mapNoticeDetail(raw: NoticeDetailRaw): NoticeDetail {
  return {
    id: raw.id,
    category: raw.category,
    categoryName: raw.category_name,
    title: raw.title,
    content: raw.content,
    branch: raw.branch,
    branchCode: raw.branch_code,
    createdAt: raw.created_at,
    images: raw.images.map((img) => ({
      id: img.id,
      url: img.url,
      sortOrder: img.sort_order,
    })),
  };
}

function mapFormMeta(raw: NoticeFormMetaRaw): NoticeFormMeta {
  return {
    categories: raw.categories,
    branches: raw.branches.map((b) => ({
      branchCode: b.branch_code,
      branchName: b.branch_name,
    })),
  };
}

// --- API functions ---

export async function fetchNotices(params: NoticeListParams): Promise<NoticeListData> {
  const res = await client.get<ApiResponse<NoticeListRaw>>('/api/v1/admin/notices', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공지사항 목록 조회에 실패했습니다');
  }
  return mapNoticeList(res.data.data);
}

export async function fetchNoticeDetail(id: number): Promise<NoticeDetail> {
  const res = await client.get<ApiResponse<NoticeDetailRaw>>(`/api/v1/admin/notices/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공지사항 상세 조회에 실패했습니다');
  }
  return mapNoticeDetail(res.data.data);
}

export async function createNotice(data: NoticeFormData): Promise<void> {
  const res = await client.post<ApiResponse<unknown>>('/api/v1/admin/notices', data);
  if (!res.data.success) {
    throw new Error(res.data.message || '공지사항 등록에 실패했습니다');
  }
}

export async function updateNotice(id: number, data: NoticeFormData): Promise<void> {
  const res = await client.put<ApiResponse<unknown>>(`/api/v1/admin/notices/${id}`, data);
  if (!res.data.success) {
    throw new Error(res.data.message || '공지사항 수정에 실패했습니다');
  }
}

export async function deleteNotice(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/notices/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '공지사항 삭제에 실패했습니다');
  }
}

export async function fetchNoticeFormMeta(): Promise<NoticeFormMeta> {
  const res = await client.get<ApiResponse<NoticeFormMetaRaw>>('/api/v1/admin/notices/form-meta');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '폼 메타데이터 조회에 실패했습니다');
  }
  return mapFormMeta(res.data.data);
}
