import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Card, Col, DatePicker, Empty, Row, Space, Spin, Statistic, Tabs, Tooltip } from 'antd';
import dayjs from 'dayjs';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import ReactECharts from 'echarts-for-react';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useAuthStore } from '@/stores/authStore';
import { useDashboardBranches } from '@/hooks/dashboard/useDashboardBranches';
import { SYSTEM_ADMIN_PROFILE_NAME } from '@/hooks/usePermission';
import {
  fetchDashboard,
  type AccountTypeCount,
  type AgeGroupCount,
  type ChannelWorkTypeItem,
  type DashboardResponse,
  type WorkTypeCount,
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

/** 차트 카드 제목 + 데이터 집계 기준 안내 툴팁(info 아이콘). */
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

/** 기본 현황 각 그래프의 데이터 집계 기준 안내 문구 (기간/지점 조건은 제외). */
const BASIC_CHART_INFO = {
  staffType:
    '여사원(조장·지점장 제외)의 직군을 기준으로 집계합니다. 판촉직과 OSC직(구 레이디직 포함)으로 분류하며, 두 직군에 해당하지 않거나 직군이 없는 사원은 기타로 표시합니다.',
  position:
    '여사원(조장·지점장 제외)의 재직 상태를 기준으로 집계합니다. 재직과 휴직으로 분류하며, 그 외 상태이거나 상태가 없는 사원은 기타로 표시합니다. (퇴직자는 집계에서 제외)',
  ageGroup:
    '여사원(조장·지점장 제외)의 생년월일로 만 나이를 계산하여 10세 단위(20대·30대…)로 집계합니다. 생년월일이 없거나 확인할 수 없는 사원은 미상으로 표시합니다.',
  workType:
    '월별 여사원 통합일정의 근무형태(고정·격고·순회)별 환산인원을 합산하여 집계합니다.',
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

/** 여사원 투입현황 각 그래프의 데이터 집계 기준 안내 문구 (기간/지점 조건은 제외). */
const DEPLOYMENT_CHART_INFO = {
  accountType:
    '전월(마감) 기준 월별 여사원 통합일정을 거래처유형(농협·대형마트·백화점·슈퍼·체인·홀세일 등)별로 묶어 환산인원을 합산하여 집계합니다. 거래처유형을 확인할 수 없는 일정은 미상으로 표시합니다.',
  workType:
    '당월 기준 월별 여사원 통합일정을 근무형태(진열·행사)별로 묶어 환산인원을 합산하여 집계합니다. 근무형태를 확인할 수 없는 일정은 미상으로 표시합니다.',
  previousWorkType:
    '전월(마감) 기준 월별 여사원 통합일정을 근무형태(진열·행사)별로 묶어 환산인원을 합산하여 집계합니다. 근무형태를 확인할 수 없는 일정은 미상으로 표시합니다.',
  channelWorkType:
    '당월 기준 월별 여사원 통합일정을 거래처유형(유통)별로 묶고, 다시 근무형태(고정·격고·순회)로 나누어 환산인원을 합산하여 집계합니다.',
} as const;

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

/** 유통 × 근무형태(고정/격고/순회) 그룹 막대 옵션 (환산인원). */
function channelWorkTypeOption(items: ChannelWorkTypeItem[]) {
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v: number | null) => (v == null ? '-' : `${formatHeadcount(v, 1)}명`),
    },
    legend: { data: ['고정', '격고', '순회'], top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: items.map((i) => i.channelName), axisLabel: { interval: 0, rotate: items.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [
      { name: '고정', type: 'bar', stack: 'wt', itemStyle: { color: '#1677ff' }, label: stackLabel, data: items.map((i) => i.fixedHeadcount) },
      { name: '격고', type: 'bar', stack: 'wt', itemStyle: { color: '#52c41a' }, label: stackLabel, data: items.map((i) => i.alternatingHeadcount) },
      { name: '순회', type: 'bar', stack: 'wt', itemStyle: { color: '#faad14' }, label: stackLabel, data: items.map((i) => i.visitingHeadcount) },
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

function accountTypeItems(rows: AccountTypeCount[]) {
  return rows.map((r) => ({ name: r.accountType, value: r.convertedHeadcount }));
}
function workTypeItems(rows: WorkTypeCount[]) {
  return rows.map((r) => ({ name: r.workType, value: r.convertedHeadcount }));
}
function ageGroupItems(rows: AgeGroupCount[]) {
  return rows.map((r) => ({ name: r.ageGroup, value: r.count }));
}

const CHART_HEIGHT = 320;

export default function DashboardPage() {
  const today = new Date();
  const [year, setYear] = useState<number>(today.getFullYear());
  const [month, setMonth] = useState<number>(today.getMonth() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<QueryParams>({
    yearMonth: toYearMonth(today.getFullYear(), today.getMonth() + 1),
  });

  // 시스템 관리자(전사 권한)는 마운트 시 전사 자동 조회를 막고, 지점/전체를 명시 선택해 조회를 눌렀을
  // 때만 실행한다 (무거운 전사 집계의 의도치 않은 자동 트리거 방지). 비-시스템관리자는 권한 스코프가
  // 이미 제한적이라 기존대로 마운트 자동 조회.
  const isSystemAdmin = useAuthStore(
    (state) => state.user?.profileName === SYSTEM_ADMIN_PROFILE_NAME,
  );
  const [hasSearched, setHasSearched] = useState(false);

  // 대시보드 전용 지점 목록 — 전사 권한자는 고정 화이트리스트(34개), 그 외는 본인 지점 스코프.
  // 여사원 일정 지점(useTeamScheduleBranches)과 분리하기 위해 branches 를 명시 주입한다.
  const { data: dashboardBranches = [] } = useDashboardBranches();

  const dashboardQuery = useQuery({
    queryKey: ['adminDashboard', queryParams],
    queryFn: () => fetchDashboard(queryParams.yearMonth, queryParams.branchCodes),
    enabled: !isSystemAdmin || hasSearched,
  });

  const handleSearch = () => {
    setHasSearched(true);
    setQueryParams({
      yearMonth: toYearMonth(year, month),
      // 선택한 지점을 모두 전달(다중 IN 조회). 선택 없으면 undefined → 권한 스코프 전체.
      branchCodes: selectedCodes.length > 0 ? selectedCodes : undefined,
    });
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
        <div style={{ marginBottom: 12, color: '#8c8c8c' }}>
          출근등록 거래처 {s.investedAccountCount.toLocaleString()}개
        </div>
        <Row gutter={[16, 16]}>
          <Col span={8}>
            <Card>
              {s.hasTargetData ? (
                <Statistic title={cardTitle('당월 목표', SALES_CHART_INFO.target)} value={formatThousandWon(s.targetAmount)} suffix="천원" />
              ) : (
                <Statistic title={cardTitle('당월 목표', SALES_CHART_INFO.target)} value="—" />
              )}
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              {s.hasActualData ? (
                <Statistic title={cardTitle('당월 실적', SALES_CHART_INFO.actual)} value={formatThousandWon(s.actualAmount)} suffix="천원" />
              ) : (
                <Statistic title={cardTitle('당월 실적', SALES_CHART_INFO.actual)} value="—" />
              )}
            </Card>
          </Col>
          <Col span={8}>
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
          <Col span={8}>
            <Card>
              <Statistic title={cardTitle('기준 진도율', SALES_CHART_INFO.reference)} value={s.referenceProgressRate} precision={1} suffix="%" />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              {s.hasLastYearData ? (
                <Statistic title={cardTitle('전년 동월 실적', SALES_CHART_INFO.lastYear)} value={formatThousandWon(s.lastYearAmount)} suffix="천원" />
              ) : (
                <Statistic title={cardTitle('전년 동월 실적', SALES_CHART_INFO.lastYear)} value="—" />
              )}
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              {s.hasLastYearData ? (
                <Statistic title={cardTitle('전년동월 대비 성장률', SALES_CHART_INFO.lastYearRatio)} value={s.lastYearRatio} precision={1} suffix="%" />
              ) : (
                <Statistic title={cardTitle('전년동월 대비 성장률', SALES_CHART_INFO.lastYearRatio)} value="—" />
              )}
            </Card>
          </Col>
        </Row>
        <div style={{ marginTop: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ color: '#8c8c8c' }}>
            <InfoCircleOutlined style={{ marginRight: 4 }} />
            여사원 투입거래처 기준 매출현황
          </span>
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
      </>
    );
  }, [data, queryParams.yearMonth, queryParams.branchCodes]);

  const deploymentTab = useMemo(() => {
    if (!data) return null;
    const sd = data.staffDeployment;
    const accountTypeTotal = sd.byAccountType.reduce((sum, r) => sum + r.convertedHeadcount, 0);
    const workTypeTotal = sd.byWorkType.reduce((sum, r) => sum + r.convertedHeadcount, 0);
    const prevWorkTypeTotal = sd.previousMonth.byWorkType.reduce((sum, r) => sum + r.convertedHeadcount, 0);
    const channelWorkTypeTotal = sd.byChannelAndWorkType.reduce(
      (sum, i) => sum + i.fixedHeadcount + i.alternatingHeadcount + i.visitingHeadcount,
      0,
    );
    return (
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card title={cardTitle('거래처유형별 투입현황 (전월 마감, 환산인원)', DEPLOYMENT_CHART_INFO.accountType)} extra={cardExtra(accountTypeTotal, 1)}>
            <ReactECharts option={headcountBarOption(accountTypeItems(sd.byAccountType), '#1677ff', '명', 1)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={cardTitle('근무형태 비중 (진열/행사, 환산인원)', DEPLOYMENT_CHART_INFO.workType)} extra={cardExtra(workTypeTotal, 1)}>
            <ReactECharts option={headcountBarOption(workTypeItems(sd.byWorkType), '#52c41a', '명', 1)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={cardTitle('전월 근무형태 비중 (진열/행사, 환산인원)', DEPLOYMENT_CHART_INFO.previousWorkType)} extra={cardExtra(prevWorkTypeTotal, 1)}>
            <ReactECharts option={headcountBarOption(workTypeItems(sd.previousMonth.byWorkType), '#8c8c8c', '명', 1)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={cardTitle('유통별/근무형태별 여사원 현황 (고정/격고/순회, 환산인원)', DEPLOYMENT_CHART_INFO.channelWorkType)} extra={cardExtra(channelWorkTypeTotal, 1)}>
            <ReactECharts option={channelWorkTypeOption(sd.byChannelAndWorkType)} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
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

  const basicTab = useMemo(() => {
    if (!data) return null;
    const b = data.basicStats;
    const staffTypeTotal = b.staffType.promotion + b.staffType.osc + b.staffType.etc;
    const positionTotal = b.totalByPosition.active + b.totalByPosition.onLeave + b.totalByPosition.etc;
    const ageTotal = b.byAgeGroup.reduce((sum, g) => sum + g.count, 0);
    const workTypeTotal = b.byWorkType.fixed + b.byWorkType.alternating + b.byWorkType.visiting;
    return (
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card title={cardTitle('판촉직/OSC직 인원현황', BASIC_CHART_INFO.staffType)} extra={cardExtra(staffTypeTotal)}>
            <ReactECharts
              option={donutOption([
                { name: '판촉직', value: b.staffType.promotion },
                { name: 'OSC직', value: b.staffType.osc },
                ...(b.staffType.etc > 0
                  ? [{ name: '기타', value: b.staffType.etc, breakdown: b.staffType.etcBreakdown }]
                  : []),
              ])}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
          </Card>
        </Col>
        <Col span={12}>
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
          </Card>
        </Col>
        <Col span={12}>
          <Card title={cardTitle('연령별 현황', BASIC_CHART_INFO.ageGroup)} extra={cardExtra(ageTotal)}>
            <ReactECharts option={headcountBarOption(ageGroupItems(b.byAgeGroup), '#722ed1')} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={cardTitle('근무형태별 고정/격고/순회 인원현황 (환산인원)', BASIC_CHART_INFO.workType)} extra={cardExtra(workTypeTotal, 1)}>
            <ReactECharts
              option={headcountBarOption(
                [
                  { name: '고정', value: b.byWorkType.fixed },
                  { name: '격고', value: b.byWorkType.alternating },
                  { name: '순회', value: b.byWorkType.visiting },
                ],
                '#13c2c2',
                '명',
                1,
              )}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
          </Card>
        </Col>
      </Row>
    );
  }, [data]);

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
            <DatePicker
              picker="month"
              value={dayjs(`${year}-${String(month).padStart(2, '0')}-01`)}
              onChange={(value) => {
                if (!value) return;
                setYear(value.year());
                setMonth(value.month() + 1);
              }}
              allowClear={false}
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
          defaultActiveKey="sales"
          items={[
            { key: 'sales', label: '매출현황', children: beforeSearch ? searchPrompt : salesTab },
            { key: 'deployment', label: '여사원 투입현황', children: beforeSearch ? searchPrompt : deploymentTab },
            { key: 'basic', label: '기본 현황', children: beforeSearch ? searchPrompt : basicTab },
          ]}
        />
      </Spin>
    </div>
  );
}
