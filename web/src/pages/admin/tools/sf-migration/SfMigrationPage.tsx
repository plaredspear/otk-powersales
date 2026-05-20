import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Progress,
  Radio,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useFkResolveProgress,
  useRunPicklistAll,
  useRunPicklistColumn,
  useStartFkResolve,
} from '@/hooks/admin/useSfMigration';
import type {
  FkResolveProgress,
  FkResolveStatus,
  FkResolveTableResult,
  PicklistColumn,
  PicklistResponse,
  PicklistSubstepResult,
} from '@/api/admin/sfMigration';

const { Title, Paragraph, Text } = Typography;

const STATUS_TAG: Record<FkResolveStatus, { color: string; label: string }> = {
  IDLE: { color: 'default', label: '대기' },
  RUNNING: { color: 'processing', label: '실행 중' },
  COMPLETED: { color: 'success', label: '완료' },
  FAILED: { color: 'error', label: '실패' },
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function formatDuration(start: string | null, end: string | null): string {
  if (!start) return '-';
  const endMs = end ? dayjs(end).valueOf() : Date.now();
  const seconds = Math.max(0, Math.floor((endMs - dayjs(start).valueOf()) / 1000));
  if (seconds < 60) return `${seconds}초`;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}분 ${s}초`;
}

function overallPercent(p: FkResolveProgress): number {
  if (p.totalTables === 0) return 0;
  return Math.min(100, Math.round((p.completedTables / p.totalTables) * 100));
}

function currentTablePercent(p: FkResolveProgress): number {
  if (p.currentTableTotalChunks === 0) return 0;
  return Math.min(
    100,
    Math.round((p.currentTableChunk / p.currentTableTotalChunks) * 100),
  );
}

type PicklistRunMode = 'single' | 'batch';

const PICKLIST_COLUMN_OPTIONS: Array<{ value: PicklistColumn; label: string }> = [
  { value: 'employee_role', label: 'Employee.role (한글 AppAuthority → UserRole)' },
  { value: 'employee_ppt', label: 'Employee.professional_promotion_team (한글 → enum)' },
  { value: 'user_profile_type', label: 'User.profile_type (한글 Profile.Name → ProfileType)' },
  { value: 'user_cost_center_code', label: 'User.cost_center_code (Employee 캐시 동기화)' },
];

export default function SfMigrationPage() {
  const progressQuery = useFkResolveProgress();
  const startMutation = useStartFkResolve();
  const runPicklistAllMutation = useRunPicklistAll();
  const runPicklistColumnMutation = useRunPicklistColumn();

  const [picklistMode, setPicklistMode] = useState<PicklistRunMode>('single');
  const [picklistColumn, setPicklistColumn] = useState<PicklistColumn>('user_cost_center_code');

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress ? STATUS_TAG[progress.status] : STATUS_TAG.IDLE;

  // Picklist 실행 결과는 두 mutation 중 더 최근 것을 보여준다.
  const picklistResult: PicklistResponse | undefined =
    runPicklistColumnMutation.data ?? runPicklistAllMutation.data;
  const picklistError =
    (runPicklistColumnMutation.error as Error | null) ??
    (runPicklistAllMutation.error as Error | null);
  const picklistPending =
    runPicklistAllMutation.isPending || runPicklistColumnMutation.isPending;

  const tableColumns: ColumnsType<FkResolveTableResult> = useMemo(
    () => [
      {
        title: '테이블',
        dataIndex: 'label',
        key: 'label',
        ellipsis: true,
      },
      {
        title: '적용 row 수',
        dataIndex: 'rowsAffected',
        key: 'rowsAffected',
        width: 160,
        align: 'right',
        render: (v: number) => v.toLocaleString(),
      },
    ],
    [],
  );

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>SF Migration — Stage 2 FK Resolve</Title>
      <Paragraph type="secondary">
        SF 데이터 마이그레이션 Stage 2-A (FK Resolve) 를 실행하고 진행 상태를 1초 간격으로 확인한다.
        본 작업은 1회성 cut-over 도구이며, ErpOrderProduct 등 대용량 테이블 처리 시간이 길어질 수 있다.
      </Paragraph>

      <Card style={{ marginBottom: 16 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space size="middle" wrap>
            <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
              상태: {statusTag.label}
            </Tag>
            <Button
              type="primary"
              loading={startMutation.isPending}
              disabled={isRunning}
              onClick={() => startMutation.mutate()}
            >
              {isRunning ? '진행 중…' : 'FK Resolve 실행'}
            </Button>
            <Button
              onClick={() => progressQuery.refetch()}
              disabled={progressQuery.isFetching}
            >
              새로 고침
            </Button>
          </Space>

          {startMutation.isError && (
            <Alert
              type="error"
              showIcon
              message="실행 요청 실패"
              description={(startMutation.error as Error)?.message ?? 'unknown'}
              closable
              onClose={() => startMutation.reset()}
            />
          )}
        </Space>
      </Card>

      {!progress ? (
        <Empty description="진행 상태 없음 (한 번도 실행되지 않음)" />
      ) : (
        <>
          <Card title="요약" style={{ marginBottom: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2, md: 3 }} bordered size="small">
              <Descriptions.Item label="시작">
                {formatDateTime(progress.startedAt)}
              </Descriptions.Item>
              <Descriptions.Item label="종료">
                {formatDateTime(progress.finishedAt)}
              </Descriptions.Item>
              <Descriptions.Item label="경과">
                {formatDuration(progress.startedAt, progress.finishedAt)}
              </Descriptions.Item>
            </Descriptions>

            <Space size="large" style={{ marginTop: 16 }} wrap>
              <Statistic
                title="처리 테이블"
                value={`${progress.completedTables} / ${progress.totalTables}`}
              />
              <Statistic
                title="누적 적용 row"
                value={progress.totalRowsAffected.toLocaleString()}
              />
            </Space>

            <div style={{ marginTop: 16 }}>
              <Text>전체 진행률 ({progress.completedTables}/{progress.totalTables} 테이블)</Text>
              <Progress
                percent={overallPercent(progress)}
                status={
                  progress.status === 'FAILED'
                    ? 'exception'
                    : progress.status === 'COMPLETED'
                    ? 'success'
                    : 'active'
                }
              />
            </div>

            {progress.currentTable && (
              <div style={{ marginTop: 8 }}>
                <Text>
                  현재 테이블: <Text code>{progress.currentTable}</Text>
                  {' '}
                  ({progress.currentTableChunk}/{progress.currentTableTotalChunks} chunks)
                </Text>
                <Progress
                  percent={currentTablePercent(progress)}
                  status={isRunning ? 'active' : 'normal'}
                />
              </div>
            )}
          </Card>

          {progress.errors.length > 0 && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={`경고 / 오류 ${progress.errors.length}건`}
              description={
                <ul style={{ paddingLeft: 16, margin: 0 }}>
                  {progress.errors.map((e, idx) => (
                    <li key={idx}>
                      <Text type="secondary">{e}</Text>
                    </li>
                  ))}
                </ul>
              }
            />
          )}

          <Card title="테이블별 결과">
            <Table<FkResolveTableResult>
              size="small"
              rowKey="label"
              columns={tableColumns}
              dataSource={progress.tableResults}
              pagination={false}
              locale={{ emptyText: '아직 완료된 테이블 없음' }}
            />
          </Card>
        </>
      )}

      <Card title="Stage 2-B — Picklist 변환 / Derived 캐시 동기화" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          한글 picklist 값을 enum 으로 일괄 UPDATE 한다 (Employee.role / Employee.professional_promotion_team /
          User.profile_type). 추가로 User.cost_center_code derived 캐시를 Employee.cost_center_code 기준으로
          동기화한다. 본 작업은 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Radio.Group
          value={picklistMode}
          onChange={(e) => setPicklistMode(e.target.value as PicklistRunMode)}
          optionType="button"
          buttonStyle="solid"
          disabled={picklistPending}
          style={{ marginBottom: 16 }}
        >
          <Radio.Button value="single">개별 실행 (컬럼 1개)</Radio.Button>
          <Radio.Button value="batch">일괄 실행 (4개 컬럼 전체)</Radio.Button>
        </Radio.Group>

        {picklistMode === 'single' ? (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Select<PicklistColumn>
              style={{ width: '100%', maxWidth: 540 }}
              value={picklistColumn}
              options={PICKLIST_COLUMN_OPTIONS}
              onChange={(v) => setPicklistColumn(v)}
              disabled={picklistPending}
            />
            <Space>
              <Button
                type="primary"
                loading={runPicklistColumnMutation.isPending}
                disabled={picklistPending}
                onClick={() => {
                  runPicklistAllMutation.reset();
                  runPicklistColumnMutation.mutate(picklistColumn);
                }}
              >
                실행
              </Button>
            </Space>
          </Space>
        ) : (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              4개 컬럼 (Employee.role → Employee.ppt → User.profile_type → User.cost_center_code) 을 순차 실행한다.
            </Paragraph>
            <Space>
              <Button
                type="primary"
                loading={runPicklistAllMutation.isPending}
                disabled={picklistPending}
                onClick={() => {
                  runPicklistColumnMutation.reset();
                  runPicklistAllMutation.mutate();
                }}
              >
                일괄 실행
              </Button>
            </Space>
          </Space>
        )}

        {picklistError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="Picklist 실행 실패"
            description={picklistError.message}
            closable
            onClose={() => {
              runPicklistAllMutation.reset();
              runPicklistColumnMutation.reset();
            }}
          />
        )}

        {picklistResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{picklistResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="누적 적용 row">
                {picklistResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <Table<PicklistSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '컬럼', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={picklistResult.results}
            />
          </div>
        )}
      </Card>
    </div>
  );
}
