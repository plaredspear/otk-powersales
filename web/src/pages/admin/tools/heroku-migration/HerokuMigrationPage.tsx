import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Progress,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useHerokuFkResolveProgress,
  useHerokuSfidFkResolvableTables,
  useHerokuSfidFkResolveProgress,
  useRunHerokuPasswordHash,
  useStartHerokuFkResolve,
  useStartHerokuSfidFkResolve,
} from '@/hooks/admin/useHerokuMigration';
import type {
  HerokuFkResolveProgress,
  HerokuFkResolveStatus,
  HerokuFkTableResult,
  HerokuFkUnmatched,
  HerokuPasswordHashSubstepResult,
} from '@/api/admin/herokuMigration';
import type {
  FkResolveProgress,
  FkResolveTableResult,
} from '@/api/admin/sfMigration';
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

function sfidOverallPercent(p: FkResolveProgress): number {
  if (p.totalTables === 0) return 0;
  return Math.min(100, Math.round((p.completedTables / p.totalTables) * 100));
}

function sfidCurrentTablePercent(p: FkResolveProgress): number {
  if (p.currentTableTotalChunks === 0) return 0;
  return Math.min(
    100,
    Math.round((p.currentTableChunk / p.currentTableTotalChunks) * 100),
  );
}

const sfidTableColumns: ColumnsType<FkResolveTableResult> = [
  { title: '테이블', dataIndex: 'label', key: 'label', ellipsis: true },
  {
    title: '적용 row 수',
    dataIndex: 'rowsAffected',
    key: 'rowsAffected',
    width: 160,
    align: 'right',
    render: (v: number) => v.toLocaleString(),
  },
];

/**
 * Heroku 전용 sfid 테이블 (safety_check_submission / product_expiration) 의 FK Resolve 섹션.
 *
 * 실행 엔진은 SF Stage 2 를 재사용하므로 진행 상태 타입은 SF 의 chunk 단위 progress 를 공유한다.
 */
