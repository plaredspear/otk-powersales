import client from './client';
import type { ApiResponse } from './types';


export interface EducationListParams {
  category?: string;
  search?: string;
  page: number;
  size: number;
}

export interface EducationSummary {
  eduId: string;
  eduTitle: string;
  eduCode: string;
  eduCodeNm: string;
  instDate: string;
  attachmentCount: number;
}

export interface EducationListData {
  content: EducationSummary[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export interface EducationDetail {
  id: string;
  category: string;
  categoryName: string;
  title: string;
  content: string;
  createdAt: string;
  attachments: EducationAttachment[];
}

export interface EducationAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
}

export interface EducationCategory {
  eduCode: string;
  eduCodeNm: string;
}


// --- API functions ---

export async function fetchEducations(params: EducationListParams): Promise<EducationListData> {
  const res = await client.get<ApiResponse<EducationListData>>('/api/v1/admin/education/posts', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '교육 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchEducationDetail(id: string): Promise<EducationDetail> {
  const res = await client.get<ApiResponse<EducationDetail>>(`/api/v1/admin/education/posts/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '교육 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createEducation(formData: FormData): Promise<void> {
  const res = await client.post<ApiResponse<unknown>>('/api/v1/admin/education/posts', formData);
  if (!res.data.success) {
    throw new Error(res.data.message || '교육 등록에 실패했습니다');
  }
}

export async function updateEducation(id: string, formData: FormData): Promise<void> {
  const res = await client.put<ApiResponse<unknown>>(`/api/v1/admin/education/posts/${id}`, formData);
  if (!res.data.success) {
    throw new Error(res.data.message || '교육 수정에 실패했습니다');
  }
}

export async function deleteEducation(id: string): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/education/posts/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '교육 삭제에 실패했습니다');
  }
}

export async function fetchEducationCategories(): Promise<EducationCategory[]> {
  const res = await client.get<ApiResponse<EducationCategory[]>>('/api/v1/admin/education/categories');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '카테고리 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
