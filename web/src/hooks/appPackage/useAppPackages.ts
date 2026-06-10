import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  deleteAppPackage,
  fetchAppPackages,
  fetchIosInstallUrl,
  setAppPackageLatest,
  toggleAppPackageForceUpdate,
  updateAppPackageReleaseNote,
  uploadAppPackage,
  type AppPlatform,
  type UploadAppPackageParams,
} from '@/api/appPackage';

/**
 * 앱 패키지 버전 관리 query / mutation 훅. (`/admin/app-packages`)
 *
 * 플랫폼(ANDROID/IOS) 탭별 목록을 조회하고, 업로드/최신지정/강제토글/릴리스노트수정/삭제 mutation 은
 * 성공 시 해당 플랫폼 목록 캐시를 무효화한다.
 */

const appPackagesKey = (platform: AppPlatform) => ['admin', 'app-packages', platform] as const;

export function useAppPackages(platform: AppPlatform, page = 0, size = 20) {
  return useQuery({
    queryKey: [...appPackagesKey(platform), page, size],
    queryFn: () => fetchAppPackages(platform, page, size),
  });
}

/**
 * iOS 고정 OTA 설치 링크 조회. 버전 무관 고정값이라 길게 캐시한다.
 * @param enabled iOS 탭에서만 조회하도록 제어
 */
export function useIosInstallUrl(enabled: boolean) {
  return useQuery({
    queryKey: ['admin', 'app-packages', 'ios-install-url'] as const,
    queryFn: fetchIosInstallUrl,
    enabled,
    staleTime: 60 * 60 * 1000, // 1시간 — API 도메인 기반 고정값이라 자주 바뀌지 않음
  });
}

export function useUploadAppPackage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (params: UploadAppPackageParams) => uploadAppPackage(params),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: appPackagesKey(variables.platform) });
    },
  });
}

export function useSetAppPackageLatest(platform: AppPlatform) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => setAppPackageLatest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: appPackagesKey(platform) });
    },
  });
}

export function useToggleAppPackageForceUpdate(platform: AppPlatform) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, forceUpdate }: { id: number; forceUpdate: boolean }) =>
      toggleAppPackageForceUpdate(id, forceUpdate),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: appPackagesKey(platform) });
    },
  });
}

export function useUpdateAppPackageReleaseNote(platform: AppPlatform) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, releaseNote }: { id: number; releaseNote: string | null }) =>
      updateAppPackageReleaseNote(id, releaseNote),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: appPackagesKey(platform) });
    },
  });
}

export function useDeleteAppPackage(platform: AppPlatform) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAppPackage(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: appPackagesKey(platform) });
    },
  });
}
