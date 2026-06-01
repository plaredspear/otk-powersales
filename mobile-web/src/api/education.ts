import client from './client';
import { unwrap, type ApiResponse } from './types';

/**
 * 교육 카테고리 (edu_code).
 *
 * ⚠️ 결정 필요(트리아지 T1): backend 에 카테고리 목록 메타 엔드포인트가 없어
 * 레거시 `edu/main.jsp` 의 카테고리 셋을 코드 상수로 임시 정의한다. 정확한 코드/명칭은
 * backend edu_code 마스터로 확정해야 한다(현재는 알려진 TASTING_MANUAL + 추정 카테고리).
 */
export const EDUCATION_CATEGORIES: { code: string; name: string }[] = [
  { code: 'TASTING_MANUAL', name: '시식 매뉴얼' },
  { code: 'PRODUCT_GUIDE', name: '제품 안내' },
  { code: 'SALES_GUIDE', name: '판매 가이드' },
  { code: 'ETC', name: '기타 자료' },
];

/** backend `EducationPostSummaryResponse` */
export interface EducationSummary {
  id: string;
  title: string;
  createdAt: string | null;
}

/** backend `EducationPostListResponse` */
export interface EducationListData {
  content: EducationSummary[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

/** backend `EducationAttachmentResponse` */
export interface EducationAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
}

/** backend `EducationPostDetailResponse` */
export interface EducationDetail {
  id: string;
  category: string;
  categoryName: string;
  title: string;
  content: string;
  createdAt: string | null;
  images: unknown[];
  attachments: EducationAttachment[];
}

export interface EducationListParams {
  category: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchEducationPosts(params: EducationListParams): Promise<EducationListData> {
  const res = await client.get<ApiResponse<EducationListData>>('/api/v1/mobile/education/posts', {
    params: { page: 1, size: 20, ...params },
  });
  return unwrap(res, '교육 자료 목록 조회에 실패했습니다');
}

export async function fetchEducationDetail(postId: string): Promise<EducationDetail> {
  const res = await client.get<ApiResponse<EducationDetail>>(
    `/api/v1/mobile/education/posts/${postId}`
  );
  return unwrap(res, '교육 자료 상세 조회에 실패했습니다');
}
