import client from './client';
import type { ApiResponse } from './types';

export type SavedSearchScope = 'PRIVATE' | 'SHARED';

export interface SavedSearch {
  id: number;
  resourceKey: string;
  name: string;
  scope: SavedSearchScope;
  ownerId: number | null;
  ownerName: string | null;
  filters: Record<string, string>;
  sortOrder: number;
  /** 호출자가 본 검색을 수정/삭제할 수 있는지 (PRIVATE=소유자, SHARED=saved_search EDIT 권한). */
  editable: boolean;
}

export interface SavedSearchCreateRequest {
  resourceKey: string;
  name: string;
  scope: SavedSearchScope;
  filters: Record<string, string>;
  sortOrder?: number;
}

export interface SavedSearchUpdateRequest {
  name: string;
  filters: Record<string, string>;
  sortOrder?: number;
}

const BASE = '/api/v1/admin/saved-searches';

export async function fetchSavedSearches(resourceKey: string): Promise<SavedSearch[]> {
  const res = await client.get<ApiResponse<SavedSearch[]>>(BASE, {
    params: { resourceKey },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '저장된 검색 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createSavedSearch(request: SavedSearchCreateRequest): Promise<SavedSearch> {
  const res = await client.post<ApiResponse<SavedSearch>>(BASE, request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '저장된 검색 생성에 실패했습니다');
  }
  return res.data.data;
}

export async function updateSavedSearch(
  id: number,
  request: SavedSearchUpdateRequest,
): Promise<SavedSearch> {
  const res = await client.put<ApiResponse<SavedSearch>>(`${BASE}/${id}`, request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '저장된 검색 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteSavedSearch(id: number): Promise<void> {
  await client.delete(`${BASE}/${id}`);
}
