import { useState } from 'react';
import { Button, Space, Tag, Typography, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useAttendInfoList, useDeleteAttendInfo } from '@/hooks/attend-info/useAttendInfo';
import { usePermission } from '@/hooks/usePermission';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import type { AttendInfoListItem, FetchAttendInfoParams } from '@/api/attendInfo';
import RefreshButton from '@/components/common/RefreshButton';
import AttendInfoFilter from './components/AttendInfoFilter';
import AttendInfoList from './components/AttendInfoList';
import AttendInfoCreateModal from './components/AttendInfoCreateModal';
import AttendInfoDetailModal from './components/AttendInfoDetailModal';
import AttendInfoDeleteConfirmModal from './components/AttendInfoDeleteConfirmModal';

const { Title } = Typography;

/**
 * 기준정보 > HR 적재 근무기간.
 *
 * 원래 '인사/근무 > 근무기간 조회' 의 세 번째 탭이었으나, SAP HR 인바운드가 적재한 마스터성 데이터라
 * 기준정보로 분리했다. 남은 두 탭(월별 근무내역 / 기간별 근무기간)은 조회 성격이라 인사/근무에 유지된다.
 *
 * 권한 가드는 `attend_info` — AdminAttendInfoController 전 endpoint 와 동일하다. 분리 전에는 세 탭이
 * 한 메뉴를 공유해 `attend_info` 권한이 없어도 나머지 두 탭 때문에 메뉴가 노출됐지만, 분리 후에는
 * 본 페이지만 `attend_info` 로 게이팅되어 권한 없는 프로필(예: 6.조장)에게 숨겨진다.
 */
export default function HrAttendInfoPage() {
  // 적용 필터/page/size 를 URL query string 에 보관 — 새로고침/링크 공유 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      employeeCode: '',
      keyword: '',
      attendType: '',
      status: '',
      startDateFrom: '',
      startDateTo: '',
    },
  });
  const [createOpen, setCreateOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AttendInfoListItem | null>(null);

  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('attend_info', 'EDIT');
  const canDelete = hasEntityPermission('attend_info', 'DELETE');

  const queryParams: FetchAttendInfoParams = {
    employeeCode: filters.employeeCode || undefined,
    keyword: filters.keyword || undefined,
    attendType: filters.attendType || undefined,
    status: (filters.status as 'N' | 'Y' | '') || undefined,
    startDateFrom: filters.startDateFrom || undefined,
    startDateTo: filters.startDateTo || undefined,
    page,
    size,
  };

  const { data, isLoading, refetch, isFetching } = useAttendInfoList(queryParams);
  const deleteMutation = useDeleteAttendInfo();

  const handleFilterChange = (next: FetchAttendInfoParams) => {
    setFilters({
      employeeCode: next.employeeCode ?? '',
      keyword: next.keyword ?? '',
      attendType: next.attendType ?? '',
      status: next.status ?? '',
      startDateFrom: next.startDateFrom ?? '',
      startDateTo: next.startDateTo ?? '',
    });
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
      <Title level={3} style={{ margin: '0 0 12px' }}>
        HR 적재 근무기간
      </Title>
      <Space style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            신규 등록
          </Button>
        )}
      </Space>
      <Tag color="orange" style={{ marginBottom: 16 }}>
        SAP HR 인바운드 적재 근무기간 데이터 — admin 보정 입력 / 수정 / 삭제 시 연차 일정 자동 cascade
      </Tag>
      <AttendInfoFilter
        onChange={handleFilterChange}
        initialEmployeeCode={filters.employeeCode}
        initialKeyword={filters.keyword}
        initialAttendType={filters.attendType}
        initialStatus={filters.status as 'N' | 'Y' | ''}
        initialDateFrom={filters.startDateFrom || undefined}
        initialDateTo={filters.startDateTo || undefined}
      />
      <AttendInfoList
        loading={isLoading}
        items={data?.content ?? []}
        totalElements={data?.totalElements ?? 0}
        page={data?.number ?? page}
        pageSize={size}
        onPageChange={setPage}
        onSizeChange={setSize}
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
