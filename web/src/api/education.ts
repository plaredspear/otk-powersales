import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface EducationListRaw {
  content: EducationSummaryRaw[];
  total_count: number;
  total_pages: number;
  current_page: number;
  size: number;
}

interface EducationSummaryRaw {
  edu_id: string;
  edu_title: string;
  edu_code: string;
  edu_code_nm: string;
  inst_date: string;
  attachment_count: number;
}

interface EducationDetailRaw {
  id: string;
  category: string;
  category_name: string;
  title: string;
  content: string;
  created_at: string;
  attachments: EducationAttachmentRaw[];
}

interface EducationAttachmentRaw {
  id: string;
  file_name: string;
  file_url: string;
  file_size: number;
}

interface EducationCategoryRaw {
  edu_code: string;
  edu_code_nm: string;
}

// --- Frontend interfaces (camelCase) ---

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

// --- Mappers ---

function mapEducationList(raw: EducationListRaw): EducationListData {
  return {
    content: raw.content.map((item) => ({
      eduId: item.edu_id,
      eduTitle: item.edu_title,
      eduCode: item.edu_code,
      eduCodeNm: item.edu_code_nm,
      instDate: item.inst_date,
      attachmentCount: item.attachment_count,
    })),
    totalCount: raw.total_count,
    totalPages: raw.total_pages,
    currentPage: raw.current_page,
    size: raw.size,
  };
}

function mapEducationDetail(raw: EducationDetailRaw): EducationDetail {
  return {
    id: raw.id,
    category: raw.category,
    categoryName: raw.category_name,
    title: raw.title,
    content: raw.content,
    createdAt: raw.created_at,
    attachments: raw.attachments.map((att) => ({
      id: att.id,
      fileName: att.file_name,
      fileUrl: att.file_url,
      fileSize: att.file_size,
    })),
  };
}

function mapCategories(raw: EducationCategoryRaw[]): EducationCategory[] {
  return raw.map((c) => ({
    eduCode: c.edu_code,
    eduCodeNm: c.edu_code_nm,
  }));
}

// --- API functions ---

export async function fetchEducations(params: EducationListParams): Promise<EducationListData> {
  const res = await client.get<ApiResponse<EducationListRaw>>('/api/v1/admin/education/posts', { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '교육 목록 조회에 실패했습니다');
  }
  return mapEducationList(res.data.data);
}

export async function fetchEducationDetail(id: string): Promise<EducationDetail> {
  const res = await client.get<ApiResponse<EducationDetailRaw>>(`/api/v1/admin/education/posts/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '교육 상세 조회에 실패했습니다');
  }
  return mapEducationDetail(res.data.data);
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
  const res = await client.get<ApiResponse<EducationCategoryRaw[]>>('/api/v1/admin/education/categories');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '카테고리 목록 조회에 실패했습니다');
  }
  return mapCategories(res.data.data);
}
