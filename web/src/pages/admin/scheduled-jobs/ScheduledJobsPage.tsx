import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  DatePicker,
  Popconfirm,
  Select,
  Space,
  Switch,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useOroraDailyChunks,
  useOroraMonthlyChunks,
  useScheduledJobCatalog,
  useScheduledJobRuns,
  useScheduledJobSummary,
  useTriggerOroraDailyMaterialize,
  useTriggerOroraDailyMaterializeChunk,
  useTriggerOroraMonthlyMaterialize,
  useTriggerOroraMonthlyMaterializeChunk,
  useTriggerPptMaster,
  useSetScheduledJobRuntimeEnabled,
} from '@/hooks/admin/useScheduledJobs';
import type {
  PptMasterTriggerAction,
  RegisteredScheduledJob,
  ScheduledJobRun,
  ScheduledJobStatus,
} from '@/api/admin/scheduledJob';
import { usePermission } from '@/hooks/usePermission';
import JobRunDetailModal from './JobRunDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const STATUS_TAG_COLOR: Record<ScheduledJobStatus, string> = {
  SUCCESS: 'green',
  FAILURE: 'red',
  RUNNING: 'blue',
  SKIPPED: 'default',
};

const STATUS_OPTIONS: { label: string; value: ScheduledJobStatus }[] = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'FAILURE', value: 'FAILURE' },
  { label: 'RUNNING', value: 'RUNNING' },
  { label: 'SKIPPED', value: 'SKIPPED' },
];

/** 잡 이름 → 탭에 표시할 10자 이내 한글 라벨. 미매핑 잡은 원본 jobName 으로 폴백. */
const JOB_LABELS: Record<string, string> = {
  'agreement-word-cycle-batch': '약관 동의 리셋',
  'attendance-sap-batch': '여사원일정 SAP전송',
  'display-master-sap-batch': '진열마스터 SAP전송',
  'ppt-master-sap-batch': 'PPT마스터 SAP전송',
  'display-master-last-month-revenue-batch': '진열 전월매출',
  'mfeis-this-month-revenue-batch': '일정 평균매출',
  'account-naver-geocode-batch': '거래처 좌표변환',
  'pptMaster.expire': '금일 전문행사조 마감',
  'pptMaster.syncValid': '금일 전문행사조 반영',
  'sap.processPostponedAppointments': '연기예약 처리',
  'claimMaster.sync': '클레임 업데이트',
  'logisticsClaimMaster.sync': '물류클레임 업데이트',
  'salesProgressRateMaster.sync': '목표마스터 sync',
  'sap-outbox-worker': 'SAP outbox',
  'scheduledJobRun.cleanup': '이력 정리',
  'orora-daily-sales-materialize-batch': 'ORORA 일매출',
  'orora-monthly-sales-materialize-batch': 'ORORA 월매출',
};

function jobLabel(jobName: string): string {
  return JOB_LABELS[jobName] ?? jobName;
}

/** 잡 이름 → 사람이 읽기 쉬운 실행 주기. cron placeholder(override 가능) 잡은 "기본" 표기. */
const JOB_SCHEDULES: Record<string, string> = {
  'agreement-word-cycle-batch': '매일 자정',
  'attendance-sap-batch': '기본 매일 01시',
  'display-master-sap-batch': '기본 매일 23시',
  'ppt-master-sap-batch': '기본 매일 12시',
  'display-master-last-month-revenue-batch': '기본 매일 02시',
  'mfeis-this-month-revenue-batch': '기본 매월 1일 03시',
  'account-naver-geocode-batch': '매일 02시',
  'pptMaster.expire': '매일 23:30',
  'pptMaster.syncValid': '매시간 44분',
  'sap.processPostponedAppointments': '매일 자정',
  'claimMaster.sync': '기본 매시 정각',
  'logisticsClaimMaster.sync': '기본 매시 30분',
  'salesProgressRateMaster.sync': '기본 1시간 주기',
  'sap-outbox-worker': '기본 30초 주기',
  'scheduledJobRun.cleanup': '매일 04시',
  'orora-daily-sales-materialize-batch': '기본 매일 04:30',
  'orora-monthly-sales-materialize-batch': '기본 매월 3일 05시',
};

