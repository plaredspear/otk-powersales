import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Alert, Button, Card, Col, DatePicker, Empty, Radio, Row, Space, Spin, Statistic, Tabs, Tooltip } from 'antd';
import dayjs from 'dayjs';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import ReactECharts from 'echarts-for-react';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import RankHeadcountCard from '@/pages/dashboard/components/RankHeadcountCard';
import { femaleEmployeeLinkState } from '@/pages/dashboard/femaleEmployeeLink';
import {
  BASIC_TAB_KEY,
  readDashboardUrlState,
  toDashboardSearchParams,
} from '@/pages/dashboard/dashboardUrlState';
import { useAuthStore } from '@/stores/authStore';
import { useDashboardBranches } from '@/hooks/dashboard/useDashboardBranches';
import { SYSTEM_ADMIN_PROFILE_NAME, usePermission } from '@/hooks/usePermission';
import {
  fetchDashboard,
  type AccountTypeCount,
  type AgeGroupCount,
  type DashboardResponse,
  type WorkTypeChannelChart,
} from '@/api/adminDashboard';

interface QueryParams {
  yearMonth: string;
  branchCodes?: string[];
}

function toYearMonth(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

/** 천 단위 구분 + 소수 자리수 고정 포맷. decimals 미지정 시 원래 값 그대로(정수형). */
function formatHeadcount(v: number, decimals?: number): string {
  return decimals == null
    ? v.toLocaleString()
    : v.toLocaleString(undefined, { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
}

/** 차트 카드 우측 라벨 — 해당 그래프의 총원 + 단위. decimals 지정 시 총원을 해당 소수 자리수로 표시. */
function cardExtra(total: number, decimals?: number) {
  return <span style={{ color: '#8c8c8c' }}>총 {formatHeadcount(total, decimals)}명 (단위: 명)</span>;
}

/** 금액을 천원 단위(반올림)로 표시 — 월 매출(물류배부) KPI 카드와 동일 규칙. */
function formatThousandWon(v: number): string {
  return Math.round(v / 1000).toLocaleString();
}

/**
 * 연령별 현황 카드 제목 — 평균연령을 함께 표기한다.
 * 생년월일이 없는 사원만 있어 평균을 낼 수 없으면(null) 제목만 노출한다.
 */
function ageGroupCardTitle(averageAge: number | null): string {
  return averageAge == null ? '연령별 현황' : `연령별 현황 (평균연령: ${averageAge.toFixed(1)}세)`;
}

/** 제목 + 안내 툴팁(info 아이콘). 차트 카드 제목과 탭 라벨이 공유한다. */
function cardTitle(title: string, desc: string) {
  return (
    <span>
      {title}{' '}
      <Tooltip title={desc}>
        <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help', fontSize: 14 }} />
      </Tooltip>
    </span>
  );
}

/**
 * 기본 현황 각 그래프의 데이터 집계 기준 안내 문구 (지점 조건은 제외).
 *
 * 기본 현황 3개 차트는 모두 **사원 마스터의 현재 상태 스냅샷**이라 조회월과 무관하다 — 그래서
 * 이 탭에서는 조회월 셀렉터를 잠그고([BASIC_TAB_KEY]), 대신 인원 기준일을 카드 하단에 표기한다
 * ([asOfBadge], 서버가 KST 전일로 산출).
 *
 * 모수는 레거시 SF 홈 대시보드(조장) 인원현황 리포트 정합 — 여사원+조장, 여사원 직무 3값 한정,
 * 퇴직자·테스트 계정 제외 (backend `FemaleStaffHeadcountFilter`).
 */
const BASIC_CHART_INFO = {
  staffType:
    '전일 기준, 상단에서 선택한 집계 기준의 인원입니다. 상단 조회월과 무관합니다. 여사원·조장 중 판촉직과 OSC직(구 레이디직 포함)을 직무 기준으로 분류하며, 퇴직자와 테스트 계정은 제외합니다.',
  position:
    '전일 기준 인원입니다. 이 차트는 휴직 비율을 보는 용도라 집계 기준 선택과 무관하게 항상 전체(퇴직 제외)로 표시됩니다. 재직과 휴직으로 분류하며, 그 외 상태이거나 상태가 없는 사원은 기타로 표시합니다.',
  ageGroup:
    '전일 기준, 상단에서 선택한 집계 기준의 인원입니다. 여사원·조장의 생년월일로 만 나이를 계산하여 10세 단위(20대·30대…)로 집계합니다. 생년월일이 없거나 확인할 수 없는 사원은 미상으로 표시하며, 평균연령 계산에서도 제외합니다.',
  rank:
    '전일 기준, 상단에서 선택한 집계 기준의 인원을 직급(직위)별로 집계합니다. 판매조장은 직책 기준으로 분류하며 해당 지점에 있는 직위가 그대로 표시되고, 판촉직은 OSPM/OSPE/OSPJ, OSC직은 OSC로 나누며 그 외 직위는 기타로 합산합니다. 판매조장을 별도 열로 빼므로 판촉직 열 인원은 판촉직/OSC직 도넛보다 그만큼 적습니다.',
} as const;

/**
 * 매출현황 각 지표의 집계 기준 안내 문구 (기간/지점 조건은 제외).
 * 공통: "투입 거래처 = 해당 월 여사원 통합일정에 등장하는 거래처". 실적은 전산(ABC)+물류배부(Ship) 마감 합계 기준.
 */
const SALES_CHART_INFO = {
  target:
    '해당 월에 여사원이 투입된 거래처들의 매출 목표를 합산합니다. 목표는 거래처별 월 매출 목표(연·월 1행)를 기준으로 하며, 목표가 등록되지 않은 거래처는 0으로 계산합니다.',
  actual:
    '해당 월에 여사원이 투입된 거래처들의 당월 마감 합계 실적(전산+물류배부 합계)을 합산합니다. 반품·조정으로 음수가 나올 수 있으며, 실적 데이터가 아직 적재되지 않은 경우 "—"로 표시합니다.',
  progress:
    '당월 실적을 당월 목표로 나눈 비율(실적 ÷ 목표 × 100)입니다. 목표가 0이면 0%로 표시합니다. 기준 진도율보다 높으면 파란색, 낮으면 빨간색으로 표시됩니다.',
  reference:
    '해당 월의 달력일 경과 비율(경과 일수 ÷ 총 일수 × 100)입니다. 영업일이 아니라 달력일 기준이며, 지난 달은 100%, 다음 달은 0%로 표시됩니다.',
  lastYear:
    '해당 월에 여사원이 투입된 거래처들의 전년 동월 마감 합계 실적(전산+물류배부 합계)을 합산합니다. 반품·조정으로 음수가 나올 수 있으며, 전년 데이터가 없는 경우 "—"로 표시합니다.',
  lastYearRatio:
    '당월 실적을 전년 동월 실적으로 나눈 비율(당월 ÷ 전년 동월 × 100)입니다. 100%면 전년과 동일, 100%를 넘으면 증가입니다. 전년 데이터가 없는 경우 "—"로 표시합니다.',
} as const;

/**
 * 여사원 투입현황 6개 그래프의 데이터 집계 기준 안내 문구 (기간/지점 조건은 제외).
 * SF 레거시 조장 대시보드 "투입현황" 6개 차트 정합 — 모두 전월(마감) 전건을 다르게 집계.
 */
const DEPLOYMENT_CHART_INFO = {
  accountType:
    '전월(마감) 기준 월별 여사원 통합일정을 거래처유형(유통)별로 묶어 환산인원을 합산하여 집계합니다(진열+행사 전체). 거래처유형을 확인할 수 없는 일정은 미상으로 표시합니다.',
  channelWorkType1:
    '전월(마감) 기준 월별 여사원 통합일정을 거래처유형(유통)별로 묶고, 다시 근무형태(진열·행사)로 나누어 환산인원을 합산하여 집계합니다.',
  workType1Ratio:
    '전월(마감) 기준 월별 여사원 통합일정을 근무형태(진열·행사)별로 묶어 환산인원을 합산한 비중입니다.',
  all:
    '전월(마감) 기준 월별 여사원 통합일정을 거래처유형(유통)별로 묶고, 다시 근무형태3&4(1.고정·2.격고·3.순회·4.상온·5.냉동…)로 나누어 환산인원을 합산하여 집계합니다(진열+행사 전체).',
  display:
    '전월(마감) 기준 월별 여사원 통합일정 중 근무형태(진열)를 거래처유형(유통)별로 묶고, 다시 근무형태(1.고정·2.격고·3.순회)로 나누어 환산인원을 합산하여 집계합니다.',
  event:
    '전월(마감) 기준 월별 여사원 통합일정 중 근무형태(행사)를 거래처유형(유통)별로 묶고, 다시 근무형태(4.상온·5.냉동·5.냉장·5.라면·5.만두 등)로 나누어 환산인원을 합산하여 집계합니다.',
} as const;

/** SF 스택 세그먼트 라벨(근무형태3&4) → 색상 (스크린샷 정합). 미지정 라벨은 팔레트 순환. */
const STACK_COLOR: Record<string, string> = {
  '1.고정': '#1677ff',
  '2.격고': '#10239e',
  '3.순회': '#5cdbd3',
  '4.상온': '#69b1ff',
  '5.냉동': '#10239e',
  '5.냉장': '#5cdbd3',
  '5.라면': '#08979c',
  '5.만두': '#d4b106',
  // ② 근무유형1 스택
  진열: '#1677ff',
  행사: '#10239e',
};
/** STACK_COLOR 에 없는 라벨용 순환 팔레트. */
const STACK_FALLBACK_COLORS = ['#fa8c16', '#722ed1', '#eb2f96', '#a0d911', '#13c2c2'];

function stackColor(label: string, idx: number): string {
  return STACK_COLOR[label] ?? STACK_FALLBACK_COLORS[idx % STACK_FALLBACK_COLORS.length];
}

/** ① 거래처유형별 단일 가로막대 색상 (SF 스크린샷 정합). 미지정은 회색. */
const ACCOUNT_TYPE_COLOR: Record<string, string> = {
  군납: '#1677ff',
  기타: '#10239e',
  농협: '#95de64',
  대리점: '#08979c',
  '대형마트(3대)': '#d4b106',
  백화점: '#fa8c16',
  슈퍼: '#cf1322',
  식자재: '#ff7875',
  체인: '#52c41a',
  편의점: '#5cdbd3',
  홀세일: '#b7eb8f',
};

/** 환산인원(소수) 막대 차트 옵션 — name/value 쌍 리스트. decimals 지정 시 라벨/툴팁을 해당 소수 자리수로 표시. */
function headcountBarOption(
  items: { name: string; value: number }[],
  color: string,
  unit = '명',
  decimals?: number,
) {
  return {
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number | null) => (v == null ? '-' : `${formatHeadcount(v, decimals)}${unit}`),
    },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: items.map((i) => i.name), axisLabel: { interval: 0, rotate: items.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        barMaxWidth: 48,
        itemStyle: { color },
        label: { show: true, position: 'top', formatter: (p: { value: number }) => formatHeadcount(p.value, decimals) },
        data: items.map((i) => i.value),
      },
    ],
  };
}

