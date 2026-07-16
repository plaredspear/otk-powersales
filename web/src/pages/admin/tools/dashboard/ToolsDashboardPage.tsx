import { Alert, Card, Select, Space, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchLogLevels,
  updateLogLevel,
  type LoggerLevel,
  type LoggerListResponse,
} from '@/api/admin/logLevel';
import DailyScheduleStatusCard from './DailyScheduleStatusCard';
import FeatureToggleSection from './FeatureToggleSection';

const { Title, Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'tools', 'log-levels'] as const;

/** 상속(명시 레벨 없음)을 표현하는 Select 옵션 값. null 로 매핑. */
const INHERIT_VALUE = '__INHERIT__';

/** 레벨별 태그 색상 — 부하/위험도가 높을수록 강조. */
const LEVEL_COLOR: Record<string, string> = {
  OFF: 'default',
  ERROR: 'red',
  WARN: 'orange',
  INFO: 'blue',
  DEBUG: 'purple',
  TRACE: 'magenta',
};

/**
 * 개발자 도구 - 대시보드 > 런타임 로그 레벨 관리.
 *
 * Spring LoggingSystem 을 통해 운영 중 로그 레벨을 임시로 조정한다. 변경은 메모리상 임시
 * 조정으로 앱 재시작 시 서버 기본값(운영=INFO)으로 복귀한다. 시스템 관리자 전용
 * (백엔드 가드) — 비관리자는 API 403 으로 차단된다.
 */
function LogLevelSection() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery<LoggerListResponse>({
    queryKey: QUERY_KEY,
    queryFn: fetchLogLevels,
  });

  const updateMutation = useMutation({
    mutationFn: ({ loggerName, level }: { loggerName: string; level: string | null }) =>
      updateLogLevel(loggerName, level),
    onSuccess: (result: LoggerLevel) => {
      message.success(
        `${result.name} → ${result.effectiveLevel ?? '(상속)'} 로 변경되었습니다`,
      );
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: Error) => {
      message.error(err.message || '로그 레벨 변경에 실패했습니다');
    },
  });

  const availableLevels = data?.availableLevels ?? [];

  const levelOptions = [
    { label: '상속 (기본값으로 복귀)', value: INHERIT_VALUE },
    ...availableLevels.map((lv) => ({ label: lv, value: lv })),
  ];

  const columns: ColumnsType<LoggerLevel> = [
    {
      title: '로거',
      dataIndex: 'name',
      key: 'name',
      width: 360,
      render: (name: string) =>
        name === 'ROOT' ? <Text strong>ROOT</Text> : <Text code>{name}</Text>,
    },
    {
      title: '현재 적용 레벨',
      dataIndex: 'effectiveLevel',
      key: 'effectiveLevel',
      width: 140,
      render: (level: string | null) =>
        level ? <Tag color={LEVEL_COLOR[level] ?? 'default'}>{level}</Tag> : <Text type="secondary">-</Text>,
    },
    {
      title: '명시 설정',
      dataIndex: 'configuredLevel',
      key: 'configuredLevel',
      width: 120,
      render: (level: string | null) =>
        level ? <Tag>{level}</Tag> : <Text type="secondary">상속</Text>,
    },
    {
      title: '레벨 변경',
      key: 'action',
      width: 220,
      render: (_: unknown, record: LoggerLevel) => (
        <Select
          style={{ width: 200 }}
          value={record.configuredLevel ?? INHERIT_VALUE}
          options={levelOptions}
          loading={updateMutation.isPending}
          onChange={(value: string) =>
            updateMutation.mutate({
              loggerName: record.name,
              level: value === INHERIT_VALUE ? null : value,
            })
          }
        />
      ),
    },
  ];

  return (
    <>
      <Paragraph type="secondary">
        운영 중 로그 레벨을 임시로 조정합니다. 진단이 필요할 때만 DEBUG 로 올리고, 확인 후 반드시
        원래 레벨(INFO)로 되돌리세요. DEBUG/TRACE 는 CPU·메모리·로그를 크게 늘리며, SQL 바인딩
        레벨은 개인정보가 로그에 남을 수 있습니다.
      </Paragraph>

      <Alert
        type="info"
        style={{ marginBottom: 16 }}
        message="변경은 메모리상 임시 조정입니다. 앱 재시작·재배포 시 서버 기본값(운영=INFO)으로 자동 복귀합니다."
      />

      {isError && (
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message="로그 레벨 조회에 실패했습니다. 시스템 관리자 권한이 필요합니다."
        />
      )}

      <Card size="small">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <ResizableTable<LoggerLevel>
            rowKey="name"
            columns={columns}
            dataSource={data?.loggers ?? []}
            loading={isLoading}
            pagination={false}
            locale={listTableLocale()}
          />
        </Space>
      </Card>
    </>
  );
}

/**
 * 개발자 도구 - 대시보드. 일별 스케줄 실행현황과 로그 레벨 관리를 탭으로 구분한다.
 */
export default function ToolsDashboardPage() {
  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>대시보드</Title>

      <Tabs
        defaultActiveKey="schedule"
        items={[
          {
            key: 'schedule',
            label: '일별 스케줄 실행현황',
            children: <DailyScheduleStatusCard />,
          },
          {
            key: 'feature-toggle',
            label: '기능 활성화',
            children: <FeatureToggleSection />,
          },
          {
            key: 'log-level',
            label: '로그 레벨 관리',
            children: <LogLevelSection />,
          },
        ]}
      />
    </div>
  );
}
