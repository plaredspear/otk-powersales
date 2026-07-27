import { useMemo } from 'react';
import { Card, Empty } from 'antd';
import ReactECharts from 'echarts-for-react';
import type { RankGroupCount } from '@/api/adminDashboard';
import { chartRows, flatten, type FlatRank } from './rankHeadcountRows';

/**
 * 직급별 인원현황 — 2단 헤더 표 + 가로 막대 차트.
 *
 * ## 표 구조
 *
 * 1단은 그룹(판매조장 / 판촉직 / OSC직), 2단은 직급(직위). **2단 열 구성은 그룹마다 다르다** —
 * 판매조장은 해당 지점에 실제 존재하는 직위를 그대로 노출하고(지점에 따라 '주임'/'OSPM' 등으로
 * 달라짐), 판촉직·OSC직은 OSPM/OSPE/OSPJ/OSC 고정 + '기타' 합산이다. 서버가 이 구성을 확정해
 * 내려주므로([RankGroupCount]) 화면은 받은 순서대로 렌더링만 한다.
 *
 * ## 표 ≠ 그래프
 *
 * 그래프는 판매조장을 직위 무관 1줄로 접는다 (`chartRows`) — 표는 직위별로 편다 (`flatten`).
 * 두 구성의 합계는 항상 같다.
 *
 * ## 총합계
 *
 * 전체 셀의 단순 합. 판매조장 판정이 `jikchak` 축이라 대시보드 총원(role 축)과 소수 인원이
 * 어긋날 수 있다 — 상세는 backend `FemaleStaffHeadcountFilter.LEADER_JIKCHAK` 주석 참조.
 */

const TOTAL_LABEL = '총합계';
const BAR_COLOR = '#1677ff';
/** 막대 1개당 세로 높이(px) — 항목 수에 비례해 차트 높이를 잡는다. */
const BAR_ROW_HEIGHT = 28;
const CHART_MIN_HEIGHT = 160;

/**
 * 가로 막대 옵션 — 총합계 행만 다른 색으로 강조한다.
 * y축은 위에서 아래로 읽도록 데이터 순서를 뒤집는다(ECharts 는 y축 하단부터 그린다).
 */
function horizontalBarOption(rows: FlatRank[], total: number) {
  const items = [...rows.map((r) => ({ name: r.label, value: r.count })), { name: TOTAL_LABEL, value: total }];
  const reversed = [...items].reverse();
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v: number) => `${v}명` },
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', axisLabel: { show: false }, splitLine: { show: false } },
    yAxis: {
      type: 'category',
      data: reversed.map((i) => i.name),
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        label: { show: true, position: 'right', formatter: (p: { value: number }) => `${p.value}` },
        data: reversed.map((i) => ({
          value: i.value,
          // 총합계 막대는 한 톤 진하게 — 표의 총합계 열 강조와 맞춘다.
          itemStyle: { color: i.name === TOTAL_LABEL ? '#0958d9' : BAR_COLOR },
        })),
      },
    ],
  };
}

export default function RankHeadcountCard({
  groups,
  branchName,
  asOfDate,
  title,
}: {
  groups: RankGroupCount[];
  branchName: string | null;
  asOfDate: string;
  /** 카드 제목 노드 (info 아이콘 툴팁 포함). */
  title: React.ReactNode;
}) {
  // 표는 판매조장을 직위별로 펴고(rows), 그래프는 판매조장을 1줄로 접는다(barRows).
  // 두 구성의 합계는 같아야 하므로 total 은 표 기준 하나만 계산해 공유한다.
  const rows = useMemo(() => flatten(groups), [groups]);
  const barRows = useMemo(() => chartRows(groups), [groups]);
  const total = useMemo(() => rows.reduce((sum, r) => sum + r.count, 0), [rows]);

  if (rows.length === 0) {
    return (
      <Card title={title}>
        <Empty description="조회된 인원이 없습니다." />
      </Card>
    );
  }

  const chartHeight = Math.max(CHART_MIN_HEIGHT, (barRows.length + 1) * BAR_ROW_HEIGHT);

  return (
    <Card
      title={title}
      extra={<span style={{ color: '#8c8c8c' }}>{branchName ? `[ ${branchName} ]` : ''} (단위: 명)</span>}
    >
      {/* 좁은 화면에서 열이 많아지면 표만 가로 스크롤 — 페이지 자체는 가로 스크롤되지 않게 한다. */}
      <div style={{ overflowX: 'auto' }}>
        <table style={{ borderCollapse: 'collapse', width: '100%', textAlign: 'center', fontSize: 13 }}>
          <thead>
            <tr>
              <th rowSpan={2} style={headerCellStyle}>직급</th>
              {groups.map((g) => (
                <th key={g.group} colSpan={g.ranks.length} style={headerCellStyle}>
                  {g.group}
                </th>
              ))}
              <th rowSpan={2} style={totalHeaderCellStyle}>{TOTAL_LABEL}</th>
            </tr>
            <tr>
              {rows.map((r) => (
                <th key={`${r.group}-${r.label}`} style={subHeaderCellStyle}>
                  {r.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <tr>
              <th style={headerCellStyle}>인원수(명)</th>
              {rows.map((r) => (
                <td key={`${r.group}-${r.label}`} style={bodyCellStyle}>
                  {r.count}
                </td>
              ))}
              <td style={totalBodyCellStyle}>{total}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <ReactECharts
        option={horizontalBarOption(barRows, total)}
        style={{ height: chartHeight, width: '100%', marginTop: 16 }}
        notMerge
      />

      <div style={{ marginTop: 8, textAlign: 'right', fontSize: 12, color: '#8c8c8c' }}>
        기준일 [{asOfDate}]
      </div>
    </Card>
  );
}

const baseCellStyle: React.CSSProperties = {
  border: '1px solid #d9d9d9',
  padding: '6px 12px',
  whiteSpace: 'nowrap',
};

const headerCellStyle: React.CSSProperties = {
  ...baseCellStyle,
  background: '#fafafa',
  fontWeight: 600,
};

const subHeaderCellStyle: React.CSSProperties = {
  ...baseCellStyle,
  background: '#fafafa',
  fontWeight: 500,
};

const totalHeaderCellStyle: React.CSSProperties = {
  ...headerCellStyle,
  background: '#f0f0f0',
};

const bodyCellStyle: React.CSSProperties = baseCellStyle;

const totalBodyCellStyle: React.CSSProperties = {
  ...baseCellStyle,
  background: '#f0f0f0',
  fontWeight: 600,
};
