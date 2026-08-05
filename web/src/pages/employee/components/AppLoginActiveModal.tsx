import { Modal, Typography, Alert, notification } from 'antd';
import type { EmployeeDetail } from '@/api/employee';
import { useUpdateEmployeeAppLoginActive } from '@/hooks/employee/useEmployee';

const { Paragraph, Text } = Typography;

interface AppLoginActiveModalProps {
  employee: EmployeeDetail;
  open: boolean;
  onClose: () => void;
}

/**
 * 앱 로그인 활성/비활성 전용 확인 모달.
 *
 * 일반 수정([EmployeeEditModal]) 안의 「앱 로그인 활성」 스위치는 origin=SAP 사원에서 모달 자체가
 * 열리지 않아 도달 불가였다. 운영 사원은 전량 origin=SAP 이므로 사실상 수동 활성화 수단이 없었고,
 * 비활성 사원은 비밀번호/단말 초기화 버튼까지 잠겨 구제 경로가 없었다. 이 모달은 그 사각지대를
 * 여는 단일 축 전용 경로다 (권한 변경 모달과 동일한 분리 패턴).
 */
export default function AppLoginActiveModal({
  employee,
  open,
  onClose,
}: AppLoginActiveModalProps) {
  const mutation = useUpdateEmployeeAppLoginActive();
  const currentActive = employee.appLoginActive === true;
  const nextActive = !currentActive;

  const handleConfirm = async () => {
    try {
      const updated = await mutation.mutateAsync({
        employeeId: employee.id,
        appLoginActive: nextActive,
      });
      // 서버가 현장 여사원 보호 규칙을 적용해 요청값을 되돌릴 수 있다 — 결과값 기준으로 안내한다.
      if (updated.appLoginActive === nextActive) {
        notification.success({
          message: nextActive ? '앱 로그인이 활성화되었습니다' : '앱 로그인이 비활성화되었습니다',
        });
      } else {
        notification.warning({
          message: '앱 로그인 활성 상태가 유지되었습니다',
          description:
            '현장 여사원 직군(판촉/레이디/OSC) 재직자는 보호 규칙에 따라 앱 로그인을 비활성화할 수 없습니다.',
        });
      }
      onClose();
    } catch (err) {
      notification.error({
        message: '앱 로그인 활성 변경 실패',
        description: (err as Error)?.message ?? '잠시 후 다시 시도해 주세요.',
      });
    }
  };

  return (
    <Modal
      title={nextActive ? '앱 로그인 활성화' : '앱 로그인 비활성화'}
      open={open}
      onOk={handleConfirm}
      onCancel={onClose}
      okText="확인"
      cancelText="취소"
      okButtonProps={{ danger: !nextActive }}
      confirmLoading={mutation.isPending}
      destroyOnHidden
    >
      <Paragraph>
        {nextActive
          ? '이 사원의 모바일 앱 로그인을 활성화하시겠습니까?'
          : '이 사원의 모바일 앱 로그인을 비활성화하시겠습니까? 즉시 로그인이 차단됩니다.'}
      </Paragraph>
      <Paragraph>
        <Text strong>사번:</Text> {employee.employeeCode}
        <br />
        <Text strong>이름:</Text> {employee.name}
        <br />
        <Text strong>현재 상태:</Text> {currentActive ? '활성' : '비활성'}
      </Paragraph>
      <Alert
        type="info"
        showIcon
        message="SAP 원천 사원도 변경할 수 있습니다"
        description="앱 로그인 활성은 SAP 의 시스템 접근 잠금(LockingFlag) 과 한 축이라, 저장 시 잠금 플래그도 함께 반대값으로 맞춰집니다. 다만 다음 SAP 인사 인입이 들어오면 SAP 값으로 다시 덮어써지므로, 이 변경은 인입 사이의 수동 조치입니다."
      />
    </Modal>
  );
}
