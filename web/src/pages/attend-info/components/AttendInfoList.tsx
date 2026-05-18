import { Dropdown, Pagination, Space, Table, Tag } from 'antd';
import { MoreOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { AttendInfoListItem } from '@/api/attendInfo';

interface AttendInfoListProps {
  items: AttendInfoListItem[];
  loading: boolean;
  totalElements: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number, size: number) => void;
  onView: (item: AttendInfoListItem) => void;
  onDelete?: (item: AttendInfoListItem) => void;
}

const ATTEND_TYPE_COLOR: Record<string, string> = {
  '10': 'cyan',
  '14': 'green',
  '20': 'lime',
  '90': 'purple',
  '120': 'magenta',
  '133': 'orange',
};

function formatYyyyMmDd(value: string | null | undefined): string {
  if (!value || value.length !== 8) return value ?? '-';
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
}

export default function AttendInfoList({
  items,
  loading,
  totalElements,
  page,
  pageSize,
  onPageChange,
  onView,
  onDelete,
}: AttendInfoListProps) {
  const columns: ColumnsType<AttendInfoListItem> = [
    {
      title: '근태정보번호',
      dataIndex: 'name',
      key: 'name',
      render: (value: string | null) => value ?? <span style={{ color: '#bbb' }}>(SAP 미배포)</span>,
      width: 140,
    },
    {
      title: '사원번호',
      dataIndex: 'employeeCode',
      key: 'employeeCode',
      width: 110,
    },
    {
      title: '사원명',
      key: 'employee',
      render: (_: unknown, record: AttendInfoListItem) => (
        <Space size={4}>
          <span>{record.employeeName ?? '-'}</span>
          {record.employeeJobCode && (
            <Tag color="geekblue" style={{ fontSize: 11, padding: '0 4px' }}>
              {record.employeeJobCode}
            </Tag>
          )}
        </Space>
      ),
      width: 160,
    },
    {
      title: '근태유형',
      key: 'attendType',
      render: (_: unknown, record: AttendInfoListItem) => {
        if (!record.attendType) return '-';
        return (
          <Tag color={ATTEND_TYPE_COLOR[record.attendType] ?? 'default'}>
            {record.attendTypeName ?? record.attendType} ({record.attendType})
          </Tag>
        );
      },
      width: 160,
    },
    {
      title: '기간',
      key: 'range',
      render: (_: unknown, record: AttendInfoListItem) =>
        `${formatYyyyMmDd(record.startDate)} ~ ${formatYyyyMmDd(record.endDate)}`,
      width: 200,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      render: (value: AttendInfoListItem['status']) => {
        if (value === 'N') return <Tag color="blue">N</Tag>;
        if (value === 'Y') return <Tag>Y</Tag>;
        return '-';
      },
      width: 80,
    },
    {
      title: '등록자',
      dataIndex: 'createdByName',
      key: 'createdByName',
      render: (value: string | null) => value ?? <span style={{ color: '#bbb' }}>SAP</span>,
      width: 110,
    },
    {
      title: '',
      key: 'action',
      width: 60,
      render: (_: unknown, record: AttendInfoListItem) => {
        const menuItems: { key: string; label: string }[] = [{ key: 'view', label: '상세 / 수정' }];
        if (onDelete) menuItems.push({ key: 'delete', label: '삭제' });
        return (
          <Dropdown
            menu={{
              items: menuItems,
              onClick: (info) => {
                if (info.key === 'view') onView(record);
                if (info.key === 'delete' && onDelete) onDelete(record);
              },
            }}
            trigger={['click']}
          >
            <MoreOutlined style={{ cursor: 'pointer', fontSize: 18 }} />
          </Dropdown>
        );
      },
    },
  ];

  return (
    <>
      <Table
        rowKey="id"
        size="middle"
        loading={loading}
        dataSource={items}
        columns={columns}
        pagination={false}
      />
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
        <Pagination
          current={page}
          pageSize={pageSize}
          total={totalElements}
          showSizeChanger
          pageSizeOptions={['20', '50', '100', '200']}
          onChange={onPageChange}
        />
      </div>
    </>
  );
}
