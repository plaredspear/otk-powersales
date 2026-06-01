import { useMemo, useState } from 'react';
import { Calendar, Card, Space, Tag, Typography } from 'antd';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchMonthlySchedule } from '@/api/schedule';
import DetailHeader from '@/components/DetailHeader';
import { LoadingState } from '@/components/PageStates';

/** 내 일정 캘린더 (레거시 mypage/main). 근무일 표시 + 날짜 선택 시 일별 현황. */
export default function MyScheduleCalendarPage() {
  const navigate = useNavigate();
  const [panel, setPanel] = useState<Dayjs>(dayjs());

  const year = panel.year();
  const month = panel.month() + 1;

  const query = useQuery({
    queryKey: ['my-schedule', year, month],
    queryFn: () => fetchMonthlySchedule(year, month),
  });

  const workDayMap = useMemo(() => {
    const map = new Map<string, string | null>();
    query.data?.workDays.forEach((w) => {
      if (w.hasWork) map.set(w.date, w.workingType);
    });
    return map;
  }, [query.data]);

  return (
    <>
      <DetailHeader title="내 일정" />
      {query.data && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Space size="large">
            <span>
              연차 <Tag color="blue">{query.data.annualLeaveCount}</Tag>
            </span>
            <span>
              대체휴무 <Tag color="green">{query.data.substituteHolidayCount}</Tag>
            </span>
          </Space>
        </Card>
      )}
      <Card styles={{ body: { padding: 4 } }}>
        {query.isLoading && <LoadingState />}
        <Calendar
          fullscreen={false}
          onPanelChange={(value) => setPanel(value)}
          onSelect={(value, info) => {
            if (info.source === 'date') navigate(`/mypage/daily/${value.format('YYYY-MM-DD')}`);
          }}
          cellRender={(current, info) => {
            if (info.type !== 'date') return info.originNode;
            const key = current.format('YYYY-MM-DD');
            if (!workDayMap.has(key)) return info.originNode;
            return (
              <div style={{ position: 'relative' }}>
                {info.originNode}
                <span
                  style={{
                    position: 'absolute',
                    bottom: 2,
                    left: '50%',
                    transform: 'translateX(-50%)',
                    width: 6,
                    height: 6,
                    borderRadius: '50%',
                    background: '#1677ff',
                  }}
                />
              </div>
            );
          }}
        />
      </Card>
      <Typography.Paragraph type="secondary" style={{ fontSize: 12, marginTop: 8, textAlign: 'center' }}>
        근무일(파란 점)을 탭하면 일별 현황을 볼 수 있습니다.
      </Typography.Paragraph>
    </>
  );
}
