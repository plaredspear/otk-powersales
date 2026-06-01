import { useMemo, useState } from 'react';
import { App, Input, Modal, Spin, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { useCreateAssignment, usePermissionSets } from '@/hooks/admin/useAdminPermission';
import type { PermissionSetSummary } from '@/api/admin/permission';
import type { AxiosError } from 'axios';

const { Title } = Typography;

interface Props {
  open: boolean;
  userId: number;
  userLabel: string;
  onClose: () => void;
}

/**
 * Spec #804 — Scenario A: 직원에게 PermissionSet 부여 모달.
 *
 * 검색 → PermissionSet 선택 → 부여. 409 (이미 부여) 별도 alert.
 */
export default function AddPermissionSetAssignmentModal({ open, userId, userLabel, onClose }: Props) {
  const { message } = App.useApp();
  const { data: allSets, isLoading } = usePermissionSets();
  const create = useCreateAssignment();
  const [keyword, setKeyword] = useState('');
  const [selectedFlagsId, setSelectedFlagsId] = useState<number | null>(null);

  const filtered: PermissionSetSummary[] = useMemo(() => {
    if (!allSets) return [];
    if (!keyword) return allSets;
    const kw = keyword.toLowerCase();
    return allSets.filter(
      (s) =>
        s.name.toLowerCase().includes(kw) ||
        (s.label?.toLowerCase().includes(kw) ?? false) ||
        (s.description?.toLowerCase().includes(kw) ?? false),
    );
  }, [allSets, keyword]);

  const handleSubmit = async () => {
    if (!selectedFlagsId) return;
    try {
      await create.mutateAsync({ userId, permissionSetFlagsId: selectedFlagsId });
      message.success('부여되었습니다');
      handleClose();
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string; message?: string } }>;
      const code = err.response?.data?.error?.code;
      if (code === 'ASSIGNMENT_ALREADY_EXISTS') {
        message.error('이미 부여된 PermissionSet 입니다');
      } else {
        message.error(err.response?.data?.error?.message || '부여에 실패했습니다');
      }
    }
  };

  const handleClose = () => {
    setKeyword('');
    setSelectedFlagsId(null);
    onClose();
  };

  const columns: ColumnsType<PermissionSetSummary> = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Label', dataIndex: 'label', key: 'label' },
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
  ];

  return (
    <Modal
      open={open}
      title="PermissionSet 부여 추가"
      okText="부여"
      cancelText="취소"
      onOk={handleSubmit}
      onCancel={handleClose}
      okButtonProps={{ disabled: !selectedFlagsId, loading: create.isPending }}
      width={720}
    >
      <Title level={5}>대상 사용자: {userLabel}</Title>
      <Input.Search
        placeholder="name / label / description 검색"
        allowClear
        onSearch={(v) => setKeyword(v)}
        style={{ width: '100%', marginBottom: 12 }}
      />
      {isLoading ? (
        <Spin />
      ) : (
        <ResizableTable<PermissionSetSummary>
          dataSource={filtered}
          rowKey={(r) => r.permissionSetFlagsId ?? r.permissionSetId}
          columns={columns}
          size="small"
          pagination={{ pageSize: 10 }}
          rowSelection={{
            type: 'radio',
            selectedRowKeys: selectedFlagsId ? [selectedFlagsId] : [],
            onChange: (keys) => setSelectedFlagsId(keys[0] as number),
            getCheckboxProps: (row) => ({ disabled: !row.permissionSetFlagsId }),
          }}
        />
      )}
    </Modal>
  );
}