/** 잡 이름 → 해당 스케줄 작업이 무슨 일을 하는지에 대한 자연어 설명. */
const JOB_DESCRIPTIONS: Record<string, string> = {
  'agreement-word-cycle-batch':
    'GPS 위치정보 동의문구의 6개월 재동의 주기를 처리합니다. 신규 약관이 도래하면 기존 약관을 비활성화하고 신규 약관을 활성화하며(다음 활성일을 6개월 뒤로 갱신), 약관이 교체된 경우 전 사원의 동의 플래그를 일괄 해제합니다. 그 결과 사원들은 다음 로그인 시 GPS 재동의 화면을 다시 거치게 됩니다.',
  'attendance-sap-batch':
    '근무형태가 “근무”인 여사원일정을 페이지 단위로 조회하여 SAP로 전송합니다. 기준일(오늘) 일정은 출퇴근 로그 유무와 무관하게 전송하고, 전일 일정은 출퇴근 로그가 연결된 건에 한해 2차 근무형태를 채워 재전송합니다. 신규 시스템에 쌓인 여사원일정 실적을 SAP에 동기화하는 아웃바운드 연동 배치로, 페이지별 성공/실패 건수를 집계해 실행 이력에 기록합니다.',
  'display-master-sap-batch':
    '유효 상태의 진열(디스플레이) 작업 일정 마스터를 페이지 단위로 조회하여 사원코드·거래처키·작업유형 등을 SAP로 전송합니다. 진열 업무 실적을 SAP에 반영하기 위한 아웃바운드 연동 배치이며, 전송 성공/실패 페이지 수를 집계해 이력에 남깁니다.',
  'ppt-master-sap-batch':
    '실행일이 속한 당월에 유효 기간이 걸치는 전문행사조(PPT) 마스터 전량을 페이지 단위로 조회하여 사원·거래처·기간·유효상태 등 마스터 정보를 SAP로 전송합니다. 전송 완료 플래그 없이 당월 마스터를 매번 다시 송신하는 스냅샷 방식의 아웃바운드 연동 배치이며, 전송 성공/실패 페이지 수를 집계해 이력에 남깁니다.',
  'display-master-last-month-revenue-batch':
    '유효 상태의 진열 작업 일정 마스터를 순회하며 각 거래처의 직전월 매출을 조회하여 해당 마스터의 전월 매출 값을 최신값으로 갱신합니다. 값이 비어 있거나 기존 값과 다른 경우에만 갱신해 불필요한 변경을 피합니다. 화면에서 진열 거래처의 전월 매출 참고치를 보여주기 위한 데이터를 매일 새로 채웁니다.',
  'mfeis-this-month-revenue-batch':
    '"상시" 분류의 월간 여사원 통합 일정 마스터를 대상으로, 각 거래처의 최근 6개월간 양수 매출 평균을 계산하여 이번 달 금액 값에 저장합니다. 거래처별로 양수 매출만 합산·평균하며, 기존 값과 차이가 있는 행만 갱신합니다. 월초에 한 번 갱신해 여사원이 거래처 방문 시 참고할 평균 매출 기준치를 제공합니다.',
  'account-naver-geocode-batch':
    '위경도 좌표가 비어 있는 "거래" 상태의 거래처를 최대 1000건 조회하여, 각 거래처 주소를 네이버 지도 Geocode API로 변환해 위도·경도를 채워 넣습니다. 거래처 위치를 지도에 표시하기 위한 좌표 보강 배치로, 좌표가 채워지지 못한 거래처는 다음 실행 때 다시 처리 대상이 됩니다.',
  'pptMaster.expire':
    '오늘로 만료되는(확정 + 종료일=오늘) 전문 판촉팀(PPT) 마스터를 조회하여, 해당 사원의 전문 판촉팀 소속을 해제(일반 복귀)합니다. 미확정 마스터는 제외하며, 판촉 활동 기간이 끝난 사원을 자동으로 일반 소속 상태로 되돌리고, 이후 sync 배치가 잔여 유효 마스터 기준으로 소속을 다시 정합시킵니다.',
  'pptMaster.syncValid':
    '현재 유효한(확정 + 기간 내) 전문 판촉팀(PPT) 마스터를 조회하여, 마스터에 지정된 팀 유형과 사원의 실제 소속 팀이 다르면 사원의 소속을 마스터 기준으로 맞춥니다. 미확정 마스터는 제외하며, 판촉팀 배정 마스터를 권위 출처로 삼아 사원의 팀 소속을 매시간 정합시킵니다.',
  'sap.processPostponedAppointments':
    '발령 예정일이 오늘 이전인 사원들을 조회하여, 각 사원의 최신 발령 정보를 즉시 적용하고 사용자 프로필 캐시를 갱신합니다. 미래 시점으로 예약된 인사 발령이 효력 발생일에 도달했을 때 자동으로 반영하며, 발령 데이터가 없으면 예약 표시만 해제하고 건너뜁니다.',
  'salesProgressRateMaster.sync':
    'Salesforce의 거래처목표등록마스터 변경분을 가져와 연+월+거래처코드 기준으로 신규 DB에 upsert합니다. 기존 행은 목표/실적/영업진도율/지점 컬럼을 갱신하고 없으면 새로 추가하며, 거래처코드로 거래처를 연결합니다. Salesforce에서 입력된 목표 데이터를 주기적으로 동기화하는 연동 배치이며, 삭제는 반영하지 않는 upsert 전용입니다.',
  'sap-outbox-worker':
    '송신 대기(PENDING)·재시도(RETRY) 상태의 SAP 아웃박스 행을 일괄 조회하여, 각 행의 인터페이스 ID로 엔드포인트를 찾아 페이로드를 SAP REST 어댑터로 전송합니다. 응답을 검증해 성공이면 SENT, 실패면 재시도 한도 내에서 RETRY, 한도 초과 시 FAILED로 상태를 갱신하고 도메인별 후처리를 수행합니다. 트랜잭셔널 아웃박스 패턴의 범용 SAP 송신 엔진입니다.',
  'scheduledJobRun.cleanup':
    '배치 실행 이력 테이블에서 보존 기간(90일)을 초과한 오래된 행을 일괄 삭제로 정리합니다. 배치 실행 로그가 무한히 쌓이는 것을 막기 위한 보존 정책 정리 배치입니다.',
    'orora-daily-sales-materialize-batch':
    'ORORA 영업시스템의 일별 매출 view를 거래처 범위 단위로 조회하여 daily_sales_history 테이블에 적재하고, 같은 거래처·월의 monthly_sales_history 전산마감실적 합계·물류마감실적 합계·총 원장매출을 갱신합니다. 일자는 대상월이 당월이면 오늘, 과거월이면 그 달 말일로 보정해 저장하며, 거래처가 매칭되지 않는 매출 행은 적재하지 않습니다. 당월을 대상으로 매일 실행됩니다. 아래에서 특정 월을 지정해 수동 재적재할 수 있습니다. (legacy Queueable_OroraDailySalesHistory_M1 + DailyErpSalesInfoTriggerHandler 동등)',
  'orora-monthly-sales-materialize-batch':
    'ORORA 영업시스템의 월별 마감 view(ABC/물류 마감실적)를 거래처 범위 단위로 조회하여 monthly_sales_history 테이블에 적재(upsert)합니다. 익월 초에 전월 마감분을 적재하며, 운영 목표/비고/마감확정 컬럼은 보존합니다. 아래에서 특정 월을 지정해 수동 재적재할 수 있습니다. (legacy IF_REST_ORORA_ReceiveMonthlySalesHistory 동등)',
};

