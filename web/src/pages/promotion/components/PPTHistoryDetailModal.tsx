import { Modal, Descriptions, Tag } from 'antd';
import dayjs from 'dayjs';
import { getPPTTeamTypeColor } from '@/constants/pptTeamType';
import type { PPTHistory } from '@/api/pptMaster';

interface Props {
  open: boolean;
  history: PPTHistory | null;
  onClose: () => void;
}

function renderTeamType(value: string | null) {
  if (value == null) return '-';
  return <Tag color={getPPTTeamTypeColor(value)}>{value}</Tag>;
}

export default function PPTHistoryDetailModal({ open, history, onClose }: Props) {
  return (
    <Modal
      title="전문행사조 이력 상세"
      open={open}
      onCancel={onClose}
      onOk={onClose}
      okText="닫기"
      cancelButtonProps={{ style: { display: 'none' } }}
      width={600}
    >
      {history && (
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="변경 시각">
            {dayjs(history.changedAt).format('YYYY-MM-DD HH:mm')}
          </Descriptions.Item>
          <Descriptions.Item label="사번">{history.employeeCode ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="사원명">{history.employeeName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="소속">{history.orgName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="재직상태">{history.status ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="변경 전">{renderTeamType(history.oldValue)}</Descriptions.Item>
          <Descriptions.Item label="변경 후">{renderTeamType(history.newValue)}</Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  );
}