/** 도넛 조각 1개 — breakdown 이 있으면 툴팁에 세부 내역을 나열한다. */
type DonutItem = {
  name: string;
  value: number;
  /** "기타" 등 집계 항목의 구성 원본 값별 세부 내역. 있으면 툴팁에 줄나열. */
  breakdown?: { label: string; count: number }[];
  /** 조각 색 지정 (미지정 시 ECharts 기본 팔레트). */
  itemStyle?: { color: string };
};

/** 도넛(파이) 차트 옵션. */
function donutOption(items: DonutItem[]) {
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number; data: DonutItem }) => {
        const head = `${p.name}<br/>${p.value.toLocaleString()}명 (${p.percent.toFixed(1)}%)`;
        const breakdown = p.data?.breakdown;
        if (!breakdown || breakdown.length === 0) return head;
        const lines = breakdown
          .map((b) => `${b.label} ${b.count.toLocaleString()}명`)
          .join('<br/>');
        return `${head}<hr style="margin:4px 0;border:none;border-top:1px solid #eee"/>${lines}`;
      },
    },
    legend: { orient: 'vertical', right: 0, top: 'middle' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        label: {
          show: true,
          formatter: (p: { name: string; value: number; percent: number }) =>
            `${p.name} ${p.value.toLocaleString()}명 (${p.percent.toFixed(1)}%)`,
        },
        labelLine: { show: true },
        data: items,
      },
    ],
  };
}

