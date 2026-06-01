import { useState } from 'react';
import {
  Badge,
  Button,
  Card,
  DatePicker,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Tabs,
  Tag,
  Typography,
  App as AntdApp,
} from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  fetchLeaderDailyStatus,
  fetchLeaderAccounts,
  updateTeamMemberScheduleAccount,
  deleteTeamMemberSchedule,
  type LeaderDailyWorker,
} from '@/api/leader';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';

/**
 * 여사원 일별 현황 (레거시 employee/mngDaily + mgnSchedule).
 * 진열 카드는 거래처 변경(PUT)/삭제(DELETE) 가능 — 진열 일정변경(레거시 scheduleChange).
 * 행사 일정변경은 admin promotion 도메인 소유로 제외(읽기 전용).
 */
export default function LeaderDailyStatusPage() {
  const { message } = AntdApp.useApp();
  const queryClient = useQueryClient();
  const [date, setDate] = useState<Dayjs>(dayjs());
  const dateStr = date.format('YYYY-MM-DD');
  const [editing, setEditing] = useState<LeaderDailyWorker | null>(null);
  const [newAccountId, setNewAccountId] = useState<number | undefined>();

  const query = useQuery({
    queryKey: ['leader-daily', dateStr],
    queryFn: () => fetchLeaderDailyStatus(dateStr),
  });

  const accountsQuery = useQuery({
    queryKey: ['leader-accounts'],
    queryFn: () => fetchLeaderAccounts(),
    enabled: !!editing,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['leader-daily', dateStr] });

  const updateMutation = useMutation({
    mutationFn: ({ scheduleId, accountId }: { scheduleId: number; accountId: number }) =>
      updateTeamMemberScheduleAccount(scheduleId, accountId),
    onSuccess: () => {
      message.success('거래처가 변경되었습니다');
      setEditing(null);
      invalidate();
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '변경에 실패했습니다'),
  });

  const deleteMutation = useMutation({
    mutationFn: (scheduleId: number) => deleteTeamMemberSchedule(scheduleId),
    onSuccess: () => {
      message.success('삭제되었습니다');
      invalidate();
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '삭제에 실패했습니다'),
  });

  const renderWorker = (w: LeaderDailyWorker, editable: boolean) => (
    <Card size="small" style={{ marginBottom: 10 }} styles={{ body: { padding: 14 } }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
        <div style={{ minWidth: 0 }}>
          <Space size={6}>
            <Typography.Text strong>{w.employeeName}</Typography.Text>
            <Badge status={w.attended ? 'success' : 'default'} text={w.attended ? '출근' : '미출근'} />
          </Space>
          <div style={{ fontSize: 12, color: '#666' }}>{w.accountName}</div>
          <Space size={4} wrap style={{ marginTop: 4 }}>
            {[w.workingCategory1, w.workingCategory2, w.workingCategory3].filter(Boolean).map((c, i) => (
              <Tag key={i}>{c}</Tag>
            ))}
          </Space>
        </div>
        {editable && (
          <Space direction="vertical" size={4}>
            <Button
              size="small"
              onClick={() => {
                setEditing(w);
                setNewAccountId(undefined);
              }}
            >
              거래처 변경
            </Button>
            <Popconfirm
              title="일정을 삭제하시겠습니까?"
              okText="삭제"
              cancelText="취소"
              onConfirm={() => deleteMutation.mutate(w.scheduleId)}
            >
              <Button size="small" danger>
                삭제
              </Button>
            </Popconfirm>
          </Space>
        )}
      </div>
    </Card>
  );

  return (
    <>
      <DetailHeader title="여사원 일별 현황" />
      <DatePicker
        value={date}
        allowClear={false}
        onChange={(v) => v && setDate(v)}
        style={{ width: '100%', marginBottom: 12 }}
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(d) => (
          <>
            <Card size="small" style={{ marginBottom: 12 }}>
              <Space size="large" wrap>
                <span>진열 {d.summary.displayAttended}/{d.summary.displayTotal}</span>
                <span>행사 {d.summary.eventAttended}/{d.summary.eventTotal}</span>
                <span>연차 {d.summary.annualLeaveCount}</span>
              </Space>
            </Card>
            <Tabs
              items={[
                {
                  key: 'display',
                  label: `진열 (${d.displayWorkers.length})`,
                  children: (
                    <List
                      dataSource={d.displayWorkers}
                      split={false}
                      locale={{ emptyText: '진열 일정이 없습니다' }}
                      renderItem={(w) => renderWorker(w, true)}
                    />
                  ),
                },
                {
                  key: 'event',
                  label: `행사 (${d.eventWorkers.length})`,
                  children: (
                    <List
                      dataSource={d.eventWorkers}
                      split={false}
                      locale={{ emptyText: '행사 일정이 없습니다' }}
                      renderItem={(w) => renderWorker(w, false)}
                    />
                  ),
                },
                {
                  key: 'annual',
                  label: `연차 (${d.annualLeaveWorkers.length})`,
                  children: (
                    <List
                      dataSource={d.annualLeaveWorkers}
                      locale={{ emptyText: '연차자가 없습니다' }}
                      renderItem={(e) => (
                        <List.Item>
                          <List.Item.Meta title={e.employeeName} description={e.employeeCode} />
                        </List.Item>
                      )}
                    />
                  ),
                },
              ]}
            />
          </>
        )}
      </QueryBoundary>

      <Modal
        open={!!editing}
        title="거래처 변경"
        onCancel={() => setEditing(null)}
        okText="변경"
        cancelText="취소"
        confirmLoading={updateMutation.isPending}
        okButtonProps={{ disabled: !newAccountId }}
        onOk={() =>
          editing &&
          newAccountId &&
          updateMutation.mutate({ scheduleId: editing.scheduleId, accountId: newAccountId })
        }
      >
        <Select
          showSearch
          style={{ width: '100%' }}
          placeholder="변경할 거래처 선택"
          loading={accountsQuery.isLoading}
          optionFilterProp="label"
          value={newAccountId}
          onChange={setNewAccountId}
          options={accountsQuery.data?.map((a) => ({ value: a.id, label: a.name ?? String(a.id) }))}
        />
      </Modal>
    </>
  );
}