/** ORORA 월매출 수동 트리거 대상 jobName (해당 탭에서만 수동 적재 UI 노출). */
const ORORA_MONTHLY_JOB = 'orora-monthly-sales-materialize-batch';

/** ORORA 일매출 수동 트리거 대상 jobName (해당 탭에서만 수동 적재 UI 노출). */
const ORORA_DAILY_JOB = 'orora-daily-sales-materialize-batch';

/** 전문행사조(PPT) 마스터 수동 실행 대상 jobName → 트리거 action 매핑. */
const PPT_MASTER_TRIGGER_ACTIONS: Record<string, PptMasterTriggerAction> = {
  'pptMaster.expire': 'expire',
  'pptMaster.syncValid': 'sync',
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

/**
 * ORORA 월매출 수동 적재 패널. 월 선택(미지정 시 전월) 후 실행하면 backend 가 ORORA view 를 조회해
 * 해당 월을 monthly_sales_history 에 upsert 한다. `MODIFY_ALL_DATA` 권한자에게만 노출.
 */
function OroraMonthlyTriggerPanel() {
  const { message } = App.useApp();
  const { hasSystemPermission } = usePermission();
  const [month, setMonth] = useState<Dayjs | null>(null);
  const trigger = useTriggerOroraMonthlyMaterialize();

  if (!hasSystemPermission('MODIFY_ALL_DATA')) {
    return null;
  }

  const salesMonth = month ? month.format('YYYYMM') : undefined;

  const handleRun = () => {
    trigger.mutate(salesMonth, {
      onSuccess: (result) => {
        message.success(result.message ?? `${result.salesMonth} 적재를 시작했습니다.`);
      },
      onError: (err) => {
        message.error(err instanceof Error ? err.message : 'ORORA 월매출 수동 적재 접수에 실패했습니다');
      },
    });
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }} title="수동 적재">
      <Space wrap align="center">
        <Text type="secondary">대상 월</Text>
        <DatePicker
          picker="month"
          placeholder="미지정 시 전월"
          value={month}
          onChange={(value) => setMonth(value)}
          disabledDate={(current) => current && current > dayjs().endOf('month')}
        />
        <Popconfirm
          title="ORORA 월매출 수동 적재"
          description={`${salesMonth ?? '전월'} 마감분을 ORORA에서 조회하여 적재합니다. 실행하시겠습니까?`}
          okText="실행"
          cancelText="취소"
          onConfirm={handleRun}
        >
          <Button type="primary" loading={trigger.isPending}>
            적재 실행
          </Button>
        </Popconfirm>
        <Text type="secondary" style={{ fontSize: 12 }}>
          ORORA view 를 조회해 monthly_sales_history 에 upsert 합니다. 외부 연동이라 수십 초~수 분 걸릴 수 있어
          백그라운드로 실행되며, 실행 버튼은 즉시 접수만 반환합니다. 진행/결과는 아래 실행 이력에서 확인하세요.
        </Text>
      </Space>
    </Card>
  );
}

/**
 * ORORA 월매출 거래처 청크 단위 수동 적재 패널. 전체 거래처 범위를 도는 전체 실행과 달리, 거래처
 * 청크(약 2,000 거래처 폭) 1개만 선택해 적재한다. 특정 거래처 구간만 재적재하거나 부분 점검할 때 사용.
 * `MODIFY_ALL_DATA` 권한자에게만 노출.
 */
