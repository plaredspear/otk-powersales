import { useState } from 'react';
import { Alert, Button, Card, DatePicker, Empty, Grid, Spin, Tag, Typography } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import {
  fetchSafetyCheckStatus,
  type MemberStatus,
} from '@/api/safetyCheck';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;
const { useBreakpoint } = Grid;

const EQUIPMENT_COUNT = 9;

function formatTime(datetime: string | null): string {
  if (!datetime) return '-';
  return dayjs(datetime).format('HH:mm');
}

function formatPrecautions(precautions: string | null): string {
  if (!precautions) return '-';
  return precautions.replace(/;/g, ', ');
}

export default function SafetyCheckPage() {
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());
  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const dateStr = selectedDate.format('YYYY-MM-DD');
  const dayOfWeek = selectedDate.format('ddd');

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin', 'safety-check', 'status', dateStr],
    queryFn: () => fetchSafetyCheckStatus(dateStr),
  });

  const goToPrevDay = () => setSelectedDate((d) => d.subtract(1, 'day'));
  const goToNextDay = () => setSelectedDate((d) => d.add(1, 'day'));
  const onDateChange = (date: Dayjs | null) => {
    if (date) setSelectedDate(date);
  };

  const equipmentLabels: string[] = [];
  if (data && data.members.length > 0) {
    const firstSubmitted = data.members.find((m) => m.submitted && m.equipments.length > 0);
    if (firstSubmitted) {
      for (let i = 0; i < EQUIPMENT_COUNT; i++) {
        equipmentLabels[i] = firstSubmitted.equipments[i]?.label ?? `항목 ${i + 1}`;
      }
    }
  }
  for (let i = 0; i < EQUIPMENT_COUNT; i++) {
    if (!equipmentLabels[i]) equipmentLabels[i] = `항목 ${i + 1}`;
  }

  const columns: ColumnsType<MemberStatus> = [
    { title: '사번', dataIndex: 'employeeCode', width: 90, fixed: 'left' },
    { title: '사원명', dataIndex: 'employeeName', width: 80, fixed: 'left' },
    { title: '거래처코드', dataIndex: 'accountCode', width: 100, render: (v) => v ?? '-' },
    { title: '거래처명', dataIndex: 'accountName', width: 140, render: (v) => v ?? '-' },
    {
      title: '제출',
      dataIndex: 'submitted',
      width: 70,
      render: (submitted: boolean) =>
        submitted ? <Tag color="green">완료</Tag> : <Tag>미제출</Tag>,
    },
    {
      title: '설문시작시간',
      dataIndex: 'startTime',
      width: 90,
      render: (_: unknown, record: MemberStatus) =>
        record.submitted ? formatTime(record.startTime) : '-',
    },
    {
      title: '제출시각',
      dataIndex: 'submittedAt',
      width: 90,
      render: (_: unknown, record: MemberStatus) =>
        record.submitted ? formatTime(record.submittedAt) : '-',
    },
    ...Array.from({ length: EQUIPMENT_COUNT }, (_, i) => ({
      title: equipmentLabels[i],
      width: 80,
      render: (_: unknown, record: MemberStatus) => {
        if (!record.submitted) return '-';
        return record.equipments[i]?.answer ?? '-';
      },
    })),
    {
      title: '"예" 수',
      dataIndex: 'yesCount',
      width: 60,
      render: (_: unknown, record: MemberStatus) => (record.submitted ? record.yesCount : '-'),
    },
    {
      title: '"해당없음" 수',
      dataIndex: 'noCount',
      width: 80,
      render: (_: unknown, record: MemberStatus) => (record.submitted ? record.noCount : '-'),
    },
    {
      title: '예방사항',
      dataIndex: 'precautions',
      width: 160,
      render: (_: unknown, record: MemberStatus) =>
        record.submitted ? formatPrecautions(record.precautions) : '-',
    },
    {
      title: '예방사항 수',
      dataIndex: 'precautionCount',
      width: 80,
      render: (_: unknown, record: MemberStatus) =>
        record.submitted ? record.precautionCount : '-',
    },
    {
      title: '출근현황',
      dataIndex: 'workReportStatus',
      width: 80,
      render: (v) => v ?? '-',
    },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="안전점검 현황 조회 실패"
          description={error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다'}
          action={
            <Button size="small" onClick={() => refetch()}>
              재시도
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginBottom: 8,
        }}
      >
        <Text strong>점검일자</Text>
        <Button icon={<LeftOutlined />} size="small" onClick={goToPrevDay} />
        <Text>
          {selectedDate.format('YYYY-MM-DD')} ({dayOfWeek})
        </Text>
        <Button icon={<RightOutlined />} size="small" onClick={goToNextDay} />
        <DatePicker value={selectedDate} onChange={onDateChange} allowClear={false} />
      </div>

      <div style={{ marginBottom: 16 }}>
        <Text>
          제출 {data?.submittedCount ?? 0}명 / 미제출 {data?.notSubmittedCount ?? 0}명 (총{' '}
          {data?.totalCount ?? 0}명)
        </Text>
      </div>

      <Spin spinning={isLoading}>
        {isMobile ? (
          data && data.totalCount === 0 ? (
            <Empty description="해당 날짜에 근무 스케줄이 없습니다" />
          ) : (
            <MemberCardList members={data?.members ?? []} equipmentLabels={equipmentLabels} />
          )
        ) : (
          <ResizableTable
            rowKey="id"
            columns={columns}
            dataSource={data?.members}
            loading={isLoading}
            pagination={false}
            scroll={{ x: 2000 }}
            size="small"
            locale={{ emptyText: '해당 날짜에 근무 스케줄이 없습니다' }}
          />
        )}
      </Spin>
    </div>
  );
}

