import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Progress,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useHerokuFkResolveProgress,
  useStartHerokuFkResolve,
} from '@/hooks/admin/useHerokuMigration';
import type {
  HerokuFkResolveProgress,
  HerokuFkResolveStatus,
  HerokuFkTableResult,
  HerokuFkUnmatched,
} from '@/api/admin/herokuMigration';
import ResizableTable from '@/components/common/ResizableTable';

const { Title, Paragraph, Text } = Typography;

const STATUS_TAG: Record<HerokuFkResolveStatus, { color: string; label: string }> = {
  IDLE: { color: 'default', label: '대기' },
  RUNNING: { color: 'processing', label: '실행 중' },
  COMPLETED: { color: 'success', label: '완료' },
  COMPLETED_WITH_WARNINGS: { color: 'warning', label: '완료 (경고 있음)' },
  FAILED: { color: 'error', label: '실패' },
};

const UNKNOWN_STATUS_TAG = { color: 'default', label: '알 수 없음' };

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

function overallPercent(p: HerokuFkResolveProgress): number {
  if (p.totalTables === 0) return 0;
  return Math.min(100, Math.round((p.completedTables / p.totalTables) * 100));
}

const tableColumns: ColumnsType<HerokuFkTableResult> = [
  {
    title: '테이블',
    dataIndex: 'table',
    key: 'table',
    ellipsis: true,
    render: (v: string) => <Text code>{v}</Text>,
  },
  {
    title: '컬럼',
    dataIndex: 'column',
    key: 'column',
    ellipsis: true,
    render: (v: string) => <Text code>{v}</Text>,
  },
  {
    title: '적용 row 수',
    dataIndex: 'rowsAffected',
    key: 'rowsAffected',
    width: 160,
    align: 'right',
    render: (v: number) => v.toLocaleString(),
  },
];

const unmatchedColumns: ColumnsType<HerokuFkUnmatched> = [
  {
    title: '테이블',
    dataIndex: 'table',
    key: 'table',
    ellipsis: true,
    render: (v: string) => <Text code>{v}</Text>,
  },
  {
    title: '컬럼',
    dataIndex: 'column',
    key: 'column',
    ellipsis: true,
    render: (v: string) => <Text code>{v}</Text>,
  },
  {
    title: '자연 키',
    dataIndex: 'naturalKey',
    key: 'naturalKey',
    ellipsis: true,
    render: (v: string) => <Text code>{v}</Text>,
  },
  {
    title: '미매칭 수',
    dataIndex: 'unmatchedCount',
    key: 'unmatchedCount',
    width: 140,
    align: 'right',
    render: (v: number) => <Text type="warning">{v.toLocaleString()}</Text>,
  },
];

export default function HerokuMigrationPage() {
  const progressQuery = useHerokuFkResolveProgress();
  const startMutation = useStartHerokuFkResolve();

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress
    ? (STATUS_TAG[progress.status] ?? UNKNOWN_STATUS_TAG)
    : STATUS_TAG.IDLE;

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Heroku Migration — Stage 2 FK Resolve</Title>
      <Paragraph type="secondary">
        Stage 1 적재 완료 + SF 데이터 마이그레이션(employee/account/product) 선행 적재 후
        실행하세요. 패턴 A(자연키→serial id) + 패턴 B(부모 FK) 를 일괄 처리합니다.
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
              <Descriptions.Item label="현재 테이블">
                {progress.currentTable ? (
                  <Text code>{progress.currentTable}</Text>
                ) : (
                  '-'
                )}
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
                    : progress.status === 'COMPLETED_WITH_WARNINGS'
                    ? 'normal'
                    : 'active'
                }
              />
            </div>
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

          <Card title="테이블별 결과" style={{ marginBottom: 16 }}>
            <ResizableTable<HerokuFkTableResult>
              size="small"
              rowKey={(r) => `${r.table}.${r.column}`}
              columns={tableColumns}
              dataSource={progress.tableResults}
              pagination={false}
              locale={{ emptyText: '아직 완료된 테이블 없음' }}
            />
          </Card>

          {progress.unmatched.length > 0 && (
            <Card title={`미매칭 (unmatched) ${progress.unmatched.length}건`}>
              <ResizableTable<HerokuFkUnmatched>
                size="small"
                rowKey={(r) => `${r.table}.${r.column}.${r.naturalKey}`}
                columns={unmatchedColumns}
                dataSource={progress.unmatched}
                pagination={false}
              />
            </Card>
          )}
        </>
      )}
    </div>
  );
}
