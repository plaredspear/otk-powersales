import { useEffect, useState } from 'react';
import { Alert, Form, Modal, Select, Spin, notification } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { fetchUserProfileOptions } from '@/api/user';
import type { UserDetail } from '@/api/user';
import { useUpdateUserProfile } from '@/hooks/user/useUserMutation';

interface UserProfileModalProps {
  user: UserDetail;
  open: boolean;
  onClose: () => void;
}

/**
 * User 프로파일 수동 변경 모달 — 시스템 관리자 전용.
 *
 * 평시 프로파일은 SAP 발령 후처리가 사원의 조직/직책으로부터 산출한다. 발령 인입 전에 권한이
 * 필요한 예외 상황(발령 예정일 이전 인수인계 등)을 위해 그 산출값을 수동으로 덮어쓰는 경로다.
 *
 * 변경분은 대상 사원의 **다음 발령이 인입되면 재산출 값으로 덮어써진다** — 상시 권한 운영이 아닌
 * 한시적 예외 처리 수단이라는 점을 모달 본문에 명시해 운영자가 오해하지 않도록 한다.
 */
export default function UserProfileModal({ user, open, onClose }: UserProfileModalProps) {
  const [profileId, setProfileId] = useState<number | undefined>(user.profileId ?? undefined);
  const mutation = useUpdateUserProfile();

  const { data: options, isLoading } = useQuery({
    queryKey: ['admin', 'users', 'profile-options'],
    queryFn: fetchUserProfileOptions,
    // 모달을 열 때만 조회 — 상세 진입마다 프로파일 전량을 끌어오지 않는다.
    enabled: open,
  });

  // 모달을 다시 열 때 이전 선택이 남지 않도록 현재 프로파일로 되돌린다.
  useEffect(() => {
    if (open) setProfileId(user.profileId ?? undefined);
  }, [open, user.profileId]);

  const handleSubmit = async () => {
    if (profileId == null) return;
    try {
      await mutation.mutateAsync({ id: user.id, profileId });
      notification.success({
        message: '프로파일이 변경되었습니다',
        description: '대상 사용자는 다음 요청부터 새 권한으로 동작합니다.',
      });
      onClose();
    } catch (err) {
      notification.error({
        message: '프로파일 변경 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류',
      });
    }
  };

  return (
    <Modal
      title={`프로파일 변경 — ${user.name ?? user.username} (${user.employeeCode})`}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      okText="변경"
      cancelText="취소"
      width={520}
      confirmLoading={mutation.isPending}
      okButtonProps={{ disabled: profileId == null || profileId === user.profileId }}
      destroyOnHidden
    >
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="발령이 들어오면 되돌아갑니다"
        description="프로파일은 평소 SAP 발령에 따라 자동으로 정해집니다. 여기서 바꾼 값은 이 사원의 다음 발령이 반영될 때 자동 산출값으로 덮어써집니다. 인수인계처럼 발령 전 한시적으로 권한이 필요할 때만 사용하세요."
      />

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin />
        </div>
      ) : (
        <Form layout="vertical">
          <Form.Item label="현재 프로파일">
            <span>{user.profileName ?? '-'}</span>
          </Form.Item>
          <Form.Item label="변경할 프로파일" required>
            <Select
              value={profileId}
              onChange={setProfileId}
              placeholder="프로파일을 선택하세요"
              showSearch
              optionFilterProp="label"
              options={(options ?? []).map((o) => ({ value: o.id, label: o.name }))}
            />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}