function OroraMonthlyChunkTriggerPanel() {
  const { message } = App.useApp();
  const { hasSystemPermission } = usePermission();
  const [month, setMonth] = useState<Dayjs | null>(null);
  const [chunkIndex, setChunkIndex] = useState<number | undefined>(undefined);
  const chunksQuery = useOroraMonthlyChunks();
  const trigger = useTriggerOroraMonthlyMaterializeChunk();

  if (!hasSystemPermission('MODIFY_ALL_DATA')) {
    return null;
  }

  const salesMonth = month ? month.format('YYYYMM') : undefined;
  const chunkOptions = (chunksQuery.data?.chunks ?? []).map((chunk) => ({
    value: chunk.chunkIndex,
    label: `${chunk.chunkIndex + 1}번 (거래처 ${chunk.fromAccountCode} ~ ${chunk.toAccountCode})`,
  }));

  const handleRun = () => {
    if (chunkIndex === undefined) {
      message.warning('실행할 거래처 청크를 선택하세요');
      return;
    }
    trigger.mutate(
      { chunkIndex, salesMonth },
      {
        onSuccess: (result) => {
          message.success(
            result.message ?? `${result.salesMonth} ${chunkIndex + 1}번 청크 적재를 시작했습니다.`,
          );
        },
        onError: (err) => {
          message.error(
            err instanceof Error ? err.message : 'ORORA 월매출 청크 수동 적재 접수에 실패했습니다',
          );
        },
      },
    );
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }} title="거래처 청크 단위 적재">
      <Space wrap align="center">
        <Text type="secondary">대상 월</Text>
        <DatePicker
          picker="month"
          placeholder="미지정 시 전월"
          value={month}
          onChange={(value) => setMonth(value)}
          disabledDate={(current) => current && current > dayjs().endOf('month')}
        />
        <Text type="secondary">거래처 청크</Text>
        <Select
          style={{ minWidth: 320 }}
          placeholder={chunksQuery.isLoading ? '청크 목록 불러오는 중…' : '청크 선택'}
          loading={chunksQuery.isLoading}
          value={chunkIndex}
          onChange={(value: number) => setChunkIndex(value)}
          options={chunkOptions}
          showSearch
          optionFilterProp="label"
        />
        <Popconfirm
          title="ORORA 월매출 청크 수동 적재"
          description={`${salesMonth ?? '전월'} 마감분 중 ${
            chunkIndex === undefined ? '선택한' : `${chunkIndex + 1}번`
          } 거래처 청크만 ORORA에서 조회하여 적재합니다. 실행하시겠습니까?`}
          okText="실행"
          cancelText="취소"
          onConfirm={handleRun}
          disabled={chunkIndex === undefined}
        >
          <Button type="primary" loading={trigger.isPending} disabled={chunkIndex === undefined}>
            청크 적재 실행
          </Button>
        </Popconfirm>
        <Text type="secondary" style={{ fontSize: 12 }}>
          선택한 거래처 청크 1개 구간만 적재합니다 (특정 거래처 구간 재적재 / 부분 점검용).
        </Text>
      </Space>
    </Card>
  );
}

/**
 * ORORA 일매출 수동 적재 패널. 월 선택(미지정 시 당월) 후 실행하면 backend 가 ORORA view 를 조회해
 * 해당 월의 일별 매출을 daily_sales_history 에 upsert 하고 monthly_sales_history 합계를 갱신한다.
 * `MODIFY_ALL_DATA` 권한자에게만 노출.
 */
function OroraDailyTriggerPanel() {
  const { message } = App.useApp();
  const { hasSystemPermission } = usePermission();
  const [month, setMonth] = useState<Dayjs | null>(null);
  const trigger = useTriggerOroraDailyMaterialize();

  if (!hasSystemPermission('MODIFY_ALL_DATA')) {
    return null;
  }

  const salesMonth = month ? month.format('YYYYMM') : undefined;

  const handleRun = () => {
    trigger.mutate(salesMonth, {
      onSuccess: (result) => {
        message.success(result.message ?? `${result.salesMonth} 적재를 시작했습니다.`);
      },
      onError: (err) => {
        message.error(err instanceof Error ? err.message : 'ORORA 일매출 수동 적재 접수에 실패했습니다');
      },
    });
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }} title="수동 적재">
      <Space wrap align="center">
        <Text type="secondary">대상 월</Text>
        <DatePicker
          picker="month"
          placeholder="미지정 시 당월"
          value={month}
          onChange={(value) => setMonth(value)}
          disabledDate={(current) => current && current > dayjs().endOf('month')}
        />
        <Popconfirm
          title="ORORA 일매출 수동 적재"
          description={`${salesMonth ?? '당월'} 일별 매출을 ORORA에서 조회하여 적재합니다. 실행하시겠습니까?`}
          okText="실행"
          cancelText="취소"
          onConfirm={handleRun}
        >
          <Button type="primary" loading={trigger.isPending}>
            적재 실행
          </Button>
        </Popconfirm>
        <Text type="secondary" style={{ fontSize: 12 }}>
          ORORA view 를 조회해 daily_sales_history 에 upsert + 월합계를 갱신합니다. 외부 연동이라 수십 초~수 분
          걸릴 수 있어 백그라운드로 실행되며, 실행 버튼은 즉시 접수만 반환합니다. 진행/결과는 아래 실행 이력에서 확인하세요.
        </Text>
      </Space>
    </Card>
  );
}

