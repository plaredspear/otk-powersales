import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * Naver Geocode 변환 테스트 API (Spec #638 P2-W).
 *
 * 백엔드 `POST /api/v1/admin/naver-geocode/test` 호출 — Naver Cloud Map Geocode API 단건 변환 결과 반환.
 * 권한 `NAVER_GEOCODE_TEST` (SYSTEM_ADMIN role 자동 seed) 필요.
 */

export interface NaverGeocodeTestResult {
  roadAddress: string | null;
  jibunAddress: string | null;
  longitude: string | null;
  latitude: string | null;
}

export interface NaverGeocodeTestResponse {
  input: string;
  matchedCount: number;
  results: NaverGeocodeTestResult[];
}

export interface NaverGeocodeTestRequest {
  address: string;
}

export async function postNaverGeocodeTest(
  payload: NaverGeocodeTestRequest,
): Promise<NaverGeocodeTestResponse> {
  const res = await client.post<ApiResponse<NaverGeocodeTestResponse>>(
    '/api/v1/admin/naver-geocode/test',
    payload,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || 'Naver Geocode 변환에 실패했습니다');
  }
  return res.data.data;
}
