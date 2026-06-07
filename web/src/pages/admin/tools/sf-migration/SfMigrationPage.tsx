import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Progress,
  Radio,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useFkResolveProgress,
  useRunNaturalKeyFkResolve,
  useRunNoticeRtaPlaceholder,
  useRunPicklistColumn,
  useRunUploadFilePolymorphicParent,
  useRunUserRoleHierarchyRecalc,
  useStartFkResolve,
} from '@/hooks/admin/useSfMigration';
import type {
  FkResolveProgress,
  FkResolveStatus,
  FkResolveTableResult,
  NaturalKeyFkSubstepResult,
  NoticeRtaPlaceholderSubstepResult,
  PicklistSubstepResult,
  UploadFileParentSubstepResult,
} from '@/api/admin/sfMigration';
import ResizableTable from '@/components/common/ResizableTable';

const { Title, Paragraph, Text } = Typography;

const STATUS_TAG: Record<FkResolveStatus, { color: string; label: string }> = {
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
  const runPicklistColumnMutation = useRunPicklistColumn();
  const runNaturalKeyFkMutation = useRunNaturalKeyFkResolve();
  const runUploadFileParentMutation = useRunUploadFilePolymorphicParent();
  const runHierarchyRecalcMutation = useRunUserRoleHierarchyRecalc();
  const runNoticeRtaMutation = useRunNoticeRtaPlaceholder();

  // 공지 본문 placeholder 치환은 비가역 UPDATE — 기본 dry-run, apply 명시 선택 시에만 실제 변경.
  const [noticeRtaApply, setNoticeRtaApply] = useState(false);

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress
    ? (STATUS_TAG[progress.status] ?? UNKNOWN_STATUS_TAG)
    : STATUS_TAG.IDLE;

  const picklistResult = runPicklistColumnMutation.data;
  const picklistError = runPicklistColumnMutation.error as Error | null;
  const picklistPending = runPicklistColumnMutation.isPending;

  const naturalKeyResult = runNaturalKeyFkMutation.data;
  const naturalKeyError = runNaturalKeyFkMutation.error as Error | null;
  const naturalKeyPending = runNaturalKeyFkMutation.isPending;

  const uploadFileParentResult = runUploadFileParentMutation.data;
  const uploadFileParentError = runUploadFileParentMutation.error as Error | null;
  const uploadFileParentPending = runUploadFileParentMutation.isPending;

  const hierarchyResult = runHierarchyRecalcMutation.data;
  const hierarchyError = runHierarchyRecalcMutation.error as Error | null;
  const hierarchyPending = runHierarchyRecalcMutation.isPending;

  const noticeRtaResult = runNoticeRtaMutation.data;
  const noticeRtaError = runNoticeRtaMutation.error as Error | null;
  const noticeRtaPending = runNoticeRtaMutation.isPending;

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
            <ResizableTable<FkResolveTableResult>
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

      <Card title="Natural Key FK 해소" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          sfid prefix 가 아닌 자연 키 (developer_name / name / 외부 sfid 컬럼) 기반으로
          FK id 컬럼을 채운다. <Text code>permission_set_flags</Text> /{' '}
          <Text code>permission_set_assignment</Text> / <Text code>profile_flags</Text> /{' '}
          <Text code>sharing_rule_target</Text> 등의 권한 평탄화에 필요한 FK 가 여기서
          연결된다. <Text strong>위 FK Resolve 가 완료된 후</Text> 1회 실행. 동기 실행 —
          보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={naturalKeyPending}
            disabled={naturalKeyPending}
            onClick={() => {
              runNaturalKeyFkMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {naturalKeyError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="Natural Key FK 해소 실패"
            description={naturalKeyError.message}
            closable
            onClose={() => {
              runNaturalKeyFkMutation.reset();
            }}
          />
        )}

        {naturalKeyResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{naturalKeyResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="누적 적용 row">
                {naturalKeyResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<NaturalKeyFkSubstepResult>
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
              dataSource={naturalKeyResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="UploadFile Parent Resolve" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          Stage1 이 적재한 <Text code>object_type</Text> (SF Object__c 원본) 기준으로{' '}
          <Text code>parent_type</Text> 을 파생하고, <Text code>(parent_type, record_sfid)</Text>{' '}
          → <Text code>parent_id</Text> (Long FK) 를 채운다.{' '}
          <Text code>claim</Text> / <Text code>notice</Text> / <Text code>proposal</Text> /{' '}
          <Text code>site_activity</Text> 4종 polymorphic. <Text code>record_sfid</Text> 는 일반 FK
          Resolve 가 건드리지 않으므로, <Text strong>Stage1 적재 완료 후</Text> 본 substep 을 1회
          실행해야 첨부 이미지 조회가 연결된다. 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={uploadFileParentPending}
            disabled={uploadFileParentPending}
            onClick={() => {
              runUploadFileParentMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {uploadFileParentError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="UploadFile Parent Resolve 실패"
            description={uploadFileParentError.message}
            closable
            onClose={() => {
              runUploadFileParentMutation.reset();
            }}
          />
        )}

        {uploadFileParentResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{uploadFileParentResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="누적 적용 row">
                {uploadFileParentResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<UploadFileParentSubstepResult>
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
              dataSource={uploadFileParentResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="공지 본문 이미지 placeholder 치환" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          공지 본문(<Text code>notice.contents</Text>)에 박힌 SF rtaImage 서블릿 URL{' '}
          <Text code>&lt;img&gt;</Text> 태그를 만료 없는 placeholder{' '}
          <Text code>&lt;img src="notice-image://{'{refid}'}" data-refid="{'{refid}'}"&gt;</Text> 로
          치환한다. 조회 시점에 백엔드가 <Text code>data-refid</Text> 로 presigned URL 을 rewrite
          하므로 본문엔 placeholder 만 영구 저장된다. <Text strong>공지 본문 이미지 적재(Stage1
          NoticeImageUploadFile) + UploadFile Parent Resolve 완료 후</Text> 1회 실행. 멱등 — 이미
          치환된 본문은 skip. <Text strong>비가역 UPDATE</Text> 이므로 먼저 dry-run 으로 변경 규모를
          확인한 뒤 apply 한다.
        </Paragraph>

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Radio.Group
            optionType="button"
            buttonStyle="solid"
            value={noticeRtaApply ? 'apply' : 'dry-run'}
            onChange={(e) => setNoticeRtaApply(e.target.value === 'apply')}
            disabled={noticeRtaPending}
          >
            <Radio.Button value="dry-run">Dry-run (변경 대상만 집계)</Radio.Button>
            <Radio.Button value="apply">Apply (실제 UPDATE)</Radio.Button>
          </Radio.Group>

          <Space>
            <Button
              type="primary"
              danger={noticeRtaApply}
              loading={noticeRtaPending}
              disabled={noticeRtaPending}
              onClick={() => {
                runNoticeRtaMutation.mutate(!noticeRtaApply);
              }}
            >
              {noticeRtaApply ? '실행 (Apply)' : '실행 (Dry-run)'}
            </Button>
          </Space>
        </Space>

        {noticeRtaError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="공지 본문 placeholder 치환 실패"
            description={noticeRtaError.message}
            closable
            onClose={() => {
              runNoticeRtaMutation.reset();
            }}
          />
        )}

        {noticeRtaResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{noticeRtaResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="치환 이미지 수">
                {noticeRtaResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<NoticeRtaPlaceholderSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '항목', dataIndex: 'label', key: 'label' },
                {
                  title: '건수',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={noticeRtaResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="UserRole Hierarchy 재계산" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          <Text code>user_role_hierarchy_snapshot</Text> 의{' '}
          <Text code>all_subordinate_ids</Text> / <Text code>depth</Text> /{' '}
          <Text code>ancestor_path</Text> / <Text code>snapshot_at</Text> 을{' '}
          <Text code>user_role.parent_user_role_id</Text> 트리 기반으로 재계산.{' '}
          <Text strong>위 Natural Key FK 해소가 완료된 후</Text> 1회 실행 — 미실행 시{' '}
          <Text code>depth</Text> NULL 인 row 가 권한 평가 path 에서 Hibernate primitive
          setter 호출 시점에 NPE 유발 (예: <Text code>/api/v1/admin/accounts</Text>{' '}
          500). 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={hierarchyPending}
            disabled={hierarchyPending}
            onClick={() => {
              runHierarchyRecalcMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {hierarchyError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="UserRole Hierarchy 재계산 실패"
            description={hierarchyError.message}
            closable
            onClose={() => {
              runHierarchyRecalcMutation.reset();
            }}
          />
        )}

        {hierarchyResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{hierarchyResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="결과">
                <Text type="success">snapshot 재계산 완료 (상세 row 수는 서버 logger 참조)</Text>
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Card>

      <Card title="Stage 2-B — Derived 캐시 동기화" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          User.cost_center_code derived 캐시를 Employee.cost_center_code 기준으로 동기화한다.
          한글 picklist → enum 변환 substep (Employee.role / Employee.professional_promotion_team /
          User.profile_type) 은 폐기 — SF picklist value 가 곧 저장값이라 변환 자체가 불요.
          본 작업은 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            대상: <Text code>User.cost_center_code</Text> (Employee 캐시 동기화)
          </Paragraph>
          <Space>
            <Button
              type="primary"
              loading={picklistPending}
              disabled={picklistPending}
              onClick={() => {
                runPicklistColumnMutation.mutate('user_cost_center_code');
              }}
            >
              실행
            </Button>
          </Space>
        </Space>

        {picklistError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="Derived 캐시 동기화 실패"
            description={picklistError.message}
            closable
            onClose={() => {
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
            <ResizableTable<PicklistSubstepResult>
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
              dataSource={picklistResult.results}
            />
          </div>
        )}
      </Card>
    </div>
  );
}
