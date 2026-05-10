import { useMutation } from '@tanstack/react-query';
import { postNaverGeocodeTest } from '@/api/admin/naverGeocode';
import type {
  NaverGeocodeTestRequest,
  NaverGeocodeTestResponse,
} from '@/api/admin/naverGeocode';

/**
 * Naver Geocode 변환 테스트 mutation 훅 (Spec #638 P2-W).
 *
 * 단건 변환은 read-only 외부 호출이므로 invalidate 대상 query 없음.
 * 페이지에서 onSuccess / onError 핸들링 (notification 노출 등).
 */
export function useNaverGeocodeTest() {
  return useMutation<NaverGeocodeTestResponse, Error, NaverGeocodeTestRequest>({
    mutationFn: postNaverGeocodeTest,
  });
}
