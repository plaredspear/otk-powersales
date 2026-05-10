import { Modal, Table, Empty } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import 'dayjs/locale/ko';
import type { TeamSchedule } from '@/api/team-schedule';

interface DayScheduleListModalProps {
  open: boolean;
  onClose: () => void;
  date: string;
  schedules: TeamSchedule[];
  onScheduleClick: (schedule: TeamSchedule) => void;
}

const columns: ColumnsType<TeamSchedule> = [
  {
    title: '사원명',
    width: 140,
    render: (_, record) => `${record.employeeName}(${record.employeeCode})`,
  },
  {
    title: '근무형태',
    dataIndex: 'workingType',
    width: 80,
  },
  {
    title: '유형1',
    dataIndex: 'workingCategory1',
    width: 80,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '유형2',
    dataIndex: 'workingCategory2',
    width: 80,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '유형3',
    dataIndex: 'workingCategory3',
    width: 80,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '거래처',
    dataIndex: 'accountName',
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '출근',
    width: 60,
    render: (_, record) => {
      if (record.workingType !== '근무') return '-';
      return record.isClockIn ? (
        <span style={{ color: '#52c41a', fontWeight: 600 }}>O</span>
      ) : (
        <span style={{ color: '#ff4d4f', fontWeight: 600 }}>X</span>
      );
    },
  },
];

export function DayScheduleListModal({
  open,
  onClose,
  date,
  schedules,
  onScheduleClick,
}: DayScheduleListModalProps) {
  const title = date
    ? `${dayjs(date).locale('ko').format('YYYY년 M월 D일 (ddd)')} 일정`
    : '';

  const daySchedules = schedules.filter((s) => s.workingDate === date);

  return (
    <Modal
      title={title}
      open={open}
      onCancel={onClose}
      footer={null}
      width={800}
      destroyOnHidden
    >
      {daySchedules.length === 0 ? (
        <Empty description="등록된 일정이 없습니다" />
      ) : (
        <Table<TeamSchedule>
          columns={columns}
          dataSource={daySchedules}
          rowKey="id"
          pagination={false}
          size="small"
          onRow={(record) => ({
            onClick: () => {
              if (record.workingType === '근무') {
                onScheduleClick(record);
              }
            },
            style: {
              cursor: record.workingType === '근무' ? 'pointer' : 'default',
            },
          })}
        />
      )}
    </Modal>
  );
}
