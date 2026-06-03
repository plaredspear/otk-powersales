import { useState } from 'react';
import { Button, DatePicker, Form, Input, Select, Space } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import {
  ATTENDANCE_TYPE_OPTIONS,
  type AttendanceTypeCode,
  type FetchAttendanceLogParams,
} from '@/api/attendanceLog';

const { RangePicker } = DatePicker;

interface AttendanceLogFilterProps {
  onChange: (filter: FetchAttendanceLogParams) => void;
  /** URL 복원용 초기값 (마운트 시 1회 적용). */
  initialKeyword?: string;
  initialAttendanceType?: AttendanceTypeCode | '';
  initialDateFrom?: string;
  initialDateTo?: string;
}

interface FilterFormValues {
  keyword?: string;
  attendanceType?: AttendanceTypeCode | '';
  range?: [Dayjs, Dayjs];
}

export default function AttendanceLogFilter({
  onChange,
  initialKeyword,
  initialAttendanceType,
  initialDateFrom,
  initialDateTo,
}: AttendanceLogFilterProps) {
  const [form] = Form.useForm<FilterFormValues>();
  // URL 의 from/to 가 있으면 그 값으로, 없으면 기본 (최근 30일) 으로 복원.
  const [initialRange] = useState<[Dayjs, Dayjs]>(() =>
    initialDateFrom && initialDateTo
      ? [dayjs(initialDateFrom), dayjs(initialDateTo)]
      : [dayjs().subtract(30, 'day'), dayjs()],
  );

  const submit = (values: FilterFormValues) => {
    const params: FetchAttendanceLogParams = {
      keyword: values.keyword?.trim() || undefined,
      attendanceType: values.attendanceType || undefined,
    };
    if (values.range && values.range.length === 2) {
      params.attendanceDateFrom = values.range[0].format('YYYY-MM-DD');
      params.attendanceDateTo = values.range[1].format('YYYY-MM-DD');
    }
    onChange(params);
  };

  const reset = () => {
    form.resetFields();
    onChange({});
  };

  return (
    <Form
      form={form}
      layout="inline"
      initialValues={{
        keyword: initialKeyword ?? '',
        attendanceType: initialAttendanceType ?? '',
        range: initialRange,
      }}
      onFinish={submit}
      style={{ marginBottom: 16 }}
    >
      <Form.Item name="keyword" label="사원">
        <Input placeholder="사원명/사번 부분 일치" allowClear style={{ width: 200 }} />
      </Form.Item>
      <Form.Item name="attendanceType" label="출근 종류">
        <Select
          style={{ width: 130 }}
          options={[{ value: '', label: '전체' }, ...ATTENDANCE_TYPE_OPTIONS]}
        />
      </Form.Item>
      <Form.Item name="range" label="출근일자">
        <RangePicker />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" htmlType="submit">
            검색
          </Button>
          <Button onClick={reset}>초기화</Button>
        </Space>
      </Form.Item>
    </Form>
  );
}
