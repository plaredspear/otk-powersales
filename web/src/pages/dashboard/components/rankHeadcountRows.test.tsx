import { describe, it, expect } from 'vitest';
import { flatten, chartRows } from './rankHeadcountRows';
import type { RankGroupCount } from '@/api/adminDashboard';

/**
 * 직급별 인원현황 — 표와 그래프의 행 구성이 다르다는 점을 고정한다.
 *
 * - 표: 판매조장도 직위별로 분리 (주임 / OSPM …)
 * - 그래프: 판매조장은 직위 무관 합계 1줄, 나머지는 직위별
 *
 * 두 구성의 합계는 항상 같아야 한다 — 그래프의 총합계 막대가 표의 총합계 열과 어긋나면 안 된다.
 */
describe('RankHeadcountCard 행 구성', () => {
  /** 강북4지점 시안 형태 — 판매조장의 직위가 여러 종류인 경우. */
  const groups: RankGroupCount[] = [
    {
      group: '판매조장',
      ranks: [
        { label: '주임', count: 2 },
        { label: 'OSPM', count: 1 },
      ],
    },
    // 직위는 직무에 종속 — 판촉직은 OSPM/OSPE/OSPJ, OSC직은 OSC 만 열로 온다.
    {
      group: '판촉직',
      ranks: [
        { label: 'OSPM', count: 11 },
        { label: 'OSPE', count: 6 },
        { label: 'OSPJ', count: 18 },
      ],
    },
    {
      group: 'OSC직',
      ranks: [{ label: 'OSC', count: 9 }],
    },
  ];

  it('표는 판매조장을 직위별로 분리해 편다', () => {
    const rows = flatten(groups);

    expect(rows.slice(0, 2)).toEqual([
      { group: '판매조장', label: '주임', count: 2 },
      { group: '판매조장', label: 'OSPM', count: 1 },
    ]);
    // 판매조장 2 + 판촉직 3 + OSC직 1
    expect(rows).toHaveLength(6);
  });

  it('그래프는 판매조장을 직위 무관 1줄로 합산한다', () => {
    const rows = chartRows(groups);

    expect(rows[0]).toEqual({ group: '판매조장', label: '판매조장', count: 3 });
    // 나머지 그룹은 직위별로 그대로 — 판매조장 2줄이 1줄로 접혀 총 5줄
    expect(rows).toHaveLength(5);
  });

  it('표와 그래프의 합계가 일치한다', () => {
    const sum = (rows: { count: number }[]) => rows.reduce((acc, r) => acc + r.count, 0);

    expect(sum(chartRows(groups))).toBe(sum(flatten(groups)));
  });

  it('판촉직 행에는 판매조장 인원이 중복되지 않는다', () => {
    // 서버가 판매조장을 먼저 떼어내고 내려주므로, 판촉직 OSPM(11)에 조장 OSPM(1)이 섞이지 않는다.
    const promotionOspm = chartRows(groups).find((r) => r.group === '판촉직' && r.label === 'OSPM');

    expect(promotionOspm?.count).toBe(11);
  });

  it('판매조장 인원이 0이면 그래프에서 행 자체를 만들지 않는다', () => {
    const withEmptyLeader: RankGroupCount[] = [
      { group: '판매조장', ranks: [{ label: '주임', count: 0 }] },
      { group: '판촉직', ranks: [{ label: 'OSPM', count: 5 }] },
    ];

    expect(chartRows(withEmptyLeader).map((r) => r.label)).toEqual(['OSPM']);
  });
});
