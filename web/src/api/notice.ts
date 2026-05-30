import client from './client';
import type { ApiResponse } from './types';


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
  scope: string | null;
  title: string;
  branch: string | null;
  department: string | null;
  authorName: string | null;
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
  scope: string | null;
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
  scope: string;
  category: string;
  content: string;
  branch: string | null;
  branchCode: string | null;
}

export interface NoticeFormMeta {
  scopes: ScopeOption[];
  categories: CategoryOption[];
  branches: BranchOption[];
}

export interface ScopeOption {
  code: string;
  name: string;
}

export interface CategoryOption {
  code: string;
  name: string;
}

export interface BranchOption {
  branchCode: string;
  branchName: string;
}


// --- API functions ---

export async function fetchNotices(params: NoticeListParams): Promise<NoticeListData> {
  const res = await client.get<ApiResponse<NoticeListData>>('/api/v1/admin/notices', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공지사항 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchNoticeDetail(id: number): Promise<NoticeDetail> {
  const res = await client.get<ApiResponse<NoticeDetail>>(`/api/v1/admin/notices/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '공지사항 상세 조회에 실패했습니다');
  }
  return res.data.data;
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
  const res = await client.get<ApiResponse<NoticeFormMeta>>('/api/v1/admin/notices/form-meta');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '폼 메타데이터 조회에 실패했습니다');
  }
  return res.data.data;
}
