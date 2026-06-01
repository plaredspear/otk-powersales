import { Button, Card, DatePicker, Form, Input, Select, App as AntdApp } from 'antd';
import { useQuery, useMutation } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import { fetchTeamMembers, createTeamMemberSchedule } from '@/api/leader';
import DetailHeader from '@/components/DetailHeader';

interface ScheduleForm {
  targetEmployeeId?: number;
  workingDate: Dayjs;
  workingType: string;
  workingCategory2: string;
  workingCategory3: string;
}

/**
 * 여사원 일정 추가 (레거시 employee/addSchedule) — 조장 대리 등록.
 * ⚠️ T1: workingType/workingCategory2/3 의 enum/코드값 미확정 → 자유 입력. 정확한 코드 마스터 확정 필요.
 */
export default function LeaderScheduleCreatePage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const members = useQuery({ queryKey: ['leader-members'], queryFn: fetchTeamMembers });

  const mutation = useMutation({
    mutationFn: (v: ScheduleForm) =>
      createTeamMemberSchedule({
        targetEmployeeId: v.targetEmployeeId,
        workingDate: v.workingDate.format('YYYY-MM-DD'),
        workingType: v.workingType,
        workingCategory2: v.workingCategory2,
        workingCategory3: v.workingCategory3,
      }),
    onSuccess: () => {
      message.success('일정이 등록되었습니다');
      navigate('/leader/daily-status', { replace: true });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '등록에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="일정 추가" />
      <Card styles={{ body: { padding: 16 } }}>
        <Form<ScheduleForm> layout="vertical" requiredMark={false} onFinish={(v) => mutation.mutate(v)}>
          <Form.Item name="targetEmployeeId" label="대상 여사원" rules={[{ required: true, message: '대상을 선택하세요' }]}>
            <Select
              showSearch
              placeholder="여사원 선택"
              loading={members.isLoading}
              optionFilterProp="label"
              options={members.data?.map((m) => ({ value: m.id, label: `${m.name} (${m.employeeCode})` }))}
            />
          </Form.Item>
          <Form.Item name="workingDate" label="근무일" rules={[{ required: true, message: '근무일을 선택하세요' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="workingType" label="근무 유형" rules={[{ required: true, message: '근무 유형을 입력하세요' }]}>
            <Input placeholder="근무 유형 (예: 진열)" />
          </Form.Item>
          <Form.Item name="workingCategory2" label="구분2" rules={[{ required: true, message: '구분2를 입력하세요' }]}>
            <Input placeholder="구분2" />
          </Form.Item>
          <Form.Item name="workingCategory3" label="구분3" rules={[{ required: true, message: '구분3을 입력하세요' }]}>
            <Input placeholder="구분3" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            일정 등록
          </Button>
        </Form>
      </Card>
    </>
  );
}
