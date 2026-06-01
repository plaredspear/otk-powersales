import { useMemo, useRef, useState } from 'react';
import { Button, Card, Radio, Result, Space, Typography, App as AntdApp } from 'antd';
import { CheckCircleTwoTone } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  fetchSafetyCheckItems,
  fetchSafetyCheckToday,
  submitSafetyCheck,
  type EquipmentAnswer,
} from '@/api/safetyCheck';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDateTime } from '@/lib/format';

const DEFAULT_OPTIONS = ['양호', '불량'];

export default function SafetyCheckPage() {
  const { message } = AntdApp.useApp();
  const queryClient = useQueryClient();
  const startTimeRef = useRef(dayjs().format('YYYY-MM-DDTHH:mm:ss'));
  const [answers, setAnswers] = useState<Record<number, string>>({});

  const itemsQuery = useQuery({ queryKey: ['safety-items'], queryFn: fetchSafetyCheckItems });
  const todayQuery = useQuery({ queryKey: ['safety-today'], queryFn: fetchSafetyCheckToday });

  const allItems = useMemo(
    () =>
      itemsQuery.data?.categories.flatMap((c) =>
        c.items.map((it) => ({ ...it, options: c.options ?? DEFAULT_OPTIONS }))
      ) ?? [],
    [itemsQuery.data]
  );

  const mutation = useMutation({
    mutationFn: () => {
      const equipments: EquipmentAnswer[] = allItems.map((it) => ({
        seqNum: it.seqNum,
        answer: answers[it.seqNum],
      }));
      return submitSafetyCheck({
        startTime: startTimeRef.current,
        completeTime: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
        equipments,
      });
    },
    onSuccess: () => {
      message.success('안전점검이 완료되었습니다');
      queryClient.invalidateQueries({ queryKey: ['safety-today'] });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '제출에 실패했습니다'),
  });

  const onSubmit = () => {
    const unanswered = allItems.filter((it) => !answers[it.seqNum]);
    if (unanswered.length > 0) {
      message.warning('모든 항목에 응답해주세요');
      return;
    }
    mutation.mutate();
  };

  // 오늘 이미 완료
  if (todayQuery.data?.completed) {
    return (
      <>
        <DetailHeader title="안전점검" />
        <Result
          icon={<CheckCircleTwoTone twoToneColor="#52c41a" />}
          title="오늘 안전점검 완료"
          subTitle={`제출: ${formatDateTime(todayQuery.data.submittedAt)}`}
        />
      </>
    );
  }

  return (
    <>
      <DetailHeader title="안전점검" />
    <QueryBoundary
      isLoading={itemsQuery.isLoading}
      isError={itemsQuery.isError}
      data={itemsQuery.data}
      onRetry={itemsQuery.refetch}
      isEmpty={(d) => d.categories.length === 0}
      emptyDescription="점검 항목이 없습니다"
    >
      {(data) => (
        <div>
          {data.categories.map((cat) => (
            <Card key={cat.questionNum} title={cat.title} style={{ marginBottom: 12 }} size="small">
              <Space direction="vertical" style={{ width: '100%' }} size={16}>
                {cat.items.map((it) => (
                  <div key={it.seqNum}>
                    <Typography.Text style={{ display: 'block', marginBottom: 6 }}>
                      {it.contents}
                    </Typography.Text>
                    <Radio.Group
                      optionType="button"
                      buttonStyle="solid"
                      options={cat.options ?? DEFAULT_OPTIONS}
                      value={answers[it.seqNum]}
                      onChange={(e) =>
                        setAnswers((prev) => ({ ...prev, [it.seqNum]: e.target.value }))
                      }
                    />
                  </div>
                ))}
              </Space>
            </Card>
          ))}
          <Button
            type="primary"
            block
            size="large"
            loading={mutation.isPending}
            onClick={onSubmit}
          >
            안전점검 제출
          </Button>
        </div>
      )}
    </QueryBoundary>
    </>
  );
}
