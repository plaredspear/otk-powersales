import { useQuery } from '@tanstack/react-query';
import { fetchPPTMasterFormMeta } from '@/api/pptMaster';

/**
 * 전문행사조 마스터 폼(등록/수정/복제 모달) 렌더링용 메타.
 *
 * 행사마스터 usePromotionFormMeta 와 동일 패턴 — 폼 Select 옵션(전문행사조 유형)을
 * 서버 enum 을 단일 출처로 받는다. 권한 주체 무관 정적 데이터라 staleTime 을 길게 둔다.
 */
export function usePPTMasterFormMeta() {
  return useQuery({
    queryKey: ['admin', 'ppt-masters', 'form-meta'],
    queryFn: fetchPPTMasterFormMeta,
    staleTime: 10 * 60 * 1000,
  });
}
