import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 운영도구 — Redis 캐시 evict admin API.
 *
 * 권한: SYSTEM_ADMIN (`MODIFY_ALL_DATA`). 사용 시나리오는 backend `AdminCacheService` 클래스 주석 참조.
 */

export interface CacheInfo {
  name: string;
  /** Redis SCAN 으로 추정한 key 개수. -1 = 미지원 (NoOpCache 등). */
  estimatedKeyCount: number;
}

export interface EvictResult {
  cacheName: string;
  keysBefore: number;
  keysAfter: number;
}

export async function fetchCacheList(): Promise<CacheInfo[]> {
  const res = await client.get<ApiResponse<CacheInfo[]>>('/api/v1/admin/cache');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '캐시 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function evictCache(cacheName: string): Promise<EvictResult> {
  const res = await client.post<ApiResponse<EvictResult>>(
    `/api/v1/admin/cache/${encodeURIComponent(cacheName)}/evict`,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '캐시 무효화에 실패했습니다');
  }
  return res.data.data;
}

/** 전체 캐시 일괄 evict — Spring CacheManager 캐시 + in-memory 권한 캐시 모두. */
export async function evictAllCaches(): Promise<EvictResult[]> {
  const res = await client.post<ApiResponse<EvictResult[]>>('/api/v1/admin/cache/evict-all');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '전체 캐시 무효화에 실패했습니다');
  }
  return res.data.data;
}