/** 스택 막대 세그먼트 내부 라벨 — 환산인원 소수 1자리. 값이 0이면 숨김. */
const stackLabel = {
  show: true,
  position: 'inside' as const,
  color: '#fff',
  fontSize: 11,
  formatter: (p: { value: number }) => (p.value > 0 ? formatHeadcount(p.value, 1) : ''),
};

/**
 * 유통(거래처유형) × 근무형태3&4 가로 누적 막대 옵션 — SF 리포트 정합.
 * Y축 = 거래처유형(chart.rows), X축 = 환산인원, 스택 = chart.stackKeys.
 */
function channelStackOption(chart: WorkTypeChannelChart) {
  const channels = chart.rows.map((r) => r.channelName);
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v: number | null) => (v == null ? '-' : `${formatHeadcount(v, 1)}명`),
    },
    legend: { data: chart.stackKeys, top: 0 },
    grid: { left: 70, right: 30, top: 40, bottom: 40 },
    xAxis: { type: 'value', name: '총 환산인원 합계', nameLocation: 'middle' as const, nameGap: 28 },
    // SF 스크린샷은 위→아래로 거래처유형이 나열되므로 y축 역순.
    yAxis: { type: 'category', data: channels, inverse: true, axisLabel: { interval: 0 } },
    series: chart.stackKeys.map((key, si) => ({
      name: key,
      type: 'bar',
      stack: 'wt',
      itemStyle: { color: stackColor(key, si) },
      label: stackLabel,
      data: chart.rows.map((r) => r.headcounts[si] ?? 0),
    })),
  };
}

/**
 * ① 거래처유형별 투입현황 — 단일 가로막대. Y축 = 거래처유형, X축 = 환산인원 SUM.
 * 막대 색은 거래처유형별 (SF 스크린샷 정합).
 */
function accountTypeBarOption(rows: AccountTypeCount[]) {
  const channels = rows.map((r) => r.accountType);
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v: number | null) => (v == null ? '-' : `${formatHeadcount(v, 1)}명`),
    },
    grid: { left: 80, right: 40, top: 30, bottom: 40 },
    xAxis: { type: 'value', name: '총 환산인원 합계', nameLocation: 'middle' as const, nameGap: 28 },
    yAxis: { type: 'category', data: channels, inverse: true, axisLabel: { interval: 0 } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 20,
        label: { show: true, position: 'right' as const, formatter: (p: { value: number }) => formatHeadcount(p.value, 1) },
        data: rows.map((r) => ({
          value: r.convertedHeadcount,
          itemStyle: { color: ACCOUNT_TYPE_COLOR[r.accountType] ?? '#8c8c8c' },
        })),
      },
    ],
  };
}

