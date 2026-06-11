import { useState } from 'react';
import { Space, Tag, Typography } from 'antd';
import { useAttendanceLogList } from '@/hooks/attendance-log/useAttendanceLog';
import type {
  AttendanceLogListItem,
  AttendanceTypeCode,
  FetchAttendanceLogParams,
} from '@/api/attendanceLog';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import RefreshButton from '@/components/common/RefreshButton';
import AttendanceLogFilter from './attendance/components/AttendanceLogFilter';
import AttendanceLogList from './attendance/components/AttendanceLogList';
import AttendanceLogDetailModal from './attendance/components/AttendanceLogDetailModal';

const { Title } = Typography;

const DEFAULT_PAGE_SIZE = 20;

export default function AttendancePage() {
  const { page, setPage, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      keyword: '',
      attendanceType: '',
      attendanceDateFrom: '',
      attendanceDateTo: '',
      size: String(DEFAULT_PAGE_SIZE),
    },
  });
  const [detailId, setDetailId] = useState<number | null>(null);

  const pageSize = Number.parseInt(filters.size, 10) || DEFAULT_PAGE_SIZE;

  const queryParams: FetchAttendanceLogParams = {
    keyword: filters.keyword || undefined,
    attendanceType: (filters.attendanceType as AttendanceTypeCode | '') || undefined,
    attendanceDateFrom: filters.attendanceDateFrom || undefined,
    attendanceDateTo: filters.attendanceDateTo || undefined,
    page,
    size: pageSize,
  };

  const { data, isLoading, refetch, isFetching } = useAttendanceLogList(queryParams);

  const handleFilterChange = (next: FetchAttendanceLogParams) => {
    setFilters({
      keyword: next.keyword ?? '',
      attendanceType: next.attendanceType ?? '',
      attendanceDateFrom: next.attendanceDateFrom ?? '',
      attendanceDateTo: next.attendanceDateTo ?? '',
    });
  };

  const handlePageChange = (nextPage: number, size: number) => {
    if (size !== pageSize) {
      setFilters({ size: String(size) });
    } else {
      setPage(nextPage - 1);
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          근무 등록현황
        </Title>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
      </Space>
      <Tag color="blue" style={{ marginBottom: 16 }}>
        모바일 앱에서 등록된 사원의 출근 이력 (DKRetail__CommuteLog__c) 조회 전용
      </Tag>
      <AttendanceLogFilter
        onChange={handleFilterChange}
        initialKeyword={filters.keyword}
        initialAttendanceType={filters.attendanceType as AttendanceTypeCode | ''}
        initialDateFrom={filters.attendanceDateFrom || undefined}
        initialDateTo={filters.attendanceDateTo || undefined}
      />
      <AttendanceLogList
        loading={isLoading}
        items={data?.content ?? []}
        totalElements={data?.totalElements ?? 0}
        page={(data?.number ?? page) + 1}
        pageSize={pageSize}
        onPageChange={handlePageChange}
        onView={(item: AttendanceLogListItem) => setDetailId(item.id)}
      />
      {detailId != null && (
        <AttendanceLogDetailModal
          attendanceLogId={detailId}
          onClose={() => setDetailId(null)}
        />
      )}
    </div>
  );
}
