import client from './client';
import type { ApiResponse } from './types';

/**
 * 모바일 앱 패키지(APK/IPA) 버전 관리 API. (Backend `AdminAppPackageController`, `/api/v1/admin/app-package`)
 *
 * 사내 직접 배포용 패키지를 업로드/버전관리한다. 권한: MODIFY_ALL_DATA.
 */

export type AppPlatform = 'ANDROID' | 'IOS';

export interface AppPackageListItem {
  id: number;
  platform: AppPlatform;
  versionName: string;
  versionCode: number;
  forceUpdate: boolean;
  isLatest: boolean;
  releaseNote: string | null;
  fileName: string;
  fileSize: number;
  bundleIdentifier: string | null;
  uploadedAt: string;
}

export interface AppPackageDetail extends AppPackageListItem {
  downloadUrl: string;
  /** presigned 다운로드 URL 유효 시간(초). 복사 안내 문구의 만료 시각 계산에 사용. */
  downloadUrlExpiresInSeconds: number;
}

export interface AppPackagePage {
  content: AppPackageListItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface UploadAppPackageParams {
  platform: AppPlatform;
  versionName: string;
  versionCode: number;
  forceUpdate: boolean;
  releaseNote?: string;
  bundleIdentifier?: string;
  file: File;
}

export async function uploadAppPackage(params: UploadAppPackageParams): Promise<AppPackageDetail> {
  const formData = new FormData();
  formData.append('platform', params.platform);
  formData.append('versionName', params.versionName);
  formData.append('versionCode', String(params.versionCode));
  formData.append('forceUpdate', String(params.forceUpdate));
  if (params.releaseNote) formData.append('releaseNote', params.releaseNote);
  if (params.bundleIdentifier) formData.append('bundleIdentifier', params.bundleIdentifier);
  formData.append('file', params.file);

  const res = await client.post<ApiResponse<AppPackageDetail>>('/api/v1/admin/app-package', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || res.data.error?.message || '패키지 업로드에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchAppPackages(
  platform: AppPlatform,
  page = 0,
  size = 20,
): Promise<AppPackagePage> {
  const res = await client.get<ApiResponse<AppPackagePage>>('/api/v1/admin/app-package', {
    params: { platform, page, size },
  });
  if (!res.data.data) {
    throw new Error(res.data.message || '목록 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function fetchAppPackageDetail(id: number): Promise<AppPackageDetail> {
  const res = await client.get<ApiResponse<AppPackageDetail>>(`/api/v1/admin/app-package/${id}`);
  if (!res.data.data) throw new Error(res.data.message || '상세 조회에 실패했습니다');
  return res.data.data;
}

export async function setAppPackageLatest(id: number): Promise<AppPackageDetail> {
  const res = await client.patch<ApiResponse<AppPackageDetail>>(`/api/v1/admin/app-package/${id}/latest`);
  if (!res.data.data) throw new Error(res.data.message || '최신 지정에 실패했습니다');
  return res.data.data;
}

export async function toggleAppPackageForceUpdate(
  id: number,
  forceUpdate: boolean,
): Promise<AppPackageDetail> {
  const res = await client.patch<ApiResponse<AppPackageDetail>>(
    `/api/v1/admin/app-package/${id}/force-update`,
    { forceUpdate },
  );
  if (!res.data.data) throw new Error(res.data.message || '강제 업데이트 변경에 실패했습니다');
  return res.data.data;
}

export async function updateAppPackageReleaseNote(
  id: number,
  releaseNote: string | null,
): Promise<AppPackageDetail> {
  const res = await client.patch<ApiResponse<AppPackageDetail>>(`/api/v1/admin/app-package/${id}`, {
    releaseNote,
  });
  if (!res.data.data) throw new Error(res.data.message || '릴리스 노트 수정에 실패했습니다');
  return res.data.data;
}

export async function deleteAppPackage(id: number): Promise<void> {
  await client.delete<ApiResponse<void>>(`/api/v1/admin/app-package/${id}`);
}
