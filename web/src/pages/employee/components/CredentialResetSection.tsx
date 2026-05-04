import { useState } from 'react';
import { Alert, Button, Space, Tooltip, Typography } from 'antd';
import { usePermission } from '@/hooks/usePermission';
import type { Employee } from '@/api/employee';
import DeviceResetModal from './DeviceResetModal';
import PasswordResetModal from './PasswordResetModal';

const { Title } = Typography;

interface CredentialResetSectionProps {
  employee: Employee;
}

const RESET_PERMISSION = 'EMPLOYEE_RESET_CREDENTIALS';

const DEVICE_TOOLTIP =
  '단말 바인딩(deviceUuid)이 해제됩니다. 사원이 다음에 어떤 단말로 로그인하더라도 새 단말로 자동 등록됩니다.';
const PASSWORD_TOOLTIP =
  "임시 비밀번호 '1234' 로 초기화됩니다. 사원은 다음 로그인 시 비밀번호 변경을 요구받습니다.";

/**
 * 사원 자격 정보 (단말 / 비밀번호) 운영자 리셋 영역 (Spec #582 P2-W).
 *
 * - SYSTEM_ADMIN 권한(`EMPLOYEE_RESET_CREDENTIALS`) 미보유 시 영역 자체를 렌더링하지 않음.
 * - 대상 사원 `appLoginActive=false` 일 경우 안내 문구 + 버튼 비활성화.
 */
export default function CredentialResetSection({ employee }: CredentialResetSectionProps) {
  const { hasPermission } = usePermission();
  const [deviceModalOpen, setDeviceModalOpen] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);

  if (!hasPermission(RESET_PERMISSION)) return null;

  const inactive = employee.appLoginActive !== true;

  return (
    <div style={{ marginTop: 16 }}>
      <Title level={5} style={{ marginBottom: 12 }}>
        계정 관리
      </Title>
      {inactive && (
        <Alert
          type="warning"
          showIcon
          message="앱 로그인이 비활성화된 사원입니다. 사원 정보를 먼저 활성화해 주세요."
          style={{ marginBottom: 12 }}
        />
      )}
      <Space>
        <Tooltip title={DEVICE_TOOLTIP}>
          <Button
            danger
            disabled={inactive}
            onClick={() => setDeviceModalOpen(true)}
          >
            단말 초기화
          </Button>
        </Tooltip>
        <Tooltip title={PASSWORD_TOOLTIP}>
          <Button
            danger
            disabled={inactive}
            onClick={() => setPasswordModalOpen(true)}
          >
            비밀번호 초기화
          </Button>
        </Tooltip>
      </Space>
      <DeviceResetModal
        employee={employee}
        open={deviceModalOpen}
        onClose={() => setDeviceModalOpen(false)}
      />
      <PasswordResetModal
        employee={employee}
        open={passwordModalOpen}
        onClose={() => setPasswordModalOpen(false)}
      />
    </div>
  );
}
