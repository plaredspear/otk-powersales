import { Alert, Modal, Space, Tag } from 'antd';
import type { AttendInfoListItem } from '@/api/attendInfo';

interface AttendInfoDeleteConfirmModalProps {
  target: AttendInfoListItem;
  loading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

function formatYyyyMmDd(value: string | null | undefined): string {
  if (!value || value.length !== 8) return value ?? '-';
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
}

export default function AttendInfoDeleteConfirmModal({
  target,
  loading,
  onConfirm,
  onCancel,
}: AttendInfoDeleteConfirmModalProps) {
  return (
    <Modal
      title="근무기간 조회 — 삭제"
      open
      onOk={onConfirm}
      okText="삭제"
      okType="danger"
      cancelText="취소"
      onCancel={onCancel}
      confirmLoading={loading}
    >
      <p>다음 근무기간 데이터를 삭제하시겠습니까?</p>
      <ul style={{ paddingLeft: 20, marginBottom: 16 }}>
        <li>
          <strong>근태정보번호:</strong> {target.name ?? '(SAP 미배포)'}
        </li>
        <li>
          <strong>사원:</strong>{' '}
          <Space size={4}>
            {target.employeeName ?? '-'} ({target.employeeCode})
            {target.employeeJobCode && (
              <Tag color="geekblue" style={{ fontSize: 11 }}>
                {target.employeeJobCode}
              </Tag>
            )}
          </Space>
        </li>
        <li>
          <strong>근태유형:</strong> {target.attendTypeName ?? '-'} ({target.attendType ?? '-'})
        </li>
        <li>
          <strong>기간:</strong> {formatYyyyMmDd(target.startDate)} ~ {formatYyyyMmDd(target.endDate)}
        </li>
      </ul>
      <Alert
        type="warning"
        showIcon
        message="삭제 시 다음이 함께 처리됩니다"
        description="연결된 여사원 연차 일정이 자동 삭제됩니다 (해당 사원·기간·연차 유형 row 전량)."
      />
    </Modal>
  );
}
