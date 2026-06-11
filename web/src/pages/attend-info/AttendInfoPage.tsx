import { useState } from 'react';
import { Button, Space, Tag, Typography, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useAttendInfoList, useDeleteAttendInfo } from '@/hooks/attend-info/useAttendInfo';
import { usePermission } from '@/hooks/usePermission';
import type { AttendInfoListItem, FetchAttendInfoParams } from '@/api/attendInfo';
import RefreshButton from '@/components/common/RefreshButton';
import AttendInfoFilter from './components/AttendInfoFilter';
import AttendInfoList from './components/AttendInfoList';
import AttendInfoCreateModal from './components/AttendInfoCreateModal';
import AttendInfoDetailModal from './components/AttendInfoDetailModal';
import AttendInfoDeleteConfirmModal from './components/AttendInfoDeleteConfirmModal';

const { Title } = Typography;

const DEFAULT_PAGE_SIZE = 20;

export default function AttendInfoPage() {
  const [filters, setFilters] = useState<FetchAttendInfoParams>({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
  });
  const [createOpen, setCreateOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AttendInfoListItem | null>(null);

  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('attend_info', 'EDIT');
  const canDelete = hasEntityPermission('attend_info', 'DELETE');

  const { data, isLoading, refetch, isFetching } = useAttendInfoList(filters);
  const deleteMutation = useDeleteAttendInfo();

  const handleFilterChange = (next: FetchAttendInfoParams) => {
    setFilters({ ...next, page: 0, size: filters.size ?? DEFAULT_PAGE_SIZE });
  };

  const handlePageChange = (page: number, size: number) => {
    setFilters((prev) => ({ ...prev, page: page - 1, size }));
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    try {
      const result = await deleteMutation.mutateAsync(deleteTarget.id);
      message.success(
        `근무기간 삭제 완료. 연결 연차 일정 ${result.deletedScheduleCount}건 함께 삭제되었습니다`,
      );
      setDeleteTarget(null);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '삭제에 실패했습니다');
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          근무기간 조회
        </Title>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          {canWrite && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
              신규 등록
            </Button>
          )}
        </Space>
      </Space>
      <Tag color="orange" style={{ marginBottom: 16 }}>
        SAP HR 인바운드 적재 근무기간 데이터 — admin 보정 입력 / 수정 / 삭제 시 연차 일정 자동 cascade
      </Tag>
      <AttendInfoFilter onChange={handleFilterChange} />
      <AttendInfoList
        loading={isLoading}
        items={data?.content ?? []}
        totalElements={data?.totalElements ?? 0}
        page={(data?.number ?? filters.page ?? 0) + 1}
        pageSize={filters.size ?? DEFAULT_PAGE_SIZE}
        onPageChange={handlePageChange}
        onView={(item) => setDetailId(item.id)}
        onDelete={canDelete ? (item) => setDeleteTarget(item) : undefined}
      />
      {createOpen && (
        <AttendInfoCreateModal open={createOpen} onClose={() => setCreateOpen(false)} />
      )}
      {detailId != null && (
        <AttendInfoDetailModal
          attendInfoId={detailId}
          canWrite={canWrite}
          onClose={() => setDetailId(null)}
        />
      )}
      {deleteTarget && (
        <AttendInfoDeleteConfirmModal
          target={deleteTarget}
          loading={deleteMutation.isPending}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