/**
 * ② 근무형태별/유통별 인원현황 — 거래처유형마다 진열/행사 2개 가로막대(그룹). 스택 아님.
 * SF 스크린샷: 각 거래처유형 아래 진열/행사 막대가 나란히.
 */
function channelWorkType1Option(chart: WorkTypeChannelChart) {
  const channels = chart.rows.map((r) => r.channelName);
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v: number | null) => (v == null ? '-' : `${formatHeadcount(v, 1)}명`),
    },
    legend: { data: chart.stackKeys, top: 0 },
    grid: { left: 70, right: 40, top: 40, bottom: 40 },
    xAxis: { type: 'value', name: '총 환산인원 합계', nameLocation: 'middle' as const, nameGap: 28 },
    yAxis: { type: 'category', data: channels, inverse: true, axisLabel: { interval: 0 } },
    // 스택 없이 그룹 막대 (진열/행사 나란히).
    series: chart.stackKeys.map((key, si) => ({
      name: key,
      type: 'bar',
      barMaxWidth: 12,
      itemStyle: { color: stackColor(key, si) },
      label: { show: true, position: 'right' as const, formatter: (p: { value: number }) => (p.value > 0 ? formatHeadcount(p.value, 1) : '') },
      data: chart.rows.map((r) => r.headcounts[si] ?? 0),
    })),
  };
}

function ageGroupItems(rows: AgeGroupCount[]) {
  return rows.map((r) => ({ name: r.ageGroup, value: r.count }));
}

const CHART_HEIGHT = 320;

/**
 * 차트 카드 반응형 폭 — 노트북(lg 이상)은 2열, 태블릿 이하는 1열.
 *
 * 도넛/막대 차트는 폭이 좁아지면 라벨과 범례가 겹쳐 읽기 어려워지므로, 반폭을 유지할 최소
 * 기준을 lg(≥992px)로 잡는다. 그 아래에서는 카드 하나가 전체 폭을 쓰게 해 차트를 온전히 보여준다.
 * (antd 기본 breakpoint: xs<576 / sm≥576 / md≥768 / lg≥992 / xl≥1200 / xxl≥1600)
 */
const CHART_COL_SPAN = { xs: 24, lg: 12 } as const;

/**
 * 표 기반 카드(인원현황) 반응형 폭 — 2단 헤더 표라 차트보다 폭이 더 필요하다.
 * xl(≥1200px) 부터 반폭을 쓰고 그 아래는 전체 폭 — lg 구간에서 반폭이면 열이 눌려 가로 스크롤이
 * 상시 발생한다.
 */
const TABLE_COL_SPAN = { xs: 24, xl: 12 } as const;

/**
 * KPI 숫자 카드(매출현황 상단 3열) 반응형 폭 — 폭이 좁아도 숫자 하나라 잘 견딘다.
 * 모바일(xs)만 1열, sm 부터 2열, lg 부터 원래의 3열.
 */
const KPI_COL_SPAN = { xs: 24, sm: 12, lg: 8 } as const;

/** 기본 현황 집계 기준 — 재직만 / 재직+휴직(퇴직 제외). */
type BasicScope = 'active' | 'includingLeave';

/** 집계 기준 토글 안내 — 총원 카드가 토글에서 빠지는 이유를 함께 설명한다. */
const BASIC_SCOPE_NOTICE =
  '인원현황·판촉직/OSC직·연령별 현황에 적용됩니다. 총원(재직/휴직)은 휴직 비율을 보는 차트라 항상 전체(퇴직 제외) 기준으로 표시됩니다.';

/**
 * 기본 현황 탭 라벨의 info 아이콘 안내 — 조회월이 잠긴 사유.
 *
 * 필터 바 안에 문구를 넣으면 지점명/조회월 정렬이 어긋나고 바 높이가 탭마다 달라지므로,
 * 탭 라벨의 tooltip 으로 안내한다.
 */
const BASIC_TAB_PERIOD_LOCK_NOTICE =
  '기본 현황은 현재 기준으로 조회합니다. 상단 조회월과 무관하며, 각 차트의 인원 기준일은 카드 하단에 표시됩니다.';

/**
 * 기본 현황 차트의 인원 기준일 배지 — 카드 우측 하단에 표기.
 *
 * 사원 정보를 일별로 수신한다는 전제에서 서버가 KST 기준 전일로 내려준다
 * (backend `BasicStats.asOfDate`). 클라이언트에서 날짜를 계산하지 않는다 —
 * 사용자 PC 의 타임존/날짜 설정에 따라 표기가 달라지는 것을 막기 위함.
 */
