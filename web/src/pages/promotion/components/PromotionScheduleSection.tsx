import { useMemo, useState } from 'react';
import { Button, DatePicker, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  usePromotionSchedules,
} from '@/hooks/promotion/usePromotionSchedules';
import { usePermission } from '@/hooks/usePermission';
import { getPPTTeamTypeColor } from '@/constants/pptTeamType';
import type {
  PromotionScheduleItem,
  PromotionScheduleMember,
} from '@/api/promotionSchedule';
import PromotionScheduleBulkUpdateModal from './PromotionScheduleBulkUpdateModal';
import PromotionScheduleBulkDeleteDialog from './PromotionScheduleBulkDeleteDialog';

const { Title } = Typography;

interface FlatRow {
  key: string; // schedule_id 단일 key
  scheduleId: number;
  member: PromotionScheduleMember;
  schedule: PromotionScheduleItem;
}

interface Props {
  promotionId: number;
  promotionStartDate: string;
  promotionEndDate: string;
}

function flatten(members: PromotionScheduleMember[]): FlatRow[] {
  const rows: FlatRow[] = [];
  for (const member of members) {
    for (const schedule of member.schedules) {
      rows.push({
        key: String(schedule.scheduleId),
        scheduleId: schedule.scheduleId,
        member,
        schedule,
      });
    }
  }
  return rows;
}

export default function PromotionScheduleSection({
  promotionId,
  promotionStartDate,
  promotionEndDate,
}: Props) {
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('promotion', 'EDIT');

  const [startDate, setStartDate] = useState<string | undefined>(undefined);
  const [endDate, setEndDate] = useState<string | undefined>(undefined);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [bulkUpdateOpen, setBulkUpdateOpen] = useState(false);
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);

  const { data, isLoading } = usePromotionSchedules(promotionId, {
    startDate,
    endDate,
  });

  const rows = useMemo(() => flatten(data?.members ?? []), [data]);
  const selectedRows = useMemo(
    () => rows.filter((r) => selectedKeys.includes(r.key)),
    [rows, selectedKeys],
  );

  const effectiveStart = data?.schedulePeriod.startDate ?? promotionStartDate;
  const effectiveEnd = data?.schedulePeriod.endDate ?? promotionEndDate;

  const handleSelectionSuccess = () => {
    setSelectedKeys([]);
  };

  const columns: ColumnsType<FlatRow> = [
    {
      title: '사번',
      dataIndex: ['member', 'employeeNumber'],
      width: 100,
      align: 'center' as const,
    },
    {
      title: '사원명',
      dataIndex: ['member', 'employeeName'],
      width: 120,
    },
    {
      title: '전문행사조',
      dataIndex: ['member', 'professionalPromotionTeam'],
      width: 130,
      align: 'center' as const,
      render: (v: string | null) =>
        v ? <Tag color={getPPTTeamTypeColor(v)}>{v}</Tag> : '-',
    },
    {
      title: '작업일자',
      dataIndex: ['schedule', 'workingDate'],
      width: 110,
      align: 'center' as const,
      render: (v: string) => v.replace(/-/g, '/'),
    },
    {
      title: '거래처',
      dataIndex: ['schedule', 'accountName'],
      width: 200,
      render: (name: string, record: FlatRow) => {
        const code = record.schedule.accountCode;
        return code ? `${name} (${code})` : name;
      },
    },
    {
      title: '분류1',
      dataIndex: ['schedule', 'workingCategory1'],
      width: 70,
      align: 'center' as const,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '분류3',
      dataIndex: ['schedule', 'workingCategory3'],
      width: 70,
      align: 'center' as const,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '분류4',
      dataIndex: ['schedule', 'workingCategory4'],
      width: 100,
      align: 'center' as const,
      render: (v: string | null) => v ?? '-',
    },
  ];

  const noSelection = selectedKeys.length === 0;
  const writeDisabledTooltip = !canWrite ? '권한이 없습니다 (PROMOTION_WRITE)' : '';
  const selectionDisabledTooltip = noSelection ? '먼저 일정을 선택하세요' : '';

  return (
    <div style={{ marginTop: 32 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Title level={5} style={{ margin: 0 }}>
          일정 관리
        </Title>
        <Space>
          <Tooltip title={writeDisabledTooltip || selectionDisabledTooltip}>
            <Button
              onClick={() => setBulkUpdateOpen(true)}
              disabled={!canWrite || noSelection}
            >
              일괄 변경
            </Button>
          </Tooltip>
          <Tooltip title={writeDisabledTooltip || selectionDisabledTooltip}>
            <Button
              danger
              onClick={() => setBulkDeleteOpen(true)}
              disabled={!canWrite || noSelection}
            >
              일괄 삭제
            </Button>
          </Tooltip>
        </Space>
      </div>

      <Space style={{ marginBottom: 12 }}>
        <span>기간:</span>
        <DatePicker
          format="YYYY-MM-DD"
          value={startDate ? dayjs(startDate) : dayjs(effectiveStart)}
          onChange={(d) => setStartDate(d ? d.format('YYYY-MM-DD') : undefined)}
          allowClear={false}
        />
        <span>~</span>
        <DatePicker
          format="YYYY-MM-DD"
          value={endDate ? dayjs(endDate) : dayjs(effectiveEnd)}
          onChange={(d) => setEndDate(d ? d.format('YYYY-MM-DD') : undefined)}
          allowClear={false}
        />
        <span style={{ color: '#666' }}>
          선택: {selectedKeys.length}건 / 총 {data?.totalScheduleCount ?? 0}건
        </span>
        {selectedKeys.length > 0 && (
          <Button size="small" onClick={() => setSelectedKeys([])}>
            선택해제
          </Button>
        )}
      </Space>

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 32 }}>
          <Spin />
        </div>
      ) : (
        <Table<FlatRow>
          columns={columns}
          dataSource={rows}
          rowKey="key"
          size="small"
          pagination={false}
          rowSelection={{
            selectedRowKeys: selectedKeys,
            onChange: setSelectedKeys,
          }}
          locale={{ emptyText: '해당 기간의 일정이 없습니다' }}
          scroll={{ x: 1000 }}
          footer={() =>
            `배치원 ${data?.totalMemberCount ?? 0}명 / 일정 ${data?.totalScheduleCount ?? 0}건`
          }
        />
      )}

      <PromotionScheduleBulkUpdateModal
        open={bulkUpdateOpen}
        promotionId={promotionId}
        selected={selectedRows}
        onClose={() => setBulkUpdateOpen(false)}
        onSuccess={handleSelectionSuccess}
      />
      <PromotionScheduleBulkDeleteDialog
        open={bulkDeleteOpen}
        promotionId={promotionId}
        selected={selectedRows}
        onClose={() => setBulkDeleteOpen(false)}
        onSuccess={handleSelectionSuccess}
      />
    </div>
  );
}
