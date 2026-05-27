import { useState } from 'react';
import { App, Input, Modal, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { fetchUsers, type UserSummary } from '@/api/user';
import { useCreateAssignmentBatch } from '@/hooks/admin/useAdminPermission';
import type { AxiosError } from 'axios';

const { Title } = Typography;

const PAGE_SIZE = 20;

interface Props {
  open: boolean;
  permissionSetFlagsId: number;
  permissionSetLabel: string;
  onClose: () => void;
}

/**
 * Spec #804 — Scenario B: PermissionSet 에 다수 사용자 부여 모달.
 *
 * 사용자 검색 (username/사번/이름) + multi-select → 일괄 부여.
 * 부분 성공 (이미 부여) 결과를 succeeded/skipped 로 분류 표시.
 */
export default function AddUserToPermissionSetModal({
  open,
  permissionSetFlagsId,
  permissionSetLabel,
  onClose,
}: Props) {
  const { message } = App.useApp();
  const [keyword, setKeyword] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ['users-for-assignment', keyword, page],
    queryFn: () => fetchUsers({ keyword, page, size: PAGE_SIZE, isActive: true }),
    enabled: open,
  });

  const batch = useCreateAssignmentBatch();

  const formatUsers = (userIds: number[]): string => {
    const rows = data?.content ?? [];
    const labels = userIds.map((id) => {
      const u = rows.find((r) => r.id === id);
      if (!u) return `#${id}`;
      const name = u.name ?? u.username;
      return u.employeeCode ? `${name}(${u.employeeCode})` : name;
    });
    return labels.join(', ');
  };

  const handleSubmit = async () => {
    if (selectedUserIds.length === 0) return;
    try {
      const result = await batch.mutateAsync({
        permissionSetFlagsId,
        userIds: selectedUserIds,
      });
      const { succeeded, skipped, failed } = result;

      if (succeeded.length > 0) {
        message.success(`${succeeded.length}명 부여되었습니다`);
      }
      if (skipped.length > 0) {
        const who = formatUsers(skipped.map((s) => s.userId));
        message.warning(`이미 부여된 사용자입니다: ${who}`);
      }
      if (failed.length > 0) {
        const who = formatUsers(failed.map((f) => f.userId));
        message.error(`부여 실패: ${who}`);
      }

      if (succeeded.length > 0 && failed.length === 0) {
        handleClose();
      } else {
        setSelectedUserIds([]);
      }
    } catch (e) {
      const err = e as AxiosError<{ error?: { message?: string } }>;
      message.error(err.response?.data?.error?.message || '일괄 부여에 실패했습니다');
    }
  };

  const handleClose = () => {
    setKeyword(undefined);
    setPage(0);
    setSelectedUserIds([]);
    onClose();
  };

  const columns: ColumnsType<UserSummary> = [
    { title: '사번', dataIndex: 'employeeCode', key: 'employeeCode', width: 140 },
    { title: '이름', dataIndex: 'name', key: 'name' },
    { title: 'Username', dataIndex: 'username', key: 'username' },
    { title: 'Profile', dataIndex: 'profileName', key: 'profileName', render: (val: string | null) => val ?? '-' },
    {
      title: '활성',
      dataIndex: 'isActive',
      key: 'isActive',
      width: 80,
      render: (v: boolean) => (v ? <Tag color="green">활성</Tag> : <Tag>비활성</Tag>),
    },
  ];

  return (
    <Modal
      open={open}
      title="PermissionSet 에 사용자 부여"
      okText={`부여 (${selectedUserIds.length}명)`}
      cancelText="취소"
      onOk={handleSubmit}
      onCancel={handleClose}
      okButtonProps={{ disabled: selectedUserIds.length === 0, loading: batch.isPending }}
      width={840}
    >
      <Title level={5}>대상 PermissionSet: {permissionSetLabel}</Title>
      <Input.Search
        placeholder="사번 / 이름 / username 검색"
        allowClear
        onSearch={(v) => {
          setKeyword(v || undefined);
          setPage(0);
          setSelectedUserIds([]);
        }}
        style={{ width: '100%', marginBottom: 12 }}
      />
      {isLoading ? (
        <Spin />
      ) : (
        <Table<UserSummary>
          dataSource={data?.content ?? []}
          rowKey="id"
          columns={columns}
          size="small"
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total: data?.totalElements ?? 0,
            onChange: (p) => setPage(p - 1),
          }}
          rowSelection={{
            type: 'checkbox',
            selectedRowKeys: selectedUserIds,
            onChange: (keys) => setSelectedUserIds(keys as number[]),
          }}
        />
      )}
    </Modal>
  );
}
