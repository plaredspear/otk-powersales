import { useMemo, useState } from 'react';
import {
  Alert,
  Card,
  Col,
  DatePicker,
  Row,
  Select,
  Space,
  Statistic,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useScheduledJobCatalog,
  useScheduledJobRuns,
  useScheduledJobSummary,
} from '@/hooks/admin/useScheduledJobs';
import type {
  RegisteredScheduledJob,
  ScheduledJobRun,
  ScheduledJobStatus,
} from '@/api/admin/scheduledJob';
import JobRunDetailModal from './JobRunDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const STATUS_TAG_COLOR: Record<ScheduledJobStatus, string> = {
  SUCCESS: 'green',
  FAILURE: 'red',
  RUNNING: 'blue',
};

const STATUS_OPTIONS: { label: string; value: ScheduledJobStatus }[] = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'FAILURE', value: 'FAILURE' },
  { label: 'RUNNING', value: 'RUNNING' },
];

/** 잡 이름 → 탭에 표시할 10자 이내 한글 라벨. 미매핑 잡은 원본 jobName 으로 폴백. */
const JOB_LABELS: Record<string, string> = {
  'agreement-word-cycle-batch': '약관 동의 리셋',
  'attendance-sap-batch': '근태 SAP전송',
  'display-master-sap-batch': '진열 SAP전송',
  'display-master-last-month-revenue-batch': '진열 전월매출',
  'mfeis-this-month-revenue-batch': '일정 전월매출',
  'account-naver-geocode-batch': '거래처 좌표변환',
  'pptMaster.expire': '행사조 만료',
  'pptMaster.syncValid': '행사조 sync',
  'sap.processPostponedAppointments': '연기예약 처리',
  'salesProgressRateMaster.sync': '목표마스터 sync',
  'sap-outbox-worker': 'SAP outbox',
  'scheduledJobRun.cleanup': '이력 정리',
};

function jobLabel(jobName: string): string {
  return JOB_LABELS[jobName] ?? jobName;
}

/** 잡 이름 → 사람이 읽기 쉬운 실행 주기. cron placeholder(override 가능) 잡은 "기본" 표기. */
const JOB_SCHEDULES: Record<string, string> = {
  'agreement-word-cycle-batch': '매일 09시',
  'attendance-sap-batch': '기본 매일 01시',
  'display-master-sap-batch': '기본 매일 01시',
  'display-master-last-month-revenue-batch': '기본 매일 02시',
  'mfeis-this-month-revenue-batch': '기본 매월 1일 03시',
  'account-naver-geocode-batch': '매일 02시',
  'pptMaster.expire': '매일 23시',
  'pptMaster.syncValid': '매일 05시',
  'sap.processPostponedAppointments': '매일 자정',
  'salesProgressRateMaster.sync': '기본 1시간 주기',
  'sap-outbox-worker': '기본 30초 주기',
  'scheduledJobRun.cleanup': '매일 04시',
};

