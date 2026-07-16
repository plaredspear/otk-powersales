import client from '@/api/client';
import type { ApiResponse } from '../types';

/**
 * 기능 활성화 관리 API (개발자 도구 > 대시보드 > 기능 활성화).
 *
 * 백엔드 `GET/POST /api/v1/admin/tools/feature-toggles` 호출 — 제품 클레임/물류 클레임/주문 등록
 * API 를 런타임에 on/off 한다. 상태는 Redis 에 지속 저장되어 앱 재시작 후에도 유지된다.
 * 권한: 시스템 관리자 전용 (백엔드에서 profileName='시스템 관리자' 로 가드).
 */

export interface FeatureToggleItem {
  code: string;
  label: string;
  enabled: boolean;
  reason: string | null;
}

export interface FeatureToggleListResponse {
  features: FeatureToggleItem[];
}

export async function fetchFeatureToggles(): Promise<FeatureToggleListResponse> {
  const res = await client.get<ApiResponse<FeatureToggleListResponse>>(
    '/api/v1/admin/tools/feature-toggles',
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '기능 활성화 상태 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 기능 활성화 상태 변경. `reason` 은 비활성화(enabled=false) 시 모바일에 노출되는 사유 문구다.
 */
export async function updateFeatureToggle(params: {
  code: string;
  enabled: boolean;
  reason: string | null;
}): Promise<FeatureToggleItem> {
  const res = await client.post<ApiResponse<FeatureToggleItem>>(
    '/api/v1/admin/tools/feature-toggles',
    params,
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '기능 활성화 변경에 실패했습니다');
  }
  return res.data.data;
}
