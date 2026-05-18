import { useState } from 'react';
import { Space, Tag, Typography } from 'antd';
import { useAttendanceLogList } from '@/hooks/attendance-log/useAttendanceLog';
import type { AttendanceLogListItem, FetchAttendanceLogParams } from '@/api/attendanceLog';
import AttendanceLogFilter from './attendance/components/AttendanceLogFilter';
import AttendanceLogList from './attendance/components/AttendanceLogList';
import AttendanceLogDetailModal from './attendance/components/AttendanceLogDetailModal';

const { Title } = Typography;

const DEFAULT_PAGE_SIZE = 20;

export default function AttendancePage() {
  const [filters, setFilters] = useState<FetchAttendanceLogParams>({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
  });
  const [detailId, setDetailId] = useState<number | null>(null);

  const { data, isLoading } = useAttendanceLogList(filters);

  const handleFilterChange = (next: FetchAttendanceLogParams) => {
    setFilters({ ...next, page: 0, size: filters.size ?? DEFAULT_PAGE_SIZE });
  };

  const handlePageChange = (page: number, size: number) => {
    setFilters((prev) => ({ ...prev, page: page - 1, size }));
  };

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          근무 등록현황
        </Title>
      </Space>
      <Tag color="blue" style={{ marginBottom: 16 }}>
        모바일 앱에서 등록된 사원의 출근 이력 (DKRetail__CommuteLog__c) 조회 전용
      </Tag>
      <AttendanceLogFilter onChange={handleFilterChange} />
      <AttendanceLogList
        loading={isLoading}
        items={data?.content ?? []}
        totalElements={data?.totalElements ?? 0}
        page={(data?.number ?? filters.page ?? 0) + 1}
        pageSize={filters.size ?? DEFAULT_PAGE_SIZE}
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
