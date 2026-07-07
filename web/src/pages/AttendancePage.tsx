import { useState } from 'react';
import { Space, Tag, Typography } from 'antd';
import { useAttendanceLogList } from '@/hooks/attendance-log/useAttendanceLog';
import type {
  AttendanceLogListItem,
  AttendanceTypeCode,
  FetchAttendanceLogParams,
} from '@/api/attendanceLog';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import RefreshButton from '@/components/common/RefreshButton';
import AttendanceLogFilter from './attendance/components/AttendanceLogFilter';
import AttendanceLogList from './attendance/components/AttendanceLogList';
import AttendanceLogDetailModal from './attendance/components/AttendanceLogDetailModal';

const { Title } = Typography;

const DEFAULT_PAGE_SIZE = 20;

export default function AttendancePage() {
  // 페이지 전체 스크롤 제거 — 제목/태그/필터는 고정, 목록 테이블 body(행) 만 세로 스크롤. 높이는 상단
  // 가변 요소를 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56). scrollY 는 자식에 전달.
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // 적용 필터/page/size 를 URL query string 에 보관 — 새로고침/링크 공유 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      keyword: '',
      attendanceType: '',
      attendanceDateFrom: '',
      attendanceDateTo: '',
    },
    defaultPageSize: DEFAULT_PAGE_SIZE,
  });
  const [detailId, setDetailId] = useState<number | null>(null);

  const queryParams: FetchAttendanceLogParams = {
    keyword: filters.keyword || undefined,
    attendanceType: (filters.attendanceType as AttendanceTypeCode | '') || undefined,
    attendanceDateFrom: filters.attendanceDateFrom || undefined,
    attendanceDateTo: filters.attendanceDateTo || undefined,
    page,
    size,
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

  return (
    // 페이지 전체 스크롤 제거 — 컨테이너를 실측 가용 높이에 고정. 제목/태그/필터는 고정, 목록만 스크롤.
    <div
      ref={containerRef}
      style={{
        padding: 24,
        display: 'flex',
        flexDirection: 'column',
        height: containerHeight,
        boxSizing: 'border-box',
        minHeight: 0,
      }}
    >
      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16, flexShrink: 0 }}>
        <Title level={3} style={{ margin: 0 }}>
          근무 등록현황
        </Title>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
      </Space>
      <Tag color="blue" style={{ marginBottom: 16, flexShrink: 0 }}>
        모바일 앱에서 등록된 사원의 출근 이력 (DKRetail__CommuteLog__c) 조회 전용
      </Tag>
      <div style={{ flexShrink: 0 }}>
        <AttendanceLogFilter
          onChange={handleFilterChange}
          initialKeyword={filters.keyword}
          initialAttendanceType={filters.attendanceType as AttendanceTypeCode | ''}
          initialDateFrom={filters.attendanceDateFrom || undefined}
          initialDateTo={filters.attendanceDateTo || undefined}
        />
      </div>
      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이를 scrollY 로 자식에 전달. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <AttendanceLogList
          loading={isLoading}
          items={data?.content ?? []}
          totalElements={data?.totalElements ?? 0}
          page={data?.number ?? page}
          pageSize={size}
          onPageChange={setPage}
          onSizeChange={setSize}
          onView={(item: AttendanceLogListItem) => setDetailId(item.id)}
          scrollY={scrollY}
        />
      </div>
      {detailId != null && (
        <AttendanceLogDetailModal
          attendanceLogId={detailId}
          onClose={() => setDetailId(null)}
        />
      )}
    </div>
  );
}