/**
 * ORORA 일매출 거래처 청크 단위 수동 적재 패널. 전체 거래처 범위를 도는 전체 실행과 달리, 거래처
 * 청크(약 2,000 거래처 폭) 1개만 선택해 적재한다. 특정 거래처 구간만 재적재하거나 부분 점검할 때 사용.
 * `MODIFY_ALL_DATA` 권한자에게만 노출.
 */
function OroraDailyChunkTriggerPanel() {
  const { message } = App.useApp();
  const { hasSystemPermission } = usePermission();
  const [month, setMonth] = useState<Dayjs | null>(null);
  const [chunkIndex, setChunkIndex] = useState<number | undefined>(undefined);
  const chunksQuery = useOroraDailyChunks();
  const trigger = useTriggerOroraDailyMaterializeChunk();

  if (!hasSystemPermission('MODIFY_ALL_DATA')) {
    return null;
  }

  const salesMonth = month ? month.format('YYYYMM') : undefined;
  const chunkOptions = (chunksQuery.data?.chunks ?? []).map((chunk) => ({
    value: chunk.chunkIndex,
    label: `${chunk.chunkIndex + 1}번 (거래처 ${chunk.fromAccountCode} ~ ${chunk.toAccountCode})`,
  }));

  const handleRun = () => {
    if (chunkIndex === undefined) {
      message.warning('실행할 거래처 청크를 선택하세요');
      return;
    }
    trigger.mutate(
      { chunkIndex, salesMonth },
      {
        onSuccess: (result) => {
          message.success(
            result.message ?? `${result.salesMonth} ${chunkIndex + 1}번 청크 적재를 시작했습니다.`,
          );
        },
        onError: (err) => {
          message.error(
            err instanceof Error ? err.message : 'ORORA 일매출 청크 수동 적재 접수에 실패했습니다',
          );
        },
      },
    );
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }} title="거래처 청크 단위 적재">
      <Space wrap align="center">
        <Text type="secondary">대상 월</Text>
        <DatePicker
          picker="month"
          placeholder="미지정 시 당월"
          value={month}
          onChange={(value) => setMonth(value)}
          disabledDate={(current) => current && current > dayjs().endOf('month')}
        />
        <Text type="secondary">거래처 청크</Text>
        <Select
          style={{ minWidth: 320 }}
          placeholder={chunksQuery.isLoading ? '청크 목록 불러오는 중…' : '청크 선택'}
          loading={chunksQuery.isLoading}
          value={chunkIndex}
          onChange={(value: number) => setChunkIndex(value)}
          options={chunkOptions}
          showSearch
          optionFilterProp="label"
        />
        <Popconfirm
          title="ORORA 일매출 청크 수동 적재"
          description={`${salesMonth ?? '당월'} 일별 매출 중 ${
            chunkIndex === undefined ? '선택한' : `${chunkIndex + 1}번`
          } 거래처 청크만 ORORA에서 조회하여 적재합니다. 실행하시겠습니까?`}
          okText="실행"
          cancelText="취소"
          onConfirm={handleRun}
          disabled={chunkIndex === undefined}
        >
          <Button type="primary" loading={trigger.isPending} disabled={chunkIndex === undefined}>
            청크 적재 실행
          </Button>
        </Popconfirm>
        <Text type="secondary" style={{ fontSize: 12 }}>
          선택한 거래처 청크 1개 구간만 적재합니다 (특정 거래처 구간 재적재 / 부분 점검용).
        </Text>
      </Space>
    </Card>
  );
}

/**
 * 전문행사조(PPT) 마스터 배치 수동 실행 패널. "금일 전문행사조 마감"(expire) / "금일 전문행사조 반영"
 * (sync) 탭에서 노출되며, 실행 시 backend 가 즉시 배치를 1회 돌리고 결과를 이력에 남긴다.
 * 사원 행사조 소속을 변경하므로 `MODIFY_ALL_DATA` 권한자에게만 버튼을 노출한다.
 */