/** 잡 이름 → 해당 스케줄 작업이 무슨 일을 하는지에 대한 자연어 설명. */
const JOB_DESCRIPTIONS: Record<string, string> = {
  'agreement-word-cycle-batch':
    'GPS 위치정보 동의문구의 6개월 재동의 주기를 처리합니다. 신규 약관이 도래하면 기존 약관을 비활성화하고 신규 약관을 활성화하며(다음 활성일을 6개월 뒤로 갱신), 약관이 교체된 경우 전 사원의 동의 플래그를 일괄 해제합니다. 그 결과 사원들은 다음 로그인 시 GPS 재동의 화면을 다시 거치게 됩니다.',
  'attendance-sap-batch':
    '전날~오늘 기준의 정규 근태(여사원 일정) 데이터를 페이지 단위로 조회하여 SAP로 전송합니다. 신규 시스템에 쌓인 근태 실적을 SAP에 동기화하기 위한 아웃바운드 연동 배치로, 페이지별 성공/실패 건수를 집계해 실행 이력에 기록합니다.',
  'display-master-sap-batch':
    '유효 상태의 진열(디스플레이) 작업 일정 마스터를 페이지 단위로 조회하여 사원코드·거래처키·작업유형 등을 SAP로 전송합니다. 진열 업무 실적을 SAP에 반영하기 위한 아웃바운드 연동 배치이며, 전송 성공/실패 페이지 수를 집계해 이력에 남깁니다.',
  'display-master-last-month-revenue-batch':
    '유효 상태의 진열 작업 일정 마스터를 순회하며 각 거래처의 직전월 매출을 조회하여 해당 마스터의 전월 매출 값을 최신값으로 갱신합니다. 값이 비어 있거나 기존 값과 다른 경우에만 갱신해 불필요한 변경을 피합니다. 화면에서 진열 거래처의 전월 매출 참고치를 보여주기 위한 데이터를 매일 새로 채웁니다.',
  'mfeis-this-month-revenue-batch':
    '"상시" 분류의 월간 여사원 통합 일정 마스터를 대상으로, 각 거래처의 최근 6개월간 양수 매출 평균을 계산하여 이번 달 금액 값에 저장합니다. 거래처별로 양수 매출만 합산·평균하며, 기존 값과 차이가 있는 행만 갱신합니다. 월초에 한 번 갱신해 여사원이 거래처 방문 시 참고할 평균 매출 기준치를 제공합니다.',
  'account-naver-geocode-batch':
    '위경도 좌표가 비어 있는 "거래" 상태의 거래처를 최대 1000건 조회하여, 각 거래처 주소를 네이버 지도 Geocode API로 변환해 위도·경도를 채워 넣습니다. 거래처 위치를 지도에 표시하기 위한 좌표 보강 배치로, 좌표가 채워지지 못한 거래처는 다음 실행 때 다시 처리 대상이 됩니다.',
  'pptMaster.expire':
    '오늘로 만료되는 전문 판촉팀(PPT) 마스터를 조회하여, 해당 사원에게 더 이상 유효한 PPT 마스터가 남아 있지 않으면 사원의 전문 판촉팀 소속을 해제합니다. 판촉 활동 기간이 끝난 사원을 자동으로 일반 소속 상태로 되돌립니다.',
  'pptMaster.syncValid':
    '현재 유효한 전문 판촉팀(PPT) 마스터를 조회하여, 마스터에 지정된 팀 유형과 사원의 실제 소속 팀이 다르면 사원의 소속을 마스터 기준으로 맞춥니다. 판촉팀 배정 마스터를 권위 출처로 삼아 사원의 팀 소속을 매일 정합시킵니다.',
  'sap.processPostponedAppointments':
    '발령 예정일이 오늘 이전인 사원들을 조회하여, 각 사원의 최신 발령 정보를 즉시 적용하고 사용자 프로필 캐시를 갱신합니다. 미래 시점으로 예약된 인사 발령이 효력 발생일에 도달했을 때 자동으로 반영하며, 발령 데이터가 없으면 예약 표시만 해제하고 건너뜁니다.',
  'salesProgressRateMaster.sync':
    'Salesforce의 거래처목표등록마스터 변경분을 가져와 연+월+거래처코드 기준으로 신규 DB에 upsert합니다. 기존 행은 목표/실적/영업진도율/지점 컬럼을 갱신하고 없으면 새로 추가하며, 거래처코드로 거래처를 연결합니다. Salesforce에서 입력된 목표 데이터를 주기적으로 동기화하는 연동 배치이며, 삭제는 반영하지 않는 upsert 전용입니다.',
  'sap-outbox-worker':
    '송신 대기(PENDING)·재시도(RETRY) 상태의 SAP 아웃박스 행을 일괄 조회하여, 각 행의 인터페이스 ID로 엔드포인트를 찾아 페이로드를 SAP REST 어댑터로 전송합니다. 응답을 검증해 성공이면 SENT, 실패면 재시도 한도 내에서 RETRY, 한도 초과 시 FAILED로 상태를 갱신하고 도메인별 후처리를 수행합니다. 트랜잭셔널 아웃박스 패턴의 범용 SAP 송신 엔진입니다.',
  'scheduledJobRun.cleanup':
    '배치 실행 이력 테이블에서 보존 기간(90일)을 초과한 오래된 행을 일괄 삭제로 정리합니다. 배치 실행 로그가 무한히 쌓이는 것을 막기 위한 보존 정책 정리 배치입니다.',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return '-';
  if (ms < 1000) return `${ms}ms`;
  const seconds = ms / 1000;
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  const minutes = Math.floor(seconds / 60);
  const remainSec = Math.round(seconds - minutes * 60);
  return `${minutes}m ${remainSec}s`;
}

