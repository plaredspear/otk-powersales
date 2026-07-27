import { Modal, Typography, notification } from 'antd';
import type { EmployeeDetail } from '@/api/employee';
import { useConfirmPostponedAppointment } from '@/hooks/employee/useEmployee';

const { Paragraph, Text } = Typography;

interface AppointmentConfirmModalProps {
  employee: EmployeeDetail;
  open: boolean;
  onClose: () => void;
}

/**
 * 발령정보 승인 확인 모달 — 레거시 SF Quick Action "신규발령확정"
 * (ManualConfirmPostponedAppQuickAction Aura 모달) 동등. 안내 문구는 SF 원문 유지.
 */
export default function AppointmentConfirmModal({
  employee,
  open,
  onClose,
}: AppointmentConfirmModalProps) {
  const mutation = useConfirmPostponedAppointment();
  const reservation = employee.postponedAppointment;

  const handleConfirm = async () => {
    try {
      await mutation.mutateAsync({ employeeId: employee.id });
      notification.success({
        message: '유예된 발령정보가 반영되었습니다.',
      });
      onClose();
    } catch (err) {
      notification.error({
        message: '발령정보 승인 실패',
        description: (err as Error)?.message ?? '잠시 후 다시 시도해 주세요.',
      });
    }
  };

  return (
    <Modal
      title="발령정보 승인"
      open={open}
      onOk={handleConfirm}
      onCancel={onClose}
      okText="확인"
      cancelText="취소"
      confirmLoading={mutation.isPending}
      destroyOnHidden
    >
      <Paragraph>예정되어있는 유예된 발령정보를 즉시 반영하시겠습니까?</Paragraph>
      <Paragraph>
        <Text strong>사번:</Text> {employee.employeeCode}
        <br />
        <Text strong>이름:</Text> {employee.name}
        {reservation && (
          <>
            <br />
            <Text strong>발령일:</Text> {reservation.appointDate ?? '-'}
            <br />
            <Text strong>발령 조직:</Text> {reservation.afterOrgName ?? '-'}
            <br />
            <Text strong>발령명:</Text> {reservation.ordDetailNode ?? '-'}
          </>
        )}
      </Paragraph>
      <Paragraph type="secondary">
        발령 예정일과 무관하게 즉시 반영되며, 반영 후 유예 표시는 해제됩니다.
      </Paragraph>
    </Modal>
  );
}
