import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Card, Col, Empty, Row, Spin, Statistic, Tabs, Tag } from 'antd';
import { useQuery } from '@tanstack/react-query';
import ReactECharts from 'echarts-for-react';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useAuthStore } from '@/stores/authStore';
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
  branchCode?: string;
}

function toYearMonth(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

/** 환산인원(소수) 막대 차트 옵션 — name/value 쌍 리스트. */
function headcountBarOption(
  items: { name: string; value: number }[],
  color: string,
  unit = '명',
) {
  return {
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number | null) => (v == null ? '-' : `${v.toLocaleString()}${unit}`),
    },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: items.map((i) => i.name), axisLabel: { interval: 0, rotate: items.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        barMaxWidth: 48,
        itemStyle: { color },
        label: { show: true, position: 'top', formatter: (p: { value: number }) => p.value.toLocaleString() },
        data: items.map((i) => i.value),
      },
    ],
  };
}

/** 도넛(파이) 차트 옵션. */
function donutOption(items: { name: string; value: number }[]) {
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; percent: number }) =>
        `${p.name}<br/>${p.value.toLocaleString()}명 (${p.percent}%)`,
    },
    legend: { orient: 'vertical', right: 0, top: 'middle' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        data: items,
      },
    ],
  };
}

/** 유통 × 근무형태(고정/격고/순회) 그룹 막대 옵션 (환산인원). */
function channelWorkTypeOption(items: ChannelWorkTypeItem[]) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['고정', '격고', '순회'], top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: items.map((i) => i.channelName), axisLabel: { interval: 0, rotate: items.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [
      { name: '고정', type: 'bar', stack: 'wt', itemStyle: { color: '#1677ff' }, data: items.map((i) => i.fixedHeadcount) },
      { name: '격고', type: 'bar', stack: 'wt', itemStyle: { color: '#52c41a' }, data: items.map((i) => i.alternatingHeadcount) },
      { name: '순회', type: 'bar', stack: 'wt', itemStyle: { color: '#faad14' }, data: items.map((i) => i.visitingHeadcount) },
    ],
  };
}

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

  const dashboardQuery = useQuery({
    queryKey: ['adminDashboard', queryParams],
    queryFn: () => fetchDashboard(queryParams.yearMonth, queryParams.branchCode),
    enabled: !isSystemAdmin || hasSearched,
  });

  const handleSearch = () => {
    setHasSearched(true);
    setQueryParams({
      yearMonth: toYearMonth(year, month),
      // 대시보드는 단일 지점 또는 전체(권한 스코프). 단일 선택 시에만 branchCode 전달.
      branchCode: selectedCodes.length === 1 ? selectedCodes[0] : undefined,
    });
  };

  const data: DashboardResponse | undefined = dashboardQuery.data;

  const salesTab = useMemo(() => {
    if (!data) return null;
    const s = data.salesSummary;
    return (
      <>
        <Alert
          type="info"
          message="매출 목표 데이터는 준비 중입니다. 현재는 실적 / 기준진도율 / 전년 대비만 표시됩니다."
          style={{ marginBottom: 16 }}
        />
        <Row gutter={16}>
          <Col span={6}>
            <Card>
              <Statistic title="당월 실적" value={s.actualAmount} suffix="원" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="기준 진도율" value={s.referenceProgressRate} precision={1} suffix="%" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="전년 동월 실적" value={s.lastYearAmount} suffix="원" />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="전년 대비" value={s.lastYearRatio} precision={1} suffix="%" />
            </Card>
          </Col>
        </Row>
        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Link to="/sales/monthly">월 매출(물류배부) 보고서 보기 →</Link>
        </div>
      </>
    );
  }, [data]);

  const deploymentTab = useMemo(() => {
    if (!data) return null;
    const sd = data.staffDeployment;
    return (
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card title="거래처유형별 투입현황 (전월 마감, 환산인원)">
            <ReactECharts option={headcountBarOption(accountTypeItems(sd.byAccountType), '#1677ff')} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="근무형태 비중 (진열/행사, 환산인원)">
            <ReactECharts option={headcountBarOption(workTypeItems(sd.byWorkType), '#52c41a')} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="전월 근무형태 비중 (진열/행사, 환산인원)">
            <ReactECharts option={headcountBarOption(workTypeItems(sd.previousMonth.byWorkType), '#8c8c8c')} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="유통별/근무형태별 여사원 현황 (고정/격고/순회, 환산인원)">
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
    return (
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card title="판촉직/OSC직 인원현황">
            <ReactECharts
              option={donutOption([
                { name: '판촉직', value: b.staffType.promotion },
                { name: 'OSC직', value: b.staffType.osc },
              ])}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="총원 (재직/휴직)">
            <ReactECharts
              option={donutOption([
                { name: '재직', value: b.totalByPosition.active },
                { name: '휴직', value: b.totalByPosition.onLeave },
              ])}
              style={{ height: CHART_HEIGHT, width: '100%' }}
              notMerge
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="연령별 현황">
            <ReactECharts option={headcountBarOption(ageGroupItems(b.byAgeGroup), '#722ed1')} style={{ height: CHART_HEIGHT, width: '100%' }} notMerge />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="근무형태별 고정/격고/순회 인원현황">
            <ReactECharts
              option={headcountBarOption(
                [
                  { name: '고정', value: b.byWorkType.fixed },
                  { name: '격고', value: b.byWorkType.alternating },
                  { name: '순회', value: b.byWorkType.visiting },
                ],
                '#13c2c2',
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
      />

      {dashboardQuery.isError && (
        <Alert
          type="error"
          message="대시보드 조회에 실패했습니다"
          description={(dashboardQuery.error as Error)?.message}
          style={{ marginTop: 16 }}
        />
      )}

      {!beforeSearch && data && (
        <div style={{ marginTop: 12, color: 'rgba(0,0,0,0.65)' }}>
          조회 조건:{' '}
          <Tag color="blue">{data.salesSummary.yearMonth}</Tag>
          <Tag color="geekblue">지점: {data.salesSummary.branchName ?? '전체'}</Tag>
        </div>
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
