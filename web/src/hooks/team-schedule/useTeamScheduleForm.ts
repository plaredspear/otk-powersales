import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleForm } from '@/api/team-schedule';

/**
 * 여사원 일정관리 화면 초기 로드 통합 조회.
 *
 * `branchCode` 지정 시 해당 지점 거래처를 form 응답에 포함 (다중지점 사용자가 지점 드롭다운에서 선택했을 때 재호출).
 * 미지정 시 backend 가 단일지점 사용자만 본인 지점 거래처를 자동 채움.
 *
 * queryKey 에 branchCode 가 포함되므로 지점 변경 시 자동 재요청 + 캐시 키 분리.
 */
export function useTeamScheduleForm(branchCode?: string) {
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'form', branchCode ?? ''],
    queryFn: () => fetchTeamScheduleForm(branchCode),
  });
}
