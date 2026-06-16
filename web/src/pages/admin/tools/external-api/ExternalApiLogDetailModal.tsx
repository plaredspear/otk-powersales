import { Descriptions, Modal, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import type { ExternalApiLogRow } from '@/api/admin/externalApiLog';
import { useExternalApiLogDetail } from '@/hooks/admin/useExternalApiLog';

const { Paragraph } = Typography;

interface Props {
  row: ExternalApiLogRow | null;
  open: boolean;
  onClose: () => void;
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function targetColor(target: string): string {
  if (target === 'SAP') return 'blue';
  if (target === 'SF') return 'cyan';
  return 'geekblue';
}

export default function ExternalApiLogDetailModal({ row, open, onClose }: Props) {
  const detailQuery = useExternalApiLogDetail(row?.id ?? 0, open && row !== null);

  if (!row) return null;

  const detail = detailQuery.data ?? row;
  const errorDetail = detailQuery.data?.errorDetail ?? null;

  return (
    <Modal
      title="외부 API 호출 이력 상세"
      open={open}
      onCancel={onClose}
      onOk={onClose}
      footer={null}
      width={900}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="ID">{detail.id}</Descriptions.Item>
        <Descriptions.Item label="Target System">
          <Tag color={targetColor(detail.targetSystem)}>{detail.targetSystem}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Endpoint Key">
          {detail.endpointKey ? <code>{detail.endpointKey}</code> : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="HTTP Method">
          <code>{detail.httpMethod}</code>
        </Descriptions.Item>
        <Descriptions.Item label="URI">
          <Paragraph style={{ margin: 0, wordBreak: 'break-all' }}>
            <code>{detail.uri}</code>
          </Paragraph>
        </Descriptions.Item>
        <Descriptions.Item label="HTTP Status">{detail.httpStatus ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="결과">
          <Tag color={detail.success ? 'green' : 'red'}>{detail.success ? 'SUCCESS' : 'FAIL'}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="소요시간 (ms)">{detail.durationMs.toLocaleString()}</Descriptions.Item>
        <Descriptions.Item label="요청 시각">{formatDateTime(detail.requestedAt)}</Descriptions.Item>
        <Descriptions.Item label="완료 시각">{formatDateTime(detail.completedAt)}</Descriptions.Item>
        <Descriptions.Item label="Error Detail">
          {errorDetail ? (
            <Paragraph
              copyable={{ text: errorDetail }}
              style={{
                margin: 0,
                fontFamily: 'monospace',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
                maxHeight: 320,
                overflowY: 'auto',
              }}
            >
              {errorDetail}
            </Paragraph>
          ) : (
            '-'
          )}
        </Descriptions.Item>
      </Descriptions>
    </Modal>
  );
}
