import client from './client';
import type { ApiResponse } from './types';
import type { Branch } from './team-schedule';

/**
 * 매출/실적 계열 화면(월 매출 물류배부·전산실적·POS·월별 투입적합성·배치 적합성) 공용 지점 셀렉터 옵션 조회.
 *
 * `monthly_sales_history` READ 로 가드된 전용 endpoint 를 호출한다. 여사원 일정의
 * `/team-schedule/branches` (`team_member_schedule` 가드) 를 빌려쓰면 `team_member_schedule` READ
 * 없는 사용자가 지점 셀렉터에서 403 이 나므로 분리한다. 반환 지점 범위는 backend 에서 권한 기준으로
 * 제한된다.
 */
export async function fetchSalesBranches(): Promise<Branch[]> {
  const res = await client.get<ApiResponse<Branch[]>>('/api/v1/admin/sales/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}
