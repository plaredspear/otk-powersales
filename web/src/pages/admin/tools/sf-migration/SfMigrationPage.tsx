import { useMemo } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Progress,
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
  useStartFkResolve,
} from '@/hooks/admin/useSfMigration';
import type {
  FkResolveProgress,
  FkResolveStatus,
  FkResolveTableResult,
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

export default function SfMigrationPage() {
  const progressQuery = useFkResolveProgress();
  const startMutation = useStartFkResolve();

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress ? STATUS_TAG[progress.status] : STATUS_TAG.IDLE;

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
    </div>
  );
}
