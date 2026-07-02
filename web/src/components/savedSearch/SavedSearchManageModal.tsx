import { Modal, Tag, Button, Popconfirm, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { useSavedSearches } from '@/hooks/savedSearch/useSavedSearches';
import { useDeleteSavedSearch } from '@/hooks/savedSearch/useSavedSearchMutation';
import type { SavedSearch } from '@/api/savedSearch';

interface SavedSearchManageModalProps {
  open: boolean;
  resourceKey: string;
  onClose: () => void;
}

export default function SavedSearchManageModal({
  open,
  resourceKey,
  onClose,
}: SavedSearchManageModalProps) {
  const { data, isLoading } = useSavedSearches(resourceKey);
  const deleteMutation = useDeleteSavedSearch();

  const handleDelete = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success('삭제했습니다');
    } catch {
      message.error('삭제에 실패했습니다');
    }
  };

  const columns: ColumnsType<SavedSearch> = [
    { title: '이름', dataIndex: 'name', ellipsis: true },
    {
      title: '공개',
      dataIndex: 'scope',
      width: 90,
      align: 'center',
      render: (scope: SavedSearch['scope']) =>
        scope === 'SHARED' ? <Tag color="blue">공용</Tag> : <Tag>개인</Tag>,
    },
    { title: '소유자', dataIndex: 'ownerName', width: 110, render: (v: string | null) => v ?? '-' },
    { title: '순서', dataIndex: 'sortOrder', width: 70, align: 'center' },
    {
      title: '작업',
      width: 100,
      align: 'center',
      render: (_: unknown, record) => (
        <Popconfirm
          title="이 검색을 삭제할까요?"
          okText="삭제"
          cancelText="취소"
          disabled={!record.editable}
          onConfirm={() => handleDelete(record.id)}
        >
          <Button size="small" danger disabled={!record.editable}>
            삭제
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Modal
      title="저장된 검색 관리"
      open={open}
      onCancel={onClose}
      footer={null}
      width={640}
      destroyOnClose
    >
      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        pagination={false}
      />
    </Modal>
  );
}