function PptMasterTriggerPanel({
  action,
  jobLabelText,
}: {
  action: PptMasterTriggerAction;
  jobLabelText: string;
}) {
  const { message } = App.useApp();
  const { hasSystemPermission } = usePermission();
  const trigger = useTriggerPptMaster();

  if (!hasSystemPermission('MODIFY_ALL_DATA')) {
    return null;
  }

  const handleRun = () => {
    trigger.mutate(action, {
      onSuccess: (result) => {
        message.success(`${jobLabelText} 수동 실행 완료 (${result.status})`);
      },
      onError: (err) => {
        message.error(err instanceof Error ? err.message : '전문행사조 배치 수동 실행에 실패했습니다');
      },
    });
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }} title="수동 실행">
      <Space wrap align="center">
        <Popconfirm
          title={`${jobLabelText} 수동 실행`}
          description="지금 즉시 배치를 1회 실행합니다. 사원 행사조 소속이 변경될 수 있습니다. 실행하시겠습니까?"
          okText="실행"
          cancelText="취소"
          onConfirm={handleRun}
        >
          <Button type="primary" loading={trigger.isPending}>
            지금 실행
          </Button>
        </Popconfirm>
        <Text type="secondary" style={{ fontSize: 12 }}>
          실행 결과는 아래 이력 목록에 자동 스케줄 실행과 동일하게 기록됩니다 (수동 실행은 metadata 의 triggeredBy=MANUAL 로 구분).
        </Text>
      </Space>
    </Card>
  );
}

/**
 * 스케줄 잡 런타임 활성/비활성 토글 패널.
 * - 활성: 자동 스케줄 발화 시 본문 실행 (기존 동작).
 * - 비활성: 발화해도 본문을 실행하지 않고 SKIPPED 이력만 남긴다.
 *
 * 빈 등록 여부인 정적 활성(`staticEnabled`)이 false 인 잡은 애초에 발화하지 않으므로 스위치를 비활성화한다.
 * `MODIFY_ALL_DATA` 권한자에게만 노출.
 */
function RuntimeTogglePanel({
  jobName,
  jobLabelText,
  runtimeEnabled,
  staticEnabled,
}: {
  jobName: string;
  jobLabelText: string;
  runtimeEnabled: boolean;
  staticEnabled: boolean;
}) {
  const { message } = App.useApp();
  const { hasSystemPermission } = usePermission();
  const toggle = useSetScheduledJobRuntimeEnabled();

  if (!hasSystemPermission('MODIFY_ALL_DATA')) {
    return null;
  }

  const handleToggle = (checked: boolean) => {
    toggle.mutate(
      { jobName, enabled: checked },
      {
        onSuccess: () => {
          message.success(`'${jobLabelText}' 처리를 ${checked ? '활성화' : '비활성화'}했습니다.`);
        },
        onError: (err) => {
          message.error(err instanceof Error ? err.message : '처리 상태 변경에 실패했습니다.');
        },
      },
    );
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }} title="런타임 처리 토글">
      <Space wrap align="center">
        <Text type="secondary">자동 실행 처리</Text>
        <Switch
          checked={runtimeEnabled}
          disabled={!staticEnabled}
          loading={toggle.isPending}
          onChange={handleToggle}
          checkedChildren="활성"
          unCheckedChildren="비활성"
        />
        {!runtimeEnabled && staticEnabled && (
          <Tag color="warning" style={{ marginInlineEnd: 0 }}>
            발화 시 실행 생략 (SKIPPED 이력만 기록)
          </Tag>
        )}
        <Text type="secondary" style={{ fontSize: 12 }}>
          {staticEnabled
            ? '비활성화하면 자동 스케줄이 발화해도 본문을 실행하지 않고 SKIPPED 이력만 남깁니다. 수동 실행은 토글과 무관하게 항상 동작합니다.'
            : '현재 환경에서 정적으로 비활성(빈 미등록)이라 토글 대상이 아닙니다.'}
        </Text>
      </Space>
    </Card>
  );
}

interface RunsAppliedFilters {
  status?: ScheduledJobStatus;
  from?: string;
  to?: string;
}

/**
 * 실행 이력 목록 + 조회 필터 패널. 탭(전체 / 잡별)마다 독립 인스턴스로 렌더되어,
 * 페이지네이션·조회 필터 상태를 각 탭이 개별 보관한다. 초기 상태는 필터 없음이라
 * 탭 진입 시 별도 조회 조건 없이도 최신순(backend startedAt DESC) 이력이 바로 보인다.
 *
 * @param jobName '전체' 탭이면 undefined (전 잡 대상), 잡 탭이면 해당 잡 이름.
 * @param showJobNameColumn '전체' 탭에서만 '잡 이름' 컬럼을 노출한다.
 */
