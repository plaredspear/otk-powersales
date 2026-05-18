import { Descriptions, Modal, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import type { SapInboundAuditRow } from '@/api/admin/sapIntegration';

const { Paragraph } = Typography;

const EVENT_TAG_COLOR: Record<string, string> = {
  REQUEST_ACCEPTED: 'green',
  REQUEST_REJECTED_AUTH: 'red',
  REQUEST_REJECTED_SCOPE: 'red',
  REQUEST_REJECTED_IP: 'red',
  REQUEST_REJECTED_SANITY: 'red',
  TOKEN_ISSUED: 'blue',
  TOKEN_REJECTED: 'red',
  SCHEDULE_CONVERSION: 'cyan',
  SCHEDULE_CONVERSION_FAILED: 'orange',
  MANUAL_ORIGIN_PROTECTED: 'gold',
};

interface Props {
  row: SapInboundAuditRow | null;
  open: boolean;
  onClose: () => void;
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

export default function SapInboundAuditDetailModal({ row, open, onClose }: Props) {
  if (!row) return null;

  return (
    <Modal
      title="SAP 인바운드 호출 이력 상세"
      open={open}
      onCancel={onClose}
      onOk={onClose}
      footer={null}
      width={720}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="ID">{row.id}</Descriptions.Item>
        <Descriptions.Item label="Event Type">
          <Tag color={EVENT_TAG_COLOR[row.eventType] ?? 'default'}>{row.eventType}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Client ID">{row.clientId ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Endpoint">
          <code>{row.endpoint ?? '-'}</code>
        </Descriptions.Item>
        <Descriptions.Item label="HTTP Method">{row.httpMethod ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Client IP">{row.clientIp}</Descriptions.Item>
        <Descriptions.Item label="Scope">
          <Paragraph copyable={row.scope ? { text: row.scope } : false} style={{ margin: 0 }}>
            {row.scope ?? '-'}
          </Paragraph>
        </Descriptions.Item>
        <Descriptions.Item label="수신 건수">{row.receivedCount ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="이전 건수">{row.previousCount ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Reason">
          {row.reason ? (
            <Paragraph
              copyable={{ text: row.reason }}
              style={{ whiteSpace: 'pre-wrap', margin: 0 }}
            >
              {row.reason}
            </Paragraph>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="발생 시각">{formatDateTime(row.createdAt)}</Descriptions.Item>
      </Descriptions>
    </Modal>
  );
}
