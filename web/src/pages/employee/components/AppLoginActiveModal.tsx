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
 *
 * 레거시 SF 는 사원 레코드 상세 "현장사원 설정" 섹션에서 `APP로그인활성` 만 편집 가능,
 * `시스템접근 플래그(LockingFlag)` 는 읽기 전용으로 두었다. 본 모달도 동일하게 앱 로그인 축만
 * 변경하며, 잠긴 사원은 활성화가 적용되지 않는다(서버 정책이 되돌림).
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
      // 서버가 잠금 정책을 적용해 요청값을 되돌릴 수 있다 — 결과값 기준으로 사유까지 안내한다.
      if (updated.appLoginActive === nextActive) {
        notification.success({
          message: nextActive ? '앱 로그인이 활성화되었습니다' : '앱 로그인이 비활성화되었습니다',
        });
      } else if (nextActive) {
        notification.warning({
          message: '앱 로그인을 활성화할 수 없습니다',
          description:
            '시스템 접근 잠금(SAP LockingFlag) 상태의 사원입니다. SAP 인사에서 잠금이 해제되어야 활성화됩니다.',
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
      {nextActive && employee.lockingFlag === true && (
        <Alert
          type="warning"
          showIcon
          message="시스템 접근 잠금 상태입니다"
          description="SAP 인사에서 잠금(LockingFlag)이 걸린 사원이라 활성화가 적용되지 않습니다. 현장 여사원 직군(판촉/레이디/OSC) 재직자만 예외로 활성 복원됩니다. 먼저 SAP 잠금 해제가 필요합니다."
          style={{ marginBottom: 12 }}
        />
      )}
      <Alert
        type="info"
        showIcon
        message="SAP 원천 사원도 변경할 수 있습니다"
        description="변경 대상은 앱 로그인 활성 한 축뿐이며, 시스템 접근 잠금(LockingFlag)은 SAP 원천이라 건드리지 않습니다(레거시 SF 레이아웃과 동일). 다음 SAP 인사 인입이 들어오면 SAP 잠금 값 기준으로 다시 덮어써지므로, 이 변경은 인입 사이의 수동 조치입니다."
      />
    </Modal>
  );
}
