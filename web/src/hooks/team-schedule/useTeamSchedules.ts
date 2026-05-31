import { useQuery } from '@tanstack/react-query';
import { fetchTeamSchedules } from '@/api/team-schedule';

export function useTeamSchedules(params: {
  from: string;
  to: string;
  employeeIds: number[];
  accountIds: number[];
  promotionTeams: string[];
  branchCode?: string;
}) {
  return useQuery({
    queryKey: [
      'admin',
      'team-schedule',
      'list',
      params.from,
      params.to,
      params.employeeIds,
      params.accountIds,
      params.promotionTeams,
      params.branchCode ?? '',
    ],
    queryFn: () => fetchTeamSchedules(params),
    // enabled 가드 제거 — 거래처/여사원 필터가 없어도 (from/to 만으로) 거래처 전체 요약을
    // 항상 받는다. queryKey 에 from/to 가 있어 월 변경 시 자동 refetch.
  });
}
