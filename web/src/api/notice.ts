import { AxiosError } from 'axios';
import client from './client';
import type { ApiResponse, ApiErrorBody } from './types';

/** axios 에러에서 서버가 내려준 에러 메시지를 뽑아 fallback 과 함께 반환한다. */
function serverErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof AxiosError) {
    const body = error.response?.data as ApiErrorBody | undefined;
    if (body?.error?.message) return body.error.message;
  }
  return fallback;
}


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
  status: string; // DRAFT | PUBLISHED
  statusName: string; // 임시저장 | 발행
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
  status: string; // DRAFT | PUBLISHED
  statusName: string; // 임시저장 | 발행
  title: string;
  content: string;
  branch: string | null;
  branchCode: string | null;
  createdAt: string;
  /** 낙관적 락 버전. 수정 저장 시 그대로 되돌려보내 동시 편집 충돌(409)을 감지한다. */
  version: number;
  images: NoticeImage[];
  /** push 누적 발송 횟수 (0=미발송). 중복 발송 경고 판단용. */
  pushSentCount: number;
  /** 마지막 push 발송 이력 (미발송이면 null). */
  lastPush: NoticePushInfo | null;
}

export interface NoticePushInfo {
  sentAt: string | null;
  targetCount: number;
  successCount: number;
  failureCount: number;
}

export interface NoticePushResult {
  targetCount: number;
  successCount: number;
  failureCount: number;
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
  /** 낙관적 락 버전 — 수정 시 상세조회로 받은 version 을 되돌려보낸다. 신규 등록 시에는 불필요. */
  version?: number;
  /** 이번 편집 세션에서 본문에 삽입 목적으로 업로드한 인라인 이미지 refid 목록.
   *  저장 시 본문에서 빠진 이미지를 서버가 정리하는 대상 판별에 쓰인다. */
  sessionUploadedRefids?: string[];
  /** true=발행(PUBLISHED), false=임시저장(DRAFT). 발행/임시저장 버튼 분리. */
  publish?: boolean;
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
  try {
    const res = await client.post<ApiResponse<unknown>>('/api/v1/admin/notices', data);
    if (!res.data.success) {
      throw new Error(res.data.message || '공지사항 등록에 실패했습니다');
    }
  } catch (error) {
    throw new Error(serverErrorMessage(error, '공지사항 등록에 실패했습니다'));
  }
}

export async function updateNotice(id: number, data: NoticeFormData): Promise<void> {
  try {
    const res = await client.put<ApiResponse<unknown>>(`/api/v1/admin/notices/${id}`, data);
    if (!res.data.success) {
      throw new Error(res.data.message || '공지사항 수정에 실패했습니다');
    }
  } catch (error) {
    // 동시 편집 충돌(409, NOTICE_VERSION_CONFLICT)의 서버 안내 메시지를 그대로 사용자에게 전달한다.
    throw new Error(serverErrorMessage(error, '공지사항 수정에 실패했습니다'));
  }
}

export async function deleteNotice(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`/api/v1/admin/notices/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '공지사항 삭제에 실패했습니다');
  }
}

export async function publishNotice(id: number): Promise<void> {
  const res = await client.patch<ApiResponse<unknown>>(`/api/v1/admin/notices/${id}/publish`);
  if (!res.data.success) {
    throw new Error(res.data.message || '공지사항 발행에 실패했습니다');
  }
}

export async function unpublishNotice(id: number): Promise<void> {
  const res = await client.patch<ApiResponse<unknown>>(`/api/v1/admin/notices/${id}/unpublish`);
  if (!res.data.success) {
    throw new Error(res.data.message || '공지사항 발행취소에 실패했습니다');
  }
}

/**
 * 공지 FCM push 즉시 발송. 발행된 공지에만 가능하며, 대상은 그 공지가 앱에 노출되는 사용자와 동일.
 * 응답으로 대상/성공/실패 건수를 반환한다.
 */
export async function sendNoticePush(id: number): Promise<NoticePushResult> {
  const res = await client.post<ApiResponse<NoticePushResult>>(`/api/v1/admin/notices/${id}/push`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '푸시 알림 발송에 실패했습니다');
  }
  return res.data.data;
}

export interface NoticeInlineImage {
  refid: string;
  placeholder: string;
  previewUrl: string;
}

/**
 * 공지 본문 인라인 이미지 업로드 (Quill 드래그앤드롭).
 * 응답의 previewUrl 로 에디터에 즉시 미리보기를 띄우되, 저장 본문에는 placeholder(`<img data-refid>`)가 들어가야 한다.
 */
export async function uploadNoticeInlineImage(file: File): Promise<NoticeInlineImage> {
  const formData = new FormData();
  formData.append('image', file);
  const res = await client.post<ApiResponse<NoticeInlineImage>>(
    '/api/v1/admin/notices/images/inline',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '이미지 업로드에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchNoticeFormMeta(): Promise<NoticeFormMeta> {
  const res = await client.get<ApiResponse<NoticeFormMeta>>('/api/v1/admin/notices/form-meta');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '폼 메타데이터 조회에 실패했습니다');
  }
  return res.data.data;
}