function RunsHistory({
  jobName,
  showJobNameColumn,
  onSelectRun,
}: {
  jobName: string | undefined;
  showJobNameColumn: boolean;
  onSelectRun: (run: ScheduledJobRun) => void;
}) {
  // 조회 조건 버퍼 — "조회" 버튼 시점에만 applied 로 반영 (필터 변경만으로 조회하지 않음)
  const [statusInput, setStatusInput] = useState<ScheduledJobStatus | undefined>(undefined);
  const [rangeInput, setRangeInput] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [applied, setApplied] = useState<RunsAppliedFilters>({});
  // 0-indexed 페이지 (buildListPagination 표준). 서버 조회 시 1-indexed 로 보정한다.
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const handleSearch = () => {
    setPage(0);
    setApplied({
      status: statusInput,
      from: rangeInput?.[0]?.toISOString() ?? undefined,
      to: rangeInput?.[1]?.toISOString() ?? undefined,
    });
  };

  const runsQuery = useScheduledJobRuns({
    jobName,
    ...applied,
    page: page + 1,
    size,
  });

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

  // 잡 탭에서는 '잡 이름' 컬럼이 중복이므로 제외하고, '전체' 탭에서만 노출.
  const visibleRunColumns = showJobNameColumn
    ? runColumns
    : runColumns.filter((col) => col.key !== 'jobName');

  return (
    <>
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="상태"
          style={{ width: 140 }}
          value={statusInput}
          onChange={(value) => setStatusInput(value ?? undefined)}
          options={STATUS_OPTIONS}
        />
        <RangePicker
          showTime
          value={rangeInput as [Dayjs, Dayjs] | null}
          onChange={(value) => setRangeInput(value as [Dayjs | null, Dayjs | null] | null)}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
      </Space>
      <ResizableTable<ScheduledJobRun>
        rowKey="id"
        loading={runsQuery.isLoading}
        dataSource={runsQuery.data?.items ?? []}
        columns={visibleRunColumns}
        pagination={buildListPagination({
          page,
          pageSize: size,
          total: runsQuery.data?.totalCount ?? 0,
          onPageChange: setPage,
          onSizeChange: (nextSize) => {
            setSize(nextSize);
            setPage(0);
          },
        })}
        locale={listTableLocale()}
        onRow={(record) => ({
          onClick: () => onSelectRun(record),
          style: { cursor: 'pointer' },
        })}
      />
    </>
  );
}

