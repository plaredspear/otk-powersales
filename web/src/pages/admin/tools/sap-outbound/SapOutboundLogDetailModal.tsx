import { Descriptions, Modal, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import type {
  SapOutboundLogRow,
  SapOutboundResultCode,
} from '@/api/admin/sapIntegration';
import { useSapOutboundLogDetail } from '@/hooks/admin/useSapOutbound';

const { Paragraph } = Typography;

const RESULT_TAG_COLOR: Record<SapOutboundResultCode, string> = {
  SUCCESS: 'green',
  FAIL: 'red',
  INVALID_RESPONSE: 'orange',
};

interface Props {
  row: SapOutboundLogRow | null;
  open: boolean;
  onClose: () => void;
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

export default function SapOutboundLogDetailModal({ row, open, onClose }: Props) {
  const detailQuery = useSapOutboundLogDetail(row?.id ?? 0, open && row !== null);

  if (!row) return null;

  const detail = detailQuery.data ?? row;
  const errorDetail = detailQuery.data?.errorDetail ?? null;

  return (
    <Modal
      title="SAP 아웃바운드 호출 이력 상세"
      open={open}
      onCancel={onClose}
      onOk={onClose}
      footer={null}
      width={900}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="ID">{detail.id}</Descriptions.Item>
        <Descriptions.Item label="Interface ID">
          <code>{detail.interfaceId}</code>
        </Descriptions.Item>
        <Descriptions.Item label="Endpoint Path">
          <code>{detail.endpointPath}</code>
        </Descriptions.Item>
        <Descriptions.Item label="Request Count">{detail.requestCount}</Descriptions.Item>
        <Descriptions.Item label="HTTP Status">{detail.httpStatus ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Result Code">
          {detail.resultCode ? (
            <Tag color={RESULT_TAG_COLOR[detail.resultCode] ?? 'default'}>{detail.resultCode}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="Result Msg">
          {detail.resultMsg ? (
            <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{detail.resultMsg}</Paragraph>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="시도 횟수">{detail.attemptCount}</Descriptions.Item>
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
