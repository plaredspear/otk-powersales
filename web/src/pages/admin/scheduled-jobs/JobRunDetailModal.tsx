import { Descriptions, Modal, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import type {
  ScheduledJobRun,
  ScheduledJobStatus,
} from '@/api/admin/scheduledJob';

const { Paragraph } = Typography;

const STATUS_TAG_COLOR: Record<ScheduledJobStatus, string> = {
  SUCCESS: 'green',
  FAILURE: 'red',
  RUNNING: 'blue',
  SKIPPED: 'default',
};

interface Props {
  run: ScheduledJobRun | null;
  open: boolean;
  onClose: () => void;
}

function formatDateTime(value: string | null): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function prettyJson(raw: string | null): string {
  if (!raw) return '-';
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

export default function JobRunDetailModal({ run, open, onClose }: Props) {
  if (!run) return null;

  return (
    <Modal
      title="실행 이력 상세"
      open={open}
      onCancel={onClose}
      onOk={onClose}
      footer={null}
      width={720}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="ID">{run.id}</Descriptions.Item>
        <Descriptions.Item label="잡 이름">{run.jobName}</Descriptions.Item>
        <Descriptions.Item label="상태">
          <Tag color={STATUS_TAG_COLOR[run.status]}>{run.status}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="시작 시각">{formatDateTime(run.startedAt)}</Descriptions.Item>
        <Descriptions.Item label="종료 시각">{formatDateTime(run.endedAt)}</Descriptions.Item>
        <Descriptions.Item label="소요시간 (ms)">{run.durationMs ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="오류 메시지">
          {run.errorMessage ? (
            <Paragraph style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{run.errorMessage}</Paragraph>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="metadata">
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
            {prettyJson(run.metadata)}
          </pre>
        </Descriptions.Item>
      </Descriptions>
    </Modal>
  );
}
