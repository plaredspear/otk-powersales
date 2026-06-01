import { useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Button,
  Descriptions,
  Input,
  Modal,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  notification,
} from 'antd';
import { useUserDetail } from '@/hooks/user/useUserDetail';
import {
  useResetUserPassword,
  useUpdateUserActiveStatus,
} from '@/hooks/user/useUserMutation';
import { useAuthStore } from '@/stores/authStore';
import { usePermission } from '@/hooks/usePermission';
import { useImpersonation } from '@/hooks/useImpersonation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const { Paragraph, Text } = Typography;

const TEMPORARY_PASSWORD = '1234';
const SELF_DEACTIVATE_TOOLTIP = '자기 자신 계정은 비활성화할 수 없습니다.';

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const userId = Number(id) || 0;
  const currentUserId = useAuthStore((state) => state.user?.id);
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  const { data: user, isLoading, error } = useUserDetail(userId);
  const resetMutation = useResetUserPassword();
  const activeMutation = useUpdateUserActiveStatus();
  const { hasSystemPermission } = usePermission();
  const { startMutation: impersonateMutation } = useImpersonation();

  const [resetOpen, setResetOpen] = useState(false);
  const [activeOpen, setActiveOpen] = useState(false);
  const [impersonateOpen, setImpersonateOpen] = useState(false);
  const [impersonateReason, setImpersonateReason] = useState('');

  useEffect(() => {
    setDynamicTitle(user?.name ?? user?.username ?? null);
    return () => setDynamicTitle(null);
  }, [user?.name, user?.username, setDynamicTitle]);

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error || !user) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/users')}>← 목록으로</Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          사용자를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const isSelf = currentUserId === user.id;
  const nextActive = !user.isActive;
  const blockSelfDeactivate = isSelf && user.isActive;
  // 대행 로그인 버튼 노출: MANAGE_USERS 보유 + 본인 아님 + 활성 사용자 (Spec #851)
  const canImpersonate = hasSystemPermission('MANAGE_USERS') && !isSelf && user.isActive;

  const handleImpersonateConfirm = async () => {
    try {
      await impersonateMutation.mutateAsync({
        targetUserId: user.id,
        reason: impersonateReason.trim() || undefined,
      });
      // navigate 는 훅 onSuccess 가 담당. 모달 닫기 책임은 페이지 (다른 모달과 패턴 통일).
      setImpersonateOpen(false);
    } catch {
      // 실패 알림은 useImpersonation 훅 onError 가 담당.
    }
  };

  const handleResetConfirm = async () => {
    try {
      await resetMutation.mutateAsync(user.id);
      notification.success({
        message: '비밀번호가 초기화되었습니다.',
        description: `임시 비밀번호 '${TEMPORARY_PASSWORD}' 를 사용자에게 전달해 주세요. 사용자는 다음 로그인 시 비밀번호 변경을 요구받습니다.`,
        duration: 10,
      });
      setResetOpen(false);
    } catch (err) {
      notification.error({
        message: '비밀번호 초기화 실패',
        description: (err as Error)?.message ?? '잠시 후 다시 시도해 주세요.',
      });
    }
  };

  const handleActiveConfirm = async () => {
    try {
      await activeMutation.mutateAsync({ id: user.id, isActive: nextActive });
      notification.success({
        message: nextActive ? '사용자가 활성화되었습니다.' : '사용자가 비활성화되었습니다.',
      });
      setActiveOpen(false);
    } catch (err) {
      notification.error({
        message: '상태 변경 실패',
        description: (err as Error)?.message ?? '잠시 후 다시 시도해 주세요.',
      });
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Button type="link" onClick={() => navigate('/users')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          {canImpersonate && (
            <Button onClick={() => setImpersonateOpen(true)}>대행 로그인</Button>
          )}
          <Button danger onClick={() => setResetOpen(true)}>
            비밀번호 초기화
          </Button>
          <Tooltip title={blockSelfDeactivate ? SELF_DEACTIVATE_TOOLTIP : undefined}>
            <Button
              danger={user.isActive}
              type={user.isActive ? 'default' : 'primary'}
              disabled={blockSelfDeactivate}
              onClick={() => setActiveOpen(true)}
            >
              {user.isActive ? '비활성화' : '활성화'}
            </Button>
          </Tooltip>
        </Space>
      </div>

      <div style={{ marginBottom: 16 }}>
        {user.isActive ? <Tag color="blue">활성</Tag> : <Tag>비활성</Tag>}
        <Tag color="purple" style={{ marginLeft: 8 }}>{user.profileName ?? '-'}</Tag>
        {user.isSalesSupport && <Tag color="gold">영업지원</Tag>}
        {user.passwordChangeRequired && (
          <Tag color="orange" style={{ marginLeft: 8 }}>비밀번호 변경 필요</Tag>
        )}
      </div>

      <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="username">{user.username}</Descriptions.Item>
        <Descriptions.Item label="사번">{user.employeeCode}</Descriptions.Item>
        <Descriptions.Item label="이름">{user.name ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="email">{user.email ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Profile">{user.profileName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="alias">{user.alias ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="직책">{user.title ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="부서">{user.department ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="사업부">{user.division ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="지점">{user.branch ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="휴대폰">{user.mobilePhone ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="전화">{user.phone ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="HR 코드">{user.hrCode ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="마지막 로그인">
          {user.lastLoginAt ? user.lastLoginAt.substring(0, 19).replace('T', ' ') : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="등록일">
          {user.createdAt ? user.createdAt.substring(0, 10) : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="최근 수정일" span={2}>
          {user.lastModifiedAt ? user.lastModifiedAt.substring(0, 19).replace('T', ' ') : '-'}
        </Descriptions.Item>
      </Descriptions>

      <Modal
        title="비밀번호 초기화 확인"
        open={resetOpen}
        onOk={handleResetConfirm}
        onCancel={() => setResetOpen(false)}
        okText="초기화 실행"
        cancelText="취소"
        okButtonProps={{ danger: true }}
        confirmLoading={resetMutation.isPending}
        destroyOnHidden
      >
        <Paragraph>다음 사용자의 비밀번호를 임시 비밀번호로 초기화합니다.</Paragraph>
        <Paragraph>
          <Text strong>username:</Text> {user.username}
          <br />
          <Text strong>이름:</Text> {user.name ?? '-'}
        </Paragraph>
        <Alert
          type="info"
          showIcon
          message="임시 비밀번호"
          description={
            <>
              <Text strong copyable style={{ fontSize: 20 }}>
                {TEMPORARY_PASSWORD}
              </Text>
              <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
                사용자 본인에게 별도 전달해 주세요.
              </Paragraph>
            </>
          }
          style={{ marginBottom: 12 }}
        />
        <Paragraph type="secondary">
          사용자는 다음 로그인 시 비밀번호 변경 화면으로 자동 이동합니다.
        </Paragraph>
      </Modal>

      <Modal
        title={user.isActive ? '사용자 비활성화 확인' : '사용자 활성화 확인'}
        open={activeOpen}
        onOk={handleActiveConfirm}
        onCancel={() => setActiveOpen(false)}
        okText={user.isActive ? '비활성화' : '활성화'}
        cancelText="취소"
        okButtonProps={{ danger: user.isActive }}
        confirmLoading={activeMutation.isPending}
        destroyOnHidden
      >
        <Paragraph>
          {user.isActive
            ? '비활성화된 사용자는 web 로그인이 차단됩니다.'
            : '활성화 시 다시 web 로그인이 가능해집니다.'}
        </Paragraph>
        <Paragraph>
          <Text strong>username:</Text> {user.username}
          <br />
          <Text strong>이름:</Text> {user.name ?? '-'}
        </Paragraph>
      </Modal>

      <Modal
        title="대행 로그인 확인"
        open={impersonateOpen}
        onOk={handleImpersonateConfirm}
        onCancel={() => setImpersonateOpen(false)}
        okText="대행 시작"
        cancelText="취소"
        confirmLoading={impersonateMutation.isPending}
        destroyOnHidden
        afterClose={() => setImpersonateReason('')}
      >
        <Paragraph>
          <Text strong>{user.name ?? user.username}</Text> 계정으로 대행 로그인합니다. 대행 중에는 해당
          사용자의 권한으로 화면을 사용하게 되며, 상단 배너의 "관리자로 복귀" 로 종료할 수 있습니다.
        </Paragraph>
        <Paragraph>
          <Text strong>username:</Text> {user.username}
          <br />
          <Text strong>이름:</Text> {user.name ?? '-'}
        </Paragraph>
        <Input.TextArea
          placeholder="대행 사유 (선택, 최대 500자)"
          maxLength={500}
          rows={2}
          value={impersonateReason}
          onChange={(e) => setImpersonateReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}
