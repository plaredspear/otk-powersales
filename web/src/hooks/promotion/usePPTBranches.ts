import { useQuery } from '@tanstack/react-query';
import { getPPTBranches } from '@/api/pptMaster';
import { useAuthStore } from '@/stores/authStore';

/**
 * 전문행사조 화면 지점 셀렉터 옵션 (마스터/이력/확정인원 공용).
 *
 * 여사원 현황(`/female-employees/meta` 의 지점 옵션) 과 **동일 목록**이다 — backend 가 두 화면 모두
 * `DashboardBranchResolver` 를 단일 출처로 쓰므로, 전사 권한자는 고정 화이트리스트 34개
 * (Retail 32지점 + 영업지원2팀 + CVS전략팀) 를 본다. 전용 endpoint
 * (`/api/v1/admin/ppt-masters/branches`) 를 호출하는 것은 화면 게이팅 권한
 * (`professional_promotion_team_master`) 과 API 가드를 일치시키기 위함이다.
 *
 * 옵션은 지점명(label) 가나다순으로 정렬해 노출한다 — 여사원 현황 셀렉터와 동일 정렬 (한국어 로케일).
 * 서버 응답은 조직 코드 순이므로 정렬을 화면단에서 맞춘다.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function usePPTBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'ppt-masters', 'branches', userId],
    queryFn: getPPTBranches,
    select: (branches) =>
      [...branches].sort((a, b) => a.branchName.localeCompare(b.branchName, 'ko')),
  });
}
