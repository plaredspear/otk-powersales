import { Modal, message } from 'antd';
import type { PromotionScheduleItem, PromotionScheduleMember } from '@/api/promotionSchedule';
import { useBulkDeletePromotionSchedules } from '@/hooks/promotion/usePromotionSchedules';

interface SelectedRow {
  member: PromotionScheduleMember;
  schedule: PromotionScheduleItem;
}

interface Props {
  open: boolean;
  promotionId: number;
  selected: SelectedRow[];
  onClose: () => void;
  onSuccess: () => void;
}

const MAX_DISPLAY = 5;

export default function PromotionScheduleBulkDeleteDialog({
  open,
  promotionId,
  selected,
  onClose,
  onSuccess,
}: Props) {
  const bulkDelete = useBulkDeletePromotionSchedules();

  const handleOk = async () => {
    try {
      const result = await bulkDelete.mutateAsync({
        promotionId,
        data: { schedule_ids: selected.map((s) => s.schedule.scheduleId) },
      });
      message.success(`${result.deletedCount}건 삭제 완료`);
      onSuccess();
      onClose();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '일괄 삭제에 실패했습니다');
    }
  };

  const displayed = selected.slice(0, MAX_DISPLAY);
  const remaining = selected.length - displayed.length;

  return (
    <Modal
      title={`선택한 ${selected.length}건의 일정을 삭제하시겠습니까?`}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={bulkDelete.isPending}
      okText="삭제"
      cancelText="취소"
      okButtonProps={{ danger: true }}
    >
      <ul style={{ margin: 0, paddingLeft: 16 }}>
        {displayed.map(({ member, schedule }) => (
          <li key={schedule.scheduleId}>
            {member.employeeName} / {schedule.workingDate} / {schedule.accountName}
          </li>
        ))}
        {remaining > 0 && <li>외 {remaining}건</li>}
      </ul>
    </Modal>
  );
}
