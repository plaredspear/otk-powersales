import { useMemo } from 'react';
import { Typography } from 'antd';
import dayjs from 'dayjs';
import type { EmployeeWorkHistoryItem } from '@/api/employee';

const { Text } = Typography;

interface Props {
  items: EmployeeWorkHistoryItem[];
  /** 조회 기준 연도 */
  year: number;
  /** 조회 기준 월 (1~12) */
  month: number;
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

const CATEGORY1_COLOR: Record<string, string> = {
  진열: '#1677ff',
  행사: '#fa8c16',
};
const CATEGORY1_SYMBOL: Record<string, string> = {
  진열: '●',
  행사: '◆',
};

function resolveWorkplace(row: EmployeeWorkHistoryItem): string {
  return row.accountName ?? row.refAccountName ?? row.costCenterCode ?? '-';
}

interface Aggregates {
  /** 근무유형별 [유형, 고유 날짜 수(일), 건수(회)] — 하루에 복수 근무 row 가 있으면 일≠회. */
  byWorkType: Array<[string, number, number]>;
  byWorkplace: Array<[string, number]>;
  byCategory1: Array<[string, number]>;
  byCategory3: Array<[string, number]>;
}

function aggregate(items: EmployeeWorkHistoryItem[]): Aggregates {
  /** 근무유형 → { 건수, 고유 날짜 set } */
  const wt = new Map<string, { count: number; days: Set<string> }>();
  const wp = new Map<string, number>();
  const c1 = new Map<string, number>();
  const c3 = new Map<string, number>();
  const bump = (m: Map<string, number>, k: string | null) => {
    if (!k) return;
    m.set(k, (m.get(k) ?? 0) + 1);
  };
  for (const it of items) {
    if (it.workingType) {
      const e = wt.get(it.workingType) ?? { count: 0, days: new Set<string>() };
      e.count += 1;
      if (it.workingDate) e.days.add(it.workingDate);
      wt.set(it.workingType, e);
    }
    if (it.workingType === '근무') {
      bump(wp, resolveWorkplace(it));
      bump(c1, it.workingCategory1);
      bump(c3, it.workingCategory3);
    }
  }
  const sortDesc = (m: Map<string, number>) =>
    Array.from(m.entries()).sort((a, b) => b[1] - a[1]);
  const byWorkType = Array.from(wt.entries())
    .map(([k, v]) => [k, v.days.size, v.count] as [string, number, number])
    .sort((a, b) => b[2] - a[2]);
  return {
    byWorkType,
    byWorkplace: sortDesc(wp),
    byCategory1: sortDesc(c1),
    byCategory3: sortDesc(c3),
  };
}

/** 좌측 C — 근무지/업무성격/근무방식 요약. */
function SummaryPanel({ agg }: { agg: Aggregates }) {
  const maxWp = Math.max(1, ...agg.byWorkplace.map(([, n]) => n));
  return (
    <div style={{ width: 260, flexShrink: 0 }}>
      <div style={{ marginBottom: 16 }}>
        <Text strong>근무 구분</Text>
        <div style={{ marginTop: 6 }}>
          {agg.byWorkType.length === 0 ? (
            <Text type="secondary">-</Text>
          ) : (
            agg.byWorkType.map(([k, days, count]) => (
              <span key={k} style={{ marginRight: 12 }}>
                {k} <Text strong>{days}</Text>일{' '}
                <Text type="secondary">({count}회)</Text>
              </span>
            ))
          )}
        </div>
      </div>

      <div style={{ marginBottom: 16 }}>
        <Text strong>근무지</Text>
        <div style={{ marginTop: 6 }}>
          {agg.byWorkplace.length === 0 ? (
            <Text type="secondary">-</Text>
          ) : (
            agg.byWorkplace.map(([k, n]) => (
              <div key={k} style={{ marginBottom: 4 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
                  <span
                    style={{
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      maxWidth: 180,
                    }}
                    title={k}
                  >
                    {k}
                  </span>
                  <span style={{ flexShrink: 0 }}>{n}일</span>
                </div>
                <div style={{ background: '#f0f0f0', borderRadius: 2, height: 6 }}>
                  <div
                    style={{
                      width: `${(n / maxWp) * 100}%`,
                      background: '#1677ff',
                      height: 6,
                      borderRadius: 2,
                    }}
                  />
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div style={{ marginBottom: 16 }}>
        <Text strong>근무 유형</Text>
        <div style={{ marginTop: 6 }}>
          {agg.byCategory1.length === 0 ? (
            <Text type="secondary">-</Text>
          ) : (
            agg.byCategory1.map(([k, n]) => (
              <span key={k} style={{ marginRight: 12 }}>
                <span style={{ color: CATEGORY1_COLOR[k] ?? '#888' }}>
                  {CATEGORY1_SYMBOL[k] ?? '•'}
                </span>{' '}
                {k} <Text strong>{n}</Text>
              </span>
            ))
          )}
        </div>
      </div>

      <div>
        <Text strong>근무 방식</Text>
        <div style={{ marginTop: 6 }}>
          {agg.byCategory3.length === 0 ? (
            <Text type="secondary">-</Text>
          ) : (
            agg.byCategory3.map(([k, n]) => (
              <span key={k} style={{ marginRight: 12 }}>
                {k} <Text strong>{n}</Text>
              </span>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

/** 우측 B — 월 캘린더. 바탕(근무/휴무) + 색(진열/행사) + 텍스트(근무방식) 3중 인코딩. */
function CalendarPanel({
  items,
  year,
  month,
}: {
  items: EmployeeWorkHistoryItem[];
  year: number;
  month: number;
}) {
  const byDay = useMemo(() => {
    const m = new Map<number, EmployeeWorkHistoryItem[]>();
    for (const it of items) {
      if (!it.workingDate) continue;
      const d = dayjs(it.workingDate).date();
      if (!m.has(d)) m.set(d, []);
      m.get(d)!.push(it);
    }
    return m;
  }, [items]);

  const first = dayjs(`${year}-${String(month).padStart(2, '0')}-01`);
  const daysInMonth = first.daysInMonth();
  const startWeekday = first.day();
  const cells: Array<number | null> = [
    ...Array<null>(startWeekday).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  return (
    <div style={{ flex: 1, minWidth: 0 }}>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 1fr)',
          gap: 4,
        }}
      >
        {WEEKDAYS.map((w, idx) => (
          <div
            key={w}
            style={{
              textAlign: 'center',
              fontWeight: 600,
              fontSize: 12,
              padding: '2px 0',
              color: idx === 0 ? '#cf1322' : idx === 6 ? '#1677ff' : undefined,
            }}
          >
            {w}
          </div>
        ))}
        {cells.map((day, idx) => {
          if (day === null) return <div key={`blank-${idx}`} />;
          const dayItems = byDay.get(day) ?? [];
          const works = dayItems.filter((i) => i.workingType === '근무');
          const leave = dayItems.find(
            (i) => i.workingType === '연차' || i.workingType === '대휴',
          );
          const isLeave = works.length === 0 && !!leave;
          const head = works[0];
          return (
            <div
              key={day}
              style={{
                minHeight: 64,
                border: '1px solid #f0f0f0',
                borderRadius: 4,
                padding: 4,
                fontSize: 11,
                background: isLeave
                  ? 'repeating-linear-gradient(45deg,#fafafa,#fafafa 4px,#f0f0f0 4px,#f0f0f0 8px)'
                  : '#fff',
              }}
            >
              <div style={{ fontWeight: 600, color: '#888' }}>{day}</div>
              {isLeave && <div style={{ color: '#888' }}>{leave?.workingType}</div>}
              {head && (
                <>
                  <div
                    style={{
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                    title={resolveWorkplace(head)}
                  >
                    <span
                      style={{
                        color: head.workingCategory1
                          ? CATEGORY1_COLOR[head.workingCategory1] ?? '#888'
                          : '#888',
                      }}
                    >
                      {head.workingCategory1
                        ? CATEGORY1_SYMBOL[head.workingCategory1] ?? '•'
                        : '•'}
                    </span>{' '}
                    {resolveWorkplace(head)}
                  </div>
                  {head.workingCategory3 && (
                    <div style={{ color: '#aaa' }}>{head.workingCategory3}</div>
                  )}
                  {works.length > 1 && (
                    <div style={{ color: '#1677ff' }}>외 {works.length - 1}건</div>
                  )}
                </>
              )}
            </div>
          );
        })}
      </div>
      <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
        <span style={{ color: CATEGORY1_COLOR['진열'] }}>● 진열</span>{'   '}
        <span style={{ color: CATEGORY1_COLOR['행사'] }}>◆ 행사</span>{'   '}
        <span>▨ 휴무(연차·대휴)</span>
      </div>
    </div>
  );
}

export default function MonthlyWorkInsight({ items, year, month }: Props) {
  const agg = useMemo(() => aggregate(items), [items]);

  // 근무내역이 없어도(미선택 포함) 캘린더 격자는 항상 그린다 — 빈 달력으로 노출.
  return (
    <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start', flexWrap: 'wrap' }}>
      <SummaryPanel agg={agg} />
      <CalendarPanel items={items} year={year} month={month} />
    </div>
  );
}