function MemberCardList({
  members,
  equipmentLabels,
}: {
  members: MemberStatus[];
  equipmentLabels: string[];
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {members.map((m) => (
        <Card
          key={m.id}
          size="small"
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>
                {m.employeeCode} {m.employeeName}
              </span>
              {m.submitted ? <Tag color="green">완료</Tag> : <Tag>미제출</Tag>}
            </div>
          }
        >
          <CardRow label="거래처코드" value={m.accountCode ?? '-'} />
          <CardRow label="거래처명" value={m.accountName ?? '-'} />
          <CardRow label="설문시작시간" value={m.submitted ? formatTime(m.startTime) : '-'} />
          <CardRow label="제출시각" value={m.submitted ? formatTime(m.submittedAt) : '-'} />
          {Array.from({ length: EQUIPMENT_COUNT }, (_, i) => (
            <CardRow
              key={i}
              label={equipmentLabels[i]}
              value={m.submitted ? (m.equipments[i]?.answer ?? '-') : '-'}
            />
          ))}
          <CardRow label='"예" 수' value={m.submitted ? String(m.yesCount) : '-'} />
          <CardRow label='"해당없음" 수' value={m.submitted ? String(m.noCount) : '-'} />
          <CardRow
            label="예방사항"
            value={m.submitted ? formatPrecautions(m.precautions) : '-'}
          />
          <CardRow
            label="예방사항 수"
            value={m.submitted ? String(m.precautionCount) : '-'}
          />
          <CardRow label="출근현황" value={m.workReportStatus ?? '-'} />
        </Card>
      ))}
    </div>
  );
}

function CardRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        gap: 8,
        padding: '4px 0',
        borderBottom: '1px solid #f0f0f0',
        fontSize: 13,
      }}
    >
      <span style={{ color: '#666', flexShrink: 0, maxWidth: '55%' }}>{label}</span>
      <span style={{ textAlign: 'right', wordBreak: 'break-all' }}>{value}</span>
    </div>
  );
}