const ALL_JOBS_KEY = '__all__';
const CATALOG_KEY = '__catalog__';

export default function ScheduledJobsPage() {
  const [activeTab, setActiveTab] = useState<string>(ALL_JOBS_KEY);
  const [status, setStatus] = useState<ScheduledJobStatus | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [selectedRun, setSelectedRun] = useState<ScheduledJobRun | null>(null);

  const from = range?.[0]?.toISOString() ?? undefined;
  const to = range?.[1]?.toISOString() ?? undefined;

  // 잡 이름별 탭: '전체' 탭이면 jobName 필터 없음, 잡 탭이면 해당 잡 이름으로 필터.
  const jobName =
    activeTab === ALL_JOBS_KEY || activeTab === CATALOG_KEY ? undefined : activeTab;

  const summaryQuery = useScheduledJobSummary(24);
  const catalogQuery = useScheduledJobCatalog();
  const runsQuery = useScheduledJobRuns({
    jobName,
    status,
    from,
    to,
    page,
    size,
  });

  // 등록된 작업(catalog) 기준으로 잡 이름 탭 목록 구성.
  const jobNames = useMemo(
    () => (catalogQuery.data ?? []).map((entry) => entry.jobName),
    [catalogQuery.data],
  );

  // 잡 이름 → cron 표현식 (설명 헤더에 실행 주기 함께 표기).
  const cronByJob = useMemo(() => {
    const map: Record<string, string> = {};
    (catalogQuery.data ?? []).forEach((entry) => {
      map[entry.jobName] = entry.cron;
    });
    return map;
  }, [catalogQuery.data]);

  const runColumns: ColumnsType<ScheduledJobRun> = [
    {
      title: '시작 시각',
      dataIndex: 'startedAt',
      key: 'startedAt',
      width: 180,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '잡 이름',
      dataIndex: 'jobName',
      key: 'jobName',
      width: 220,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: ScheduledJobStatus) => (
        <Tag color={STATUS_TAG_COLOR[value]}>{value}</Tag>
      ),
    },
    {
      title: '소요시간',
      dataIndex: 'durationMs',
      key: 'durationMs',
      width: 100,
      render: (value: number | null) => formatDuration(value),
    },
    {
      title: '오류 메시지',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      ellipsis: true,
      render: (value: string | null) => value || '-',
    },
  ];

  const catalogColumns: ColumnsType<RegisteredScheduledJob> = [
    { title: '잡 이름', dataIndex: 'jobName', key: 'jobName', width: 240 },
    { title: 'cron 표현식', dataIndex: 'cron', key: 'cron', width: 280 },
    { title: '설명', dataIndex: 'description', key: 'description' },
  ];

  // 잡 탭에서는 '잡 이름' 컬럼이 중복이므로 제외하고, '전체' 탭에서만 노출.
  const visibleRunColumns =
    jobName === undefined
      ? runColumns
      : runColumns.filter((col) => col.key !== 'jobName');

  const runsHistoryNode = (
    <>
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="상태"
          style={{ width: 140 }}
          value={status}
          onChange={(value) => {
            setStatus(value ?? undefined);
            setPage(1);
          }}
          options={STATUS_OPTIONS}
        />
        <RangePicker
          showTime
          value={range as [Dayjs, Dayjs] | null}
          onChange={(value) => {
            setRange(value as [Dayjs | null, Dayjs | null] | null);
            setPage(1);
          }}
        />
      </Space>
      <ResizableTable<ScheduledJobRun>
        rowKey="id"
        loading={runsQuery.isLoading}
        dataSource={runsQuery.data?.items ?? []}
        columns={visibleRunColumns}
        pagination={{
          current: page,
          pageSize: size,
          total: runsQuery.data?.totalCount ?? 0,
          onChange: setPage,
          showSizeChanger: false,
        }}
        onRow={(record) => ({
          onClick: () => setSelectedRun(record),
          style: { cursor: 'pointer' },
        })}
      />
    </>
  );

  const tabItems = [
    {
      key: ALL_JOBS_KEY,
      label: '전체',
      children: runsHistoryNode,
    },
    ...jobNames.map((name) => ({
      key: name,
      label: (
        <span title={name} style={{ display: 'inline-block', lineHeight: 1.3 }}>
          {jobLabel(name)}
          {JOB_SCHEDULES[name] && (
            <span style={{ display: 'block', fontSize: 11, color: '#999' }}>
              {JOB_SCHEDULES[name]}
            </span>
          )}
        </span>
      ),
      children: (
        <>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message={
              <Space size={8} wrap>
                <Text strong>{jobLabel(name)}</Text>
                <Text type="secondary" code>
                  {name}
                </Text>
                {cronByJob[name] && (
                  <Text type="secondary">cron: {cronByJob[name]}</Text>
                )}
              </Space>
            }
            description={JOB_DESCRIPTIONS[name] ?? '등록된 설명이 없습니다.'}
          />
          {runsHistoryNode}
        </>
      ),
    })),
    {
      key: CATALOG_KEY,
      label: '등록된 작업',
      children: (
        <ResizableTable<RegisteredScheduledJob>
          rowKey="jobName"
          loading={catalogQuery.isLoading}
          dataSource={catalogQuery.data ?? []}
          columns={catalogColumns}
          pagination={false}
        />
      ),
    },
  ];

  // 상단 요약 카드와 실행 이력 목록은 한 화면의 핵심 데이터라 함께 새로고침한다.
  const handleRefresh = () => {
    summaryQuery.refetch();
    runsQuery.refetch();
  };

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
        <Title level={3} style={{ margin: 0 }}>스케줄 잡 실행 이력</Title>
        <RefreshButton
          onRefresh={handleRefresh}
          refreshing={summaryQuery.isFetching || runsQuery.isFetching}
        />
      </Space>
      <Text type="secondary">
        backend `@Scheduled` 배치의 실행 상태를 조회합니다. 요약 카드는 최근 24시간 윈도우 기준입니다.
      </Text>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="총 실행"
              value={summaryQuery.data?.totalCount ?? 0}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="SUCCESS"
              value={summaryQuery.data?.successCount ?? 0}
              valueStyle={{ color: '#52c41a' }}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="FAILURE"
              value={summaryQuery.data?.failureCount ?? 0}
              valueStyle={{ color: '#ff4d4f' }}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="RUNNING"
              value={summaryQuery.data?.runningCount ?? 0}
              valueStyle={{ color: '#1677ff' }}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
      </Row>
      {summaryQuery.data && (
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          윈도우: {formatDateTime(summaryQuery.data.windowFrom)} ~ {formatDateTime(summaryQuery.data.windowTo)}
        </Text>
      )}

      <Tabs
        tabPosition="left"
        style={{ marginTop: 24 }}
        activeKey={activeTab}
        onChange={(key) => {
          setActiveTab(key);
          setPage(1);
        }}
        items={tabItems}
      />

      <JobRunDetailModal
        run={selectedRun}
        open={selectedRun !== null}
        onClose={() => setSelectedRun(null)}
      />
    </div>
  );
}