function HerokuSfidFkResolveCard() {
  const progressQuery = useHerokuSfidFkResolveProgress();
  const tablesQuery = useHerokuSfidFkResolvableTables();
  const startMutation = useStartHerokuSfidFkResolve();
  const [selectedTable, setSelectedTable] = useState<string | undefined>(undefined);

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress
    ? (STATUS_TAG[progress.status] ?? UNKNOWN_STATUS_TAG)
    : STATUS_TAG.IDLE;

  return (
    <Card title="sfid FK Resolve (Heroku 전용 sfid 테이블)" style={{ marginTop: 24 }}>
      <Paragraph type="secondary">
        <Text code>safety_check_submission</Text> / <Text code>product_expiration</Text> 은{' '}
        Heroku 전용 테이블이지만 <Text code>*_sfid</Text> 값이 실제 SF Id 라, sfid resolve
        엔진(SF Stage 2)을 재사용해 FK id 컬럼을 채운다. <Text strong>SF 데이터 마이그레이션
        (employee / display_work_schedule / team_member_schedule) 선행 적재 후</Text> 실행하세요.
        SF Migration 페이지의 FK Resolve 와 동일한 진행 상태를 공유하므로, 한쪽이 실행 중이면 중복
        실행이 차단됩니다.
      </Paragraph>

      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space size="middle" wrap>
          <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
            상태: {statusTag.label}
          </Tag>
          <Button
            type="primary"
            loading={startMutation.isPending && !selectedTable}
            disabled={isRunning}
            onClick={() => startMutation.mutate(undefined)}
          >
            {isRunning ? '진행 중…' : '전체 실행'}
          </Button>
          <Select
            style={{ minWidth: 280 }}
            placeholder="테이블 선택 (단일 실행)"
            allowClear
            showSearch
            value={selectedTable}
            onChange={(v) => setSelectedTable(v)}
            loading={tablesQuery.isLoading}
            options={(tablesQuery.data ?? []).map((t) => ({ label: t, value: t }))}
            disabled={isRunning}
          />
          <Button
            loading={startMutation.isPending && !!selectedTable}
            disabled={isRunning || !selectedTable}
            onClick={() => startMutation.mutate(selectedTable)}
          >
            선택 테이블만 실행
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

      {progress && (
        <>
          <Descriptions
            column={{ xs: 1, sm: 2, md: 3 }}
            bordered
            size="small"
            style={{ marginTop: 16 }}
          >
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
              percent={sfidOverallPercent(progress)}
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

          {progress.currentTable && (
            <div style={{ marginTop: 8 }}>
              <Text>
                현재 테이블: <Text code>{progress.currentTable}</Text>
                {' '}
                ({progress.currentTableChunk}/{progress.currentTableTotalChunks} chunks)
              </Text>
              <Progress
                percent={sfidCurrentTablePercent(progress)}
                status={isRunning ? 'active' : 'normal'}
              />
            </div>
          )}

          {progress.errors.length > 0 && (
            <Alert
              type="warning"
              showIcon
              style={{ marginTop: 16 }}
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

          <div style={{ marginTop: 16 }}>
            <ResizableTable<FkResolveTableResult>
              size="small"
              rowKey="label"
              columns={sfidTableColumns}
              dataSource={progress.tableResults}
              pagination={false}
              locale={{ emptyText: '아직 완료된 테이블 없음' }}
            />
          </div>
        </>
      )}
    </Card>
  );
}

/**
 * EmployeeInfo(mobile) 초기 비밀번호 BCrypt 적재 카드.
 *
 * SF Stage 2-C (User.password) 와 **동일한 초기 평문 상수**를 backend 에서 공유하므로, web(User) /
 * mobile(EmployeeInfo) 어느 쪽으로 로그인해도 초기 비밀번호가 같다. 두 화면에서 각각 1회씩 실행해야
 * 양쪽 테이블이 모두 채워진다 (대상 테이블이 다르므로 한쪽 실행으로 대체되지 않는다).
 */
function HerokuPasswordHashCard() {
  const runMutation = useRunHerokuPasswordHash();

  const result = runMutation.data;
  const error = runMutation.error as Error | null;
  const pending = runMutation.isPending;

  return (
    <Card title="초기 비밀번호 적재 (BCrypt) — EmployeeInfo" style={{ marginTop: 24 }}>
      <Paragraph type="secondary">
        레거시 비밀번호는 이전되지 않고, <Text code>employee_info.password</Text> 를{' '}
        <Text strong>고정 초기 평문</Text> (<Text code>pwrs1234!</Text>) 의 BCrypt hash 로 새로
        발급한다. 대상은 <Text code>password IS NULL OR password = &apos;&apos;</Text> 인 row —
        이미 채워진 row 는 skip 이라 <Text strong>멱등</Text>이다.{' '}
        <Text code>password_change_required = TRUE</Text> 를 함께 설정해 최초 로그인 시 비밀번호
        변경을 강제한다.
        <br />
        SF 화면의 <Text strong>Stage 2-C (User.password)</Text> 와 동일한 초기 평문 상수를 공유하므로
        web / mobile 어느 쪽으로 로그인해도 초기 비밀번호가 같다. 다만{' '}
        <Text strong>대상 테이블이 다르므로 두 화면에서 각각 1회씩 실행</Text>해야 한다. 동기 실행 —
        row 별 개별 encode 라 사원 수에 비례해 시간이 걸린다.
      </Paragraph>

      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="런칭 공지 필요"
        description="초기 비밀번호가 전 사용자 공통 고정값이므로, 사용자에게 사번 + pwrs1234! 로 로그인 후 즉시 변경하도록 안내해야 한다."
      />

      <Space>
        <Button
          type="primary"
          loading={pending}
          disabled={pending}
          onClick={() => {
            runMutation.mutate();
          }}
        >
          실행
        </Button>
      </Space>

      {error && (
        <Alert
          type="error"
          showIcon
          style={{ marginTop: 12 }}
          message="초기 비밀번호 적재 실패"
          description={error.message}
          closable
          onClose={() => {
            runMutation.reset();
          }}
        />
      )}

      {result && (
        <div style={{ marginTop: 16 }}>
          <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
            <Descriptions.Item label="substep">
              <Text code>{result.substep}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="적용 row">
              {result.totalRowsAffected.toLocaleString()}
            </Descriptions.Item>
          </Descriptions>
          {result.totalRowsAffected === 0 && (
            <Alert
              type="warning"
              showIcon
              style={{ marginTop: 12 }}
              message="적용된 row 가 없습니다"
              description="아래 사유를 확인하세요 — Stage 1 employee_info 적재 미완료, 또는 이미 비밀번호가 채워진 상태(멱등)."
            />
          )}
          <ResizableTable<HerokuPasswordHashSubstepResult>
            style={{ marginTop: 12 }}
            size="small"
            rowKey="label"
            pagination={false}
            columns={[
              { title: '대상', dataIndex: 'label', key: 'label' },
              {
                title: '적용 row',
                dataIndex: 'rowsAffected',
                key: 'rowsAffected',
                width: 160,
                align: 'right',
                render: (v: number) => v.toLocaleString(),
              },
            ]}
            dataSource={result.results}
          />
        </div>
      )}
    </Card>
  );
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
        실행하세요. 패턴 A(자연키→serial id) + 패턴 B(부모 FK) 를 일괄 처리합니다. 하단의{' '}
        <Text strong>sfid FK Resolve</Text> 섹션은 Heroku 전용이지만 sfid 값이 SF Id 인 테이블
        (<Text code>safety_check_submission</Text> 등) 을 별도로 처리합니다.
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

      <HerokuSfidFkResolveCard />

      <HerokuPasswordHashCard />
    </div>
  );
}