export default function ScheduledJobsPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<string>(ALL_JOBS_KEY);
  const [selectedRun, setSelectedRun] = useState<ScheduledJobRun | null>(null);

  const summaryQuery = useScheduledJobSummary(24);
  const catalogQuery = useScheduledJobCatalog();

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

  // 잡 이름 → 현재 환경 활성 여부 (탭 라벨에 비활성 표기). catalog 미로딩 시 기본 활성 가정.
  const enabledByJob = useMemo(() => {
    const map: Record<string, boolean> = {};
    (catalogQuery.data ?? []).forEach((entry) => {
      map[entry.jobName] = entry.enabled;
    });
    return map;
  }, [catalogQuery.data]);

  // 잡 이름 → 런타임 토글 활성 여부 (운영 중 끄고 켜기). catalog 미로딩 시 기본 활성 가정.
  const runtimeEnabledByJob = useMemo(() => {
    const map: Record<string, boolean> = {};
    (catalogQuery.data ?? []).forEach((entry) => {
      map[entry.jobName] = entry.runtimeEnabled;
    });
    return map;
  }, [catalogQuery.data]);

  const catalogColumns: ColumnsType<RegisteredScheduledJob> = [
    {
      title: '상태',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 90,
      filters: [
        { text: '활성', value: true },
        { text: '비활성', value: false },
      ],
      onFilter: (value, record) => record.enabled === value,
      render: (enabled: boolean) =>
        enabled ? <Tag color="green">활성</Tag> : <Tag color="default">비활성</Tag>,
    },
    { title: '잡 이름', dataIndex: 'jobName', key: 'jobName', width: 240 },
    { title: 'cron 표현식', dataIndex: 'cron', key: 'cron', width: 280 },
    { title: '설명', dataIndex: 'description', key: 'description' },
  ];

  const tabItems = [
    {
      key: ALL_JOBS_KEY,
      label: '전체',
      children: (
        <RunsHistory jobName={undefined} showJobNameColumn onSelectRun={setSelectedRun} />
      ),
    },
    ...jobNames.map((name) => {
      // catalog 미로딩 중에는 활성으로 간주(undefined !== false), 로딩 후 false 면 비활성 표기.
      const isDisabled = enabledByJob[name] === false;
      // 탭 활성화 아이콘(초록 점)은 런타임 처리 토글(runtimeEnabled)까지 반영해 토글 패널과 동기화한다.
      // 정적 비활성(빈 미등록)이거나 런타임 토글 OFF 면 회색(비활성)으로 표시.
      const runtimeOff = runtimeEnabledByJob[name] === false;
      const isTabInactive = isDisabled || runtimeOff;
      // 라벨 접미 문구는 사유를 구분: 정적 비활성 우선, 그다음 런타임 토글 OFF.
      const inactiveSuffix = isDisabled ? '(비활성)' : runtimeOff ? '(처리 중지)' : null;
      return {
        key: name,
        label: (
          <span
            title={
              isDisabled
                ? `${name} (비활성)`
                : runtimeOff
                  ? `${name} (런타임 처리 중지)`
                  : name
            }
            style={{
              display: 'inline-flex',
              alignItems: 'flex-start',
              gap: 6,
              lineHeight: 1.3,
              color: isTabInactive ? '#bfbfbf' : undefined,
            }}
          >
            <span
              aria-label={isTabInactive ? '비활성' : '활성'}
              style={{
                flex: '0 0 auto',
                width: 8,
                height: 8,
                marginTop: 5,
                borderRadius: '50%',
                backgroundColor: isTabInactive ? '#d9d9d9' : '#52c41a',
              }}
            />
            <span style={{ display: 'inline-block' }}>
              {jobLabel(name)}
              {inactiveSuffix && (
                <span style={{ fontSize: 11, marginLeft: 4 }}>{inactiveSuffix}</span>
              )}
              {JOB_SCHEDULES[name] && (
                <span
                  style={{
                    display: 'block',
                    fontSize: 11,
                    color: isTabInactive ? '#cfcfcf' : '#999',
                  }}
                >
                  {JOB_SCHEDULES[name]}
                </span>
              )}
            </span>
          </span>
        ),
        children: (
          <>
            <Alert
              type={isDisabled ? 'warning' : 'info'}
              showIcon
              style={{ marginBottom: 16 }}
              message={
                <Space size={8} wrap>
                  <Text strong>{jobLabel(name)}</Text>
                  {isDisabled ? (
                    <Tag color="default">비활성</Tag>
                  ) : (
                    <Tag color="green">활성</Tag>
                  )}
                  <Text type="secondary" code>
                    {name}
                  </Text>
                  {cronByJob[name] && (
                    <Text type="secondary">cron: {cronByJob[name]}</Text>
                  )}
                </Space>
              }
              description={
                <>
                  {JOB_DESCRIPTIONS[name] ?? '등록된 설명이 없습니다.'}
                  {isDisabled && (
                    <>
                      <br />
                      <Text type="secondary">
                        현재 환경에서 비활성화되어 있어 자동 실행되지 않습니다. 과거 실행 이력만 조회됩니다.
                      </Text>
                    </>
                  )}
                </>
              }
            />
            <RuntimeTogglePanel
              jobName={name}
              jobLabelText={jobLabel(name)}
              runtimeEnabled={runtimeEnabledByJob[name] !== false}
              staticEnabled={enabledByJob[name] !== false}
            />
            {name === ORORA_MONTHLY_JOB && (
              <>
                <OroraMonthlyTriggerPanel />
                <OroraMonthlyChunkTriggerPanel />
              </>
            )}
            {name === ORORA_DAILY_JOB && (
              <>
                <OroraDailyTriggerPanel />
                <OroraDailyChunkTriggerPanel />
              </>
            )}
            {PPT_MASTER_TRIGGER_ACTIONS[name] && (
              <PptMasterTriggerPanel
                action={PPT_MASTER_TRIGGER_ACTIONS[name]}
                jobLabelText={jobLabel(name)}
              />
            )}
            <RunsHistory
              jobName={name}
              showJobNameColumn={false}
              onSelectRun={setSelectedRun}
            />
          </>
        ),
      };
    }),
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
          locale={listTableLocale()}
        />
      ),
    },
  ];

  // 상단 요약 카드와 (현재 활성 탭의) 실행 이력 목록은 한 화면의 핵심 데이터라 함께 새로고침한다.
  // 실행 이력 페이지네이션 상태는 각 탭의 RunsHistory 가 개별 보관하므로, 여기서는 'runs' 쿼리
  // 캐시를 무효화해 마운트된 탭이 스스로 refetch 하게 한다.
  const handleRefresh = () => {
    summaryQuery.refetch();
    queryClient.invalidateQueries({ queryKey: ['admin', 'scheduled-jobs', 'runs'] });
  };

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
        <Title level={3} style={{ margin: 0 }}>스케줄 잡</Title>
        <RefreshButton onRefresh={handleRefresh} refreshing={summaryQuery.isFetching} />
      </Space>
      <Text type="secondary">
        backend `@Scheduled` 배치의 실행 상태를 조회합니다. 요약 카드는 최근 24시간 윈도우 기준입니다.
      </Text>

      <Space size="small" wrap style={{ marginTop: 16 }}>
        <Text type="secondary">총 실행 {summaryQuery.data?.totalCount ?? 0}</Text>
        <Text type="secondary">·</Text>
        <Text style={{ color: '#52c41a' }}>SUCCESS {summaryQuery.data?.successCount ?? 0}</Text>
        <Text type="secondary">·</Text>
        <Text style={{ color: '#ff4d4f' }}>FAILURE {summaryQuery.data?.failureCount ?? 0}</Text>
        <Text type="secondary">·</Text>
        <Text style={{ color: '#1677ff' }}>RUNNING {summaryQuery.data?.runningCount ?? 0}</Text>
        {summaryQuery.data && (
          <Text type="secondary">
            (윈도우: {formatDateTime(summaryQuery.data.windowFrom)} ~ {formatDateTime(summaryQuery.data.windowTo)})
          </Text>
        )}
      </Space>

      <Tabs
        tabPosition="left"
        style={{ marginTop: 24 }}
        // 세로 탭이 잡 수만큼 길어져 화면을 넘어가므로, 탭 바 영역만 스크롤되도록 높이를 제한한다.
        tabBarStyle={{ maxHeight: 'calc(100vh - 240px)', overflowY: 'auto' }}
        activeKey={activeTab}
        onChange={(key) => {
          setActiveTab(key);
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
