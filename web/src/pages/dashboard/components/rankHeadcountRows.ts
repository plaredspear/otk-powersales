import type { RankGroupCount } from '@/api/adminDashboard';

/**
 * 직급별 인원현황의 행 구성 — 표와 그래프가 서로 다른 형태를 쓴다.
 *
 * RankHeadcountCard 컴포넌트에서 분리한 순수 함수 모듈이다 (react-refresh 는 컴포넌트 파일이
 * 컴포넌트만 export 하기를 요구한다).
 */

/** 판매조장 그룹명 — backend `FemaleStaffHeadcountFilter.LEADER_JIKCHAK` 과 동일 문자열. */
export const LEADER_GROUP = '판매조장';

/** 평탄화된 행 1건. */
export type FlatRank = {
  group: string;
  label: string;
  count: number;
};

/** 표 본문용 — 그룹의 모든 직위 셀을 순서대로 편다 (판매조장도 직위별로 분리). */
export function flatten(groups: RankGroupCount[]): FlatRank[] {
  return groups.flatMap((g) => g.ranks.map((r) => ({ group: g.group, label: r.label, count: r.count })));
}

/**
 * 그래프용 행 — **판매조장만 직위와 무관하게 1줄로 합산**하고, 나머지 그룹은 직위별로 편다.
 *
 * 표와 구성이 다른 이유: 판매조장의 직위는 지점마다 달라(주임/OSPM/…) 그래프에서 직위별로 쪼개면
 * 지점 간 막대 구성이 흔들린다. 판매조장은 "몇 명인지"만 보면 되므로 합계 1줄로 고정한다.
 * 판촉직·OSC직 행은 서버가 이미 판매조장을 제외한 인원이라 중복되지 않는다.
 *
 * 두 함수의 합계는 항상 같다 — 그래프의 총합계 막대가 표의 총합계 열과 어긋나면 안 된다.
 */
export function chartRows(groups: RankGroupCount[]): FlatRank[] {
  return groups.flatMap((g) => {
    if (g.group === LEADER_GROUP) {
      const total = g.ranks.reduce((sum, r) => sum + r.count, 0);
      return total > 0 ? [{ group: g.group, label: g.group, count: total }] : [];
    }
    return g.ranks.map((r) => ({ group: g.group, label: r.label, count: r.count }));
  });
}
