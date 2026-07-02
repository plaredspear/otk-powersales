import { Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { AttendanceLogListItem, AttendanceTypeCode } from '@/api/attendanceLog';
import ResizableTable from '@/components/common/ResizableTable';
import { buildListPagination } from '@/lib/listPagination';

interface AttendanceLogListProps {
  items: AttendanceLogListItem[];
  loading: boolean;
  totalElements: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number, size: number) => void;
  onView: (item: AttendanceLogListItem) => void;
}

const ATTENDANCE_TYPE_LABEL: Record<AttendanceTypeCode, string> = {
  REGULAR: '일반',
  DISPLAY: '진열',
  EVENT: '행사',
};

const ATTENDANCE_TYPE_COLOR: Record<AttendanceTypeCode, string> = {
  REGULAR: 'blue',
  DISPLAY: 'green',
  EVENT: 'purple',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : value;
}

export default function AttendanceLogList({
  items,
  loading,
  totalElements,
  page,
  pageSize,
  onPageChange,
  onView,
}: AttendanceLogListProps) {
  const columns: ColumnsType<AttendanceLogListItem> = [
    {
      title: 'No.',
      dataIndex: 'name',
      key: 'name',
      render: (value: string | null) => value ?? <span style={{ color: '#bbb' }}>-</span>,
      width: 110,
    },
    {
      title: '사원',
      key: 'employee',
      render: (_: unknown, record: AttendanceLogListItem) => (
        <Space size={4}>
          <span>{record.employeeName ?? '-'}</span>
          {record.employeeCode && (
            <span style={{ color: '#888', fontSize: 12 }}>({record.employeeCode})</span>
          )}
          {record.employeeJobCode && (
            <Tag color="geekblue" style={{ fontSize: 11, padding: '0 4px' }}>
              {record.employeeJobCode}
            </Tag>
          )}
        </Space>
      ),
      width: 220,
    },
    {
      title: '거래처',
      key: 'account',
      render: (_: unknown, record: AttendanceLogListItem) => {
        if (!record.accountName && !record.accountCode) return '-';
        return (
          <Space size={4}>
            <span>{record.accountName ?? '-'}</span>
            {record.accountCode && (
              <span style={{ color: '#888', fontSize: 12 }}>({record.accountCode})</span>
            )}
          </Space>
        );
      },
      width: 220,
    },
    {
      title: '출근 종류',
      dataIndex: 'attendanceType',
      key: 'attendanceType',
      render: (value: AttendanceTypeCode | null) => {
        if (!value) return '-';
        return <Tag color={ATTENDANCE_TYPE_COLOR[value]}>{ATTENDANCE_TYPE_LABEL[value]}</Tag>;
      },
      width: 100,
    },
    {
      title: '근무유형',
      dataIndex: 'secondWorkTypeName',
      key: 'secondWorkTypeName',
      render: (value: string | null) => value ?? '-',
      width: 110,
    },
    {
      title: '출근일시',
      dataIndex: 'attendanceDate',
      key: 'attendanceDate',
      render: (value: string | null) => formatDateTime(value),
      width: 160,
    },
    {
      title: '사유',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (value: string | null) => value ?? '-',
    },
  ];

  return (
    <ResizableTable
      rowKey="id"
      size="middle"
      loading={loading}
      dataSource={items}
      columns={columns}
      pagination={buildListPagination({
        page: page - 1,
        pageSize,
        total: totalElements,
        onPageChange: (nextPage) => onPageChange(nextPage + 1, pageSize),
        onSizeChange: (size) => onPageChange(1, size),
      })}
      onRow={(record) => ({
        onClick: () => onView(record),
        style: { cursor: 'pointer' },
      })}
    />
  );
}