function asOfBadge(asOfDate: string) {
  return (
    <div style={{ marginTop: 8, textAlign: 'right', fontSize: 12, color: '#8c8c8c' }}>
      기준일 [{asOfDate}]
    </div>
  );
}

export default function DashboardPage() {
  const today = new Date();
  const [searchParams, setSearchParams] = useSearchParams();
  // 최초 마운트 시 1회만 URL 을 읽는다 — 이후 상태 변경은 setSearchParams 로 URL 에 반영하므로,
  // URL 을 매 렌더 다시 읽으면 사용자가 만지던 필터 입력이 되돌려질 수 있다.
  const [initial] = useState(() => readDashboardUrlState(searchParams, today));
  const [year, setYear] = useState<number>(initial.year);
  const [month, setMonth] = useState<number>(initial.month);
  const [selectedCodes, setSelectedCodes] = useState<string[]>(initial.branchCodes);
  const [queryParams, setQueryParams] = useState<QueryParams>({
    yearMonth: toYearMonth(initial.year, initial.month),
    branchCodes: initial.branchCodes.length > 0 ? initial.branchCodes : undefined,
  });
  // 활성 탭 — 기본 현황 탭에서는 조회월 셀렉터를 잠근다(BASIC_TAB_KEY 참조).
  const [activeTab, setActiveTab] = useState<string>(initial.tab);
  const isBasicTab = activeTab === BASIC_TAB_KEY;
  // 기본 현황 집계 기준 — 기본값은 재직(휴직 제외). 현황 확인의 기본 관심사가 실제 근무 인원이라
  // 진입 시 재직 기준으로 보여준다 (여사원 현황 목록의 상태 기본값 '재직' 과 동일한 축).
  // 서버가 두 기준을 모두 내려주므로 토글 전환 시 재조회가 없다.
  const [basicScope, setBasicScope] = useState<BasicScope>('active');

  // 시스템 관리자(전사 권한)는 마운트 시 전사 자동 조회를 막고, 지점/전체를 명시 선택해 조회를 눌렀을
  // 때만 실행한다 (무거운 전사 집계의 의도치 않은 자동 트리거 방지). 비-시스템관리자는 권한 스코프가
  // 이미 제한적이라 기존대로 마운트 자동 조회.
  const isSystemAdmin = useAuthStore(
    (state) => state.user?.profileName === SYSTEM_ADMIN_PROFILE_NAME,
  );
  // back 복원(URL 에 조회 조건이 실려 있음)이면 조회를 이미 누른 것으로 간주해 그대로 재조회한다.
  const [hasSearched, setHasSearched] = useState(initial.hasSearched);

  // 대시보드 전용 지점 목록 — 전사 권한자는 고정 화이트리스트(34개), 그 외는 본인 지점 스코프.
  // 여사원 일정 지점(useTeamScheduleBranches)과 분리하기 위해 branches 를 명시 주입한다.
  const { data: dashboardBranches = [] } = useDashboardBranches();
  const { hasEntityPermission } = usePermission();
  const canViewFemaleEmployees = hasEntityPermission('female_employee', 'READ');

  const dashboardQuery = useQuery({
    queryKey: ['adminDashboard', queryParams],
    queryFn: () => fetchDashboard(queryParams.yearMonth, queryParams.branchCodes),
    enabled: !isSystemAdmin || hasSearched,
  });

  /**
   * 탭 · 조회 조건을 URL 쿼리에 반영한다.
   *
   * `replace` 인 이유: 탭 전환/조회마다 history 가 쌓이면 여사원 현황에서 back 한 번으로
   * 대시보드에 돌아오지 못하고 이전 탭들을 되짚게 된다. 대신 대시보드 history 항목의 URL 이
   * 항상 "떠나기 직전 상태" 로 유지되어 back 복원이 정확해진다.
   */
  const syncUrl = (tab: string, params: QueryParams) => {
    setSearchParams(toDashboardSearchParams(tab, params.yearMonth, params.branchCodes), {
      replace: true,
    });
  };

  const handleSearch = () => {
    setHasSearched(true);
    const next: QueryParams = {
      yearMonth: toYearMonth(year, month),
      // 선택한 지점을 모두 전달(다중 IN 조회). 선택 없으면 undefined → 권한 스코프 전체.
      branchCodes: selectedCodes.length > 0 ? selectedCodes : undefined,
    };
    setQueryParams(next);
    syncUrl(activeTab, next);
  };

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    syncUrl(tab, queryParams);
  };

  const data: DashboardResponse | undefined = dashboardQuery.data;

  const salesTab = useMemo(() => {
    if (!data) return null;
    const s = data.salesSummary;
    // 목표 달성률 색상 — 기준 진도율 대비 높으면 파랑, 낮으면 빨강, 같으면 중립.
    // 목표 미등록(목표 0원) / 실적 미적재는 비교가 무의미하므로 중립 처리.
    const progressColor =
      s.targetAmount <= 0 || !s.hasActualData || s.progressRate === s.referenceProgressRate
        ? undefined
        : s.progressRate > s.referenceProgressRate
          ? '#1677ff'
          : '#ff4d4f';
    return (
      <>
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ color: '#8c8c8c' }}>출근등록 거래처 {s.investedAccountCount.toLocaleString()}개</span>
          <Link
            to={`/sales/monthly?yearMonth=${queryParams.yearMonth}&deploymentFilter=deployed${
              queryParams.branchCodes?.length
                ? `&branchCodes=${queryParams.branchCodes.map(encodeURIComponent).join(',')}`
                : ''
            }`}
          >
            {Number(queryParams.yearMonth.slice(5, 7))}월 월 매출(물류배부) →
          </Link>
        </div>
        <Row gutter={[16, 16]}>
          <Col {...KPI_COL_SPAN}>
            <Card>
              {s.hasTargetData ? (
                <Statistic title={cardTitle('당월 목표', SALES_CHART_INFO.target)} value={formatThousandWon(s.targetAmount)} suffix="천원" />
              ) : (
                <Statistic title={cardTitle('당월 목표', SALES_CHART_INFO.target)} value="—" />
              )}
            </Card>
          </Col>
          <Col {...KPI_COL_SPAN}>
            <Card>
              {s.hasActualData ? (
                <Statistic title={cardTitle('당월 실적', SALES_CHART_INFO.actual)} value={formatThousandWon(s.actualAmount)} suffix="천원" />
              ) : (
                <Statistic title={cardTitle('당월 실적', SALES_CHART_INFO.actual)} value="—" />
              )}
            </Card>
          </Col>
          <Col {...KPI_COL_SPAN}>
            <Card>
              <Statistic
                title={cardTitle('목표 달성률', SALES_CHART_INFO.progress)}
                value={s.progressRate}
                precision={1}
                suffix="%"
                valueStyle={progressColor ? { color: progressColor } : undefined}
              />
            </Card>
          </Col>
          <Col {...KPI_COL_SPAN}>
            <Card>
              <Statistic title={cardTitle('기준 진도율', SALES_CHART_INFO.reference)} value={s.referenceProgressRate} precision={1} suffix="%" />
            </Card>
          </Col>
          <Col {...KPI_COL_SPAN}>
            <Card>
              {s.hasLastYearData ? (
                <Statistic title={cardTitle('전년 동월 실적', SALES_CHART_INFO.lastYear)} value={formatThousandWon(s.lastYearAmount)} suffix="천원" />
              ) : (
                <Statistic title={cardTitle('전년 동월 실적', SALES_CHART_INFO.lastYear)} value="—" />
              )}
            </Card>
          </Col>
          <Col {...KPI_COL_SPAN}>
            <Card>
              {s.hasLastYearData ? (
                <Statistic title={cardTitle('전년동월 대비 성장률', SALES_CHART_INFO.lastYearRatio)} value={s.lastYearRatio} precision={1} suffix="%" />
              ) : (
                <Statistic title={cardTitle('전년동월 대비 성장률', SALES_CHART_INFO.lastYearRatio)} value="—" />
              )}
            </Card>
          </Col>
        </Row>
        <div style={{ marginTop: 16, color: '#8c8c8c' }}>
          <InfoCircleOutlined style={{ marginRight: 4 }} />
          여사원 투입거래처 기준 매출현황
        </div>
      </>
    );
  }, [data, queryParams.yearMonth, queryParams.branchCodes]);

  const deploymentTab = useMemo(() => {
    if (!data) return null;
    const sd = data.staffDeployment;
    const accountTypeTotal = sd.byAccountType.reduce((s, r) => s + r.convertedHeadcount, 0);
    const workType1Total = sd.workType1Ratio.reduce((s, r) => s + r.convertedHeadcount, 0);
    return (
      <Row gutter={[16, 16]}>
        {/* ① 거래처유형별 투입현황 (전월 마감) — 단일 가로막대 (전폭) */}
        <Col span={24}>
          <Card
            title={cardTitle('거래처유형별 투입현황 전월(마감)', DEPLOYMENT_CHART_INFO.accountType)}
            extra={cardExtra(accountTypeTotal, 1)}
          >
            <ReactECharts option={accountTypeBarOption(sd.byAccountType)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        {/* ② 근무형태별/유통별 인원현황 — 유통 × 진열/행사 그룹막대 */}
        <Col {...CHART_COL_SPAN}>
          <Card
            title={cardTitle('전월 근무형태별/유통별 인원현황', DEPLOYMENT_CHART_INFO.channelWorkType1)}
            extra={cardExtra(sd.channelWorkType1.totalHeadcount, 1)}
          >
            <ReactECharts option={channelWorkType1Option(sd.channelWorkType1)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        {/* ③ 근무형태 비중 — 진열/행사 도넛 */}
        <Col {...CHART_COL_SPAN}>
          <Card title={cardTitle('전월 근무형태 비중', DEPLOYMENT_CHART_INFO.workType1Ratio)} extra={cardExtra(workType1Total, 1)}>
            <ReactECharts
              option={donutOption(
                sd.workType1Ratio.map((r) => ({
                  name: r.workType,
                  value: r.convertedHeadcount,
                  itemStyle: { color: stackColor(r.workType, 0) },
                })),
              )}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
          </Card>
        </Col>
        {/* ④ 유통별/근무형태별 여사원현황 (All) — 유통 × 근무형태3&4 누적 (전폭) */}
        <Col span={24}>
          <Card
            title={cardTitle('전월 유통별/근무형태별 여사원현황 (All)', DEPLOYMENT_CHART_INFO.all)}
            extra={cardExtra(sd.all.totalHeadcount, 1)}
          >
            <ReactECharts option={channelStackOption(sd.all)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        {/* ⑤ 진열 누적 / ⑥ 행사 누적 */}
        <Col {...CHART_COL_SPAN}>
          <Card
            title={cardTitle('전월 유통별/근무형태별 여사원현황 (진열)', DEPLOYMENT_CHART_INFO.display)}
            extra={cardExtra(sd.display.totalHeadcount, 1)}
          >
            <ReactECharts option={channelStackOption(sd.display)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col {...CHART_COL_SPAN}>
          <Card
            title={cardTitle('전월 유통별/근무형태별 여사원현황 (행사)', DEPLOYMENT_CHART_INFO.event)}
            extra={cardExtra(sd.event.totalHeadcount, 1)}
          >
            <ReactECharts option={channelStackOption(sd.event)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={24}>
          <div style={{ textAlign: 'right' }}>
            <Link to="/monthly-input-adequacy">월 투입현황 보고서 보기 →</Link>
          </div>
        </Col>
      </Row>
    );
  }, [data]);

  /**
   * 여사원 현황 링크가 넘길 지점 코드 — **조회에 실제 사용된** 조건(queryParams)이 기준이다.
   * 셀렉터 상태(selectedCodes)를 쓰면 조회를 누르지 않고 지점만 바꿨을 때 화면 숫자와 링크가 어긋난다.
   *
   * 지점 미선택은 원칙적으로 '전체'라 링크를 열지 않지만, 애초에 지점이 1개뿐인 사용자(조장/지점장)는
   * 미선택이어도 그 1개 지점 조회와 동일하므로 링크를 연다.
   */
  const linkBranchCode = useMemo(() => {
    const applied = queryParams.branchCodes ?? [];
    if (applied.length === 1) return applied[0];
    if (applied.length === 0 && dashboardBranches.length === 1) return dashboardBranches[0].branchCode;
    return null;
  }, [queryParams.branchCodes, dashboardBranches]);

  const basicTab = useMemo(() => {
    if (!data) return null;
    const b = data.basicStats;
    const femaleEmployeeLink = femaleEmployeeLinkState({
      branchCode: linkBranchCode,
      isActiveScope: basicScope === 'active',
      canView: canViewFemaleEmployees,
    });
    // 집계 기준 토글 — 서버가 두 기준을 모두 내려주므로 전환 시 재조회가 없다.
    const s = basicScope === 'active' ? b.active : b.includingLeave;
    const staffTypeTotal = s.staffType.promotion + s.staffType.osc + s.staffType.etc;
    // 총원(재직/휴직)만 토글과 무관하게 항상 전체 기준 — 좁히면 휴직 세그먼트가 0이 되어 무의미.
    const positionTotal = b.totalByPosition.active + b.totalByPosition.onLeave + b.totalByPosition.etc;
    const ageTotal = s.byAgeGroup.reduce((sum, g) => sum + g.count, 0);
    return (
      <>
        {/* 집계 기준 행 — 우측 끝에 여사원 현황 이동 링크를 함께 배치한다.
            링크는 단일 지점 + 재직 기준일 때만 활성 (femaleEmployeeLinkState 참조). 비활성 시 사유를 tooltip 으로 안내. */}
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space size={8}>
            <span style={{ color: '#8c8c8c' }}>집계 기준:</span>
            <Radio.Group
              value={basicScope}
              onChange={(e) => setBasicScope(e.target.value as BasicScope)}
              optionType="button"
              buttonStyle="solid"
              size="small"
              options={[
                { label: '재직', value: 'active' },
                { label: '재직+휴직', value: 'includingLeave' },
              ]}
            />
            <Tooltip title={BASIC_SCOPE_NOTICE}>
              <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help' }} />
            </Tooltip>
          </Space>
          {'to' in femaleEmployeeLink ? (
            <Link to={femaleEmployeeLink.to}>여사원 현황에서 보기 →</Link>
          ) : (
            <Tooltip title={<span style={{ whiteSpace: 'pre-line' }}>{femaleEmployeeLink.disabledReason}</span>}>
              <Button type="link" disabled style={{ padding: 0 }}>
                여사원 현황에서 보기 →
              </Button>
            </Tooltip>
          )}
        </div>
      <Row gutter={[16, 16]}>
        {/* 직급별 인원현황 — 좌측 상단 첫 카드. 2단 헤더 표라 폭이 더 필요해 xl 부터 반폭.
            그래도 열이 넘치면 표만 가로 스크롤한다(RankHeadcountCard 내부 overflow-x). */}
        <Col {...TABLE_COL_SPAN}>
          <RankHeadcountCard
            groups={s.byRank}
            branchName={b.branchName}
            asOfDate={b.asOfDate}
            title={cardTitle('인원현황', BASIC_CHART_INFO.rank)}
          />
        </Col>
        <Col {...CHART_COL_SPAN}>
          <Card title={cardTitle('판촉직/OSC직 인원현황', BASIC_CHART_INFO.staffType)} extra={cardExtra(staffTypeTotal)}>
            <ReactECharts
              option={donutOption([
                { name: '판촉직', value: s.staffType.promotion },
                { name: 'OSC직', value: s.staffType.osc },
                ...(s.staffType.etc > 0
                  ? [{ name: '기타', value: s.staffType.etc, breakdown: s.staffType.etcBreakdown }]
                  : []),
              ])}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
            {asOfBadge(b.asOfDate)}
          </Card>
        </Col>
        {/* 총원(재직/휴직) — 휴직 비율을 보는 카드라 집계 기준 토글에서 제외한다. */}
        <Col {...CHART_COL_SPAN}>
          <Card title={cardTitle('총원 (재직/휴직)', BASIC_CHART_INFO.position)} extra={cardExtra(positionTotal)}>
            <ReactECharts
              option={donutOption([
                { name: '재직', value: b.totalByPosition.active },
                { name: '휴직', value: b.totalByPosition.onLeave },
                ...(b.totalByPosition.etc > 0
                  ? [{ name: '기타', value: b.totalByPosition.etc, breakdown: b.totalByPosition.etcBreakdown }]
                  : []),
              ])}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
            {asOfBadge(b.asOfDate)}
          </Card>
        </Col>
        <Col {...CHART_COL_SPAN}>
          <Card
            title={cardTitle(ageGroupCardTitle(s.averageAge), BASIC_CHART_INFO.ageGroup)}
            extra={cardExtra(ageTotal)}
          >
            <ReactECharts option={headcountBarOption(ageGroupItems(s.byAgeGroup), '#722ed1')} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
            {asOfBadge(b.asOfDate)}
          </Card>
        </Col>
      </Row>
      </>
    );
  }, [data, basicScope, linkBranchCode, canViewFemaleEmployees]);

  // 시스템 관리자가 아직 조회하지 않은 상태 — 탭 헤더는 노출하되 각 탭 콘텐츠는 조회 안내로 채운다.
  const beforeSearch = isSystemAdmin && !hasSearched;
  const searchPrompt = (
    <Empty
      style={{ marginTop: 48 }}
      description="지점 또는 전체를 선택한 뒤 조회를 눌러주세요."
    />
  );

  return (
    <div style={{ padding: 24 }}>
      <PeriodBranchFilterBar
        branches={dashboardBranches}
        year={year}
        month={month}
        selectedCodes={selectedCodes}
        onYearChange={setYear}
        onMonthChange={setMonth}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={() => undefined}
        hideExport
        searchLoading={dashboardQuery.isFetching}
        periodFilter={
          <Space direction="vertical" size={4}>
            <span>조회월:</span>
            {/* 기본 현황 탭에서는 조회월을 잠근다 — 인원 집계가 현재 시점 스냅샷이라
                월을 바꿔도 값이 변하지 않아, 열어 두면 과거 이력 조회로 오해된다.
                잠금 사유는 탭 라벨의 info 아이콘([BASIC_TAB_PERIOD_LOCK_NOTICE])이 안내한다 —
                필터 바 안에 문구를 넣으면 지점명/조회월 정렬이 어긋나고 바 높이가 탭마다 달라진다. */}
            <DatePicker
              picker="month"
              value={dayjs(`${year}-${String(month).padStart(2, '0')}-01`)}
              onChange={(value) => {
                if (!value) return;
                setYear(value.year());
                setMonth(value.month() + 1);
              }}
              allowClear={false}
              disabled={isBasicTab}
              format="YYYY-MM"
              style={{ width: 140 }}
            />
          </Space>
        }
      />

      {dashboardQuery.isError && (
        <Alert
          type="error"
          message="대시보드 조회에 실패했습니다"
          description={(dashboardQuery.error as Error)?.message}
          style={{ marginTop: 16 }}
        />
      )}

      <Spin spinning={dashboardQuery.isLoading}>
        <Tabs
          style={{ marginTop: 16 }}
          activeKey={activeTab}
          onChange={handleTabChange}
          items={[
            { key: 'sales', label: '매출현황', children: beforeSearch ? searchPrompt : salesTab },
            { key: 'deployment', label: '여사원 투입현황', children: beforeSearch ? searchPrompt : deploymentTab },
            {
              key: BASIC_TAB_KEY,
              // 조회월이 잠긴 사유를 탭 라벨의 info 아이콘으로 안내한다.
              label: cardTitle('기본 현황', BASIC_TAB_PERIOD_LOCK_NOTICE),
              children: beforeSearch ? searchPrompt : basicTab,
            },
          ]}
        />
      </Spin>
    </div>
  );
}
