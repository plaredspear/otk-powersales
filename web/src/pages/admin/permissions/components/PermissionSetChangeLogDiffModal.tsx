import { Modal, Row, Col, Typography } from 'antd';
import type { PermissionSetChangeLogEntry } from '@/api/admin/permission';

const { Text } = Typography;

interface Props {
  open: boolean;
  entry: PermissionSetChangeLogEntry | null;
  onClose: () => void;
}

/**
 * Spec #837 — 변경 이력 단건 diff 표시 모달.
 *
 * before/after JSON 본문을 좌/우 split 으로 표시. 본 MVP 는 별도 diff highlight 없이 raw JSON
 * pretty-print 만 — UI 운영자가 시각적으로 비교. 향후 react-diff-viewer 도입 후속 보강 후보.
 */
export default function PermissionSetChangeLogDiffModal({ open, entry, onClose }: Props) {
  const prettyPrint = (raw: string | null): string => {
    if (!raw) return '(없음)';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  };

  return (
    <Modal
      open={open}
      title={entry ? `변경 이력 — ${entry.eventType} (${entry.changedAt})` : '변경 이력'}
      onCancel={onClose}
      footer={null}
      width={960}
    >
      {entry && (
        <Row gutter={16}>
          <Col span={12}>
            <Text strong>변경 전</Text>
            <pre
              style={{
                background: '#fafafa',
                padding: 12,
                marginTop: 8,
                maxHeight: 480,
                overflow: 'auto',
                fontSize: 12,
                lineHeight: 1.4,
              }}
            >
              {prettyPrint(entry.beforeSnapshot)}
            </pre>
          </Col>
          <Col span={12}>
            <Text strong>변경 후</Text>
            <pre
              style={{
                background: '#fafafa',
                padding: 12,
                marginTop: 8,
                maxHeight: 480,
                overflow: 'auto',
                fontSize: 12,
                lineHeight: 1.4,
              }}
            >
              {prettyPrint(entry.afterSnapshot)}
            </pre>
          </Col>
        </Row>
      )}
    </Modal>
  );
}
