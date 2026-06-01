import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `GpsConsentTermsResponse` */
export interface GpsConsentTerms {
  agreementNumber: string | null;
  contents: string | null;
}

/** backend `GpsConsentStatusResponse` */
export interface GpsConsentStatus {
  requiresGpsConsent: boolean;
}

/** backend `GpsConsentRecordResponse` — 동의 기록 시 새 accessToken 발급 */
export interface GpsConsentRecord {
  accessToken: string;
  expiresIn: number;
}

export async function fetchGpsTerms(): Promise<GpsConsentTerms> {
  const res = await client.get<ApiResponse<GpsConsentTerms>>('/api/v1/mobile/auth/gps-consent/terms');
  return unwrap(res, 'GPS 약관 조회에 실패했습니다');
}

export async function fetchGpsStatus(): Promise<GpsConsentStatus> {
  const res = await client.get<ApiResponse<GpsConsentStatus>>(
    '/api/v1/mobile/auth/gps-consent/status'
  );
  return unwrap(res, 'GPS 동의 상태 조회에 실패했습니다');
}

export async function recordGpsConsent(agreementNumber?: string): Promise<GpsConsentRecord> {
  const res = await client.post<ApiResponse<GpsConsentRecord>>('/api/v1/mobile/auth/gps-consent', {
    agreementNumber,
  });
  return unwrap(res, 'GPS 동의 기록에 실패했습니다');
}
