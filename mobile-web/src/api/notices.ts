import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `NoticePostSummaryResponse` */
export interface NoticeSummary {
  id: number;
  category: string;
  categoryName: string;
  scope: string | null;
  title: string;
  branch: string | null;
  department: string | null;
  authorName: string | null;
  createdAt: string | null;
}

/** backend `NoticePostListResponse` */
export interface NoticeListData {
  content: NoticeSummary[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export interface NoticeImage {
  id: number;
  url: string;
  sortOrder: number;
}

/** backend `NoticePostDetailResponse` */
export interface NoticeDetail {
  id: number;
  scope: string | null;
  category: string;
  categoryName: string;
  title: string;
  content: string;
  branch: string | null;
  branchCode: string | null;
  createdAt: string | null;
  images: NoticeImage[];
}

export interface NoticeListParams {
  category?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchNotices(params: NoticeListParams): Promise<NoticeListData> {
  const res = await client.get<ApiResponse<NoticeListData>>('/api/v1/mobile/notices', {
    params: { page: 1, size: 20, ...params },
  });
  return unwrap(res, '공지사항 목록 조회에 실패했습니다');
}

export async function fetchNoticeDetail(noticeId: number): Promise<NoticeDetail> {
  const res = await client.get<ApiResponse<NoticeDetail>>(`/api/v1/mobile/notices/${noticeId}`);
  return unwrap(res, '공지사항 상세 조회에 실패했습니다');
}
