import client from './client';
import type { ApiResponse } from './types';

// ─────────────────────────────────────────────────────
// 타입 정의 — 현장점검(등록) 테마 (SF Theme__c)
// ─────────────────────────────────────────────────────

export interface ThemeListParams {
  keyword?: string;
  page?: number;
  size?: number;
}

export interface ThemeListItem {
  id: number;
  name: string | null;
  title: string | null;
  department: string | null;
  branchCode: string | null;
  startDate: string | null;
  endDate: string | null;
  ownerName: string | null;
  siteActivityCount: number;
  createdAt: string | null;
}

export interface ThemeListData {
  content: ThemeListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ThemeSiteActivityItem {
  id: number;
  name: string | null;
  branchName: string | null;
  accountName: string | null;
  productName: string | null;
  orgName: string | null;
  employeeName: string | null;
  category: string | null;
  activityDate: string | null;
}

export interface ThemeDetail {
  id: number;
  name: string | null;
  title: string | null;
  department: string | null;
  branchCode: string | null;
  startDate: string | null;
  endDate: string | null;
  ownerName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  siteActivities: ThemeSiteActivityItem[];
}

export interface ThemeMutationRequest {
  title: string;
  startDate: string | null;
  endDate: string | null;
}

export interface ThemeMutationResult {
  id: number;
  name: string | null;
}

// ─────────────────────────────────────────────────────
// API 호출 함수
// ─────────────────────────────────────────────────────

const BASE = '/api/v1/admin/inspection-themes';

export async function fetchThemes(params: ThemeListParams): Promise<ThemeListData> {
  const res = await client.get<ApiResponse<ThemeListData>>(BASE, { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchThemeDetail(id: number): Promise<ThemeDetail> {
  const res = await client.get<ApiResponse<ThemeDetail>>(`${BASE}/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createTheme(request: ThemeMutationRequest): Promise<ThemeMutationResult> {
  const res = await client.post<ApiResponse<ThemeMutationResult>>(BASE, request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateTheme(id: number, request: ThemeMutationRequest): Promise<ThemeMutationResult> {
  const res = await client.put<ApiResponse<ThemeMutationResult>>(`${BASE}/${id}`, request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteTheme(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`${BASE}/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '테마 삭제에 실패했습니다');
  }
}

/** 테마 단위 하위 현장점검 결과 엑셀 다운로드 — Blob 응답. */
export async function downloadThemeExcel(id: number, fallbackName: string): Promise<void> {
  const res = await client.get(`${BASE}/${id}/export`, { responseType: 'blob' });
  const disposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${fallbackName}.xlsx`;
  if (disposition) {
    const match = disposition.match(/filename\*=UTF-8''(.+)$/);
    if (match) {
      filename = decodeURIComponent(match[1]);
    }
  }
  const url = window.URL.createObjectURL(new Blob([res.data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
