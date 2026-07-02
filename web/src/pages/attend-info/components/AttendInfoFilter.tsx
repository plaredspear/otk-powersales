import { useState } from 'react';
import { Button, DatePicker, Form, Input, Select, Space } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { ATTEND_TYPE_OPTIONS, type FetchAttendInfoParams } from '@/api/attendInfo';

const { RangePicker } = DatePicker;

interface AttendInfoFilterProps {
  onChange: (filter: FetchAttendInfoParams) => void;
}

interface FilterFormValues {
  employeeCode?: string;
  attendType?: string;
  status?: 'N' | 'Y' | '';
  range?: [Dayjs, Dayjs];
  keyword?: string;
}

export default function AttendInfoFilter({ onChange }: AttendInfoFilterProps) {
  const [form] = Form.useForm<FilterFormValues>();
  const [initialRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(30, 'day'), dayjs()]);

  const submit = (values: FilterFormValues) => {
    const params: FetchAttendInfoParams = {
      employeeCode: values.employeeCode?.trim() || undefined,
      attendType: values.attendType || undefined,
      status: values.status || undefined,
      keyword: values.keyword?.trim() || undefined,
    };
    if (values.range && values.range.length === 2) {
      params.startDateFrom = values.range[0].format('YYYY-MM-DD');
      params.startDateTo = values.range[1].format('YYYY-MM-DD');
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
      initialValues={{ status: '', range: initialRange }}
      onFinish={submit}
      style={{ marginBottom: 16 }}
    >
      <Form.Item name="employeeCode" label="사원번호">
        <Input placeholder="정확 일치" allowClear style={{ width: 140 }} />
      </Form.Item>
      <Form.Item name="keyword" label="사원명">
        <Input placeholder="부분 일치" allowClear style={{ width: 140 }} />
      </Form.Item>
      <Form.Item name="attendType" label="근태유형">
        <Select
          allowClear
          placeholder="전체"
          style={{ width: 180 }}
          options={ATTEND_TYPE_OPTIONS.map((o) => ({ value: o.value, label: `${o.value} · ${o.label}` }))}
        />
      </Form.Item>
      <Form.Item name="range" label="시작일">
        <RangePicker />
      </Form.Item>
      <Form.Item name="status" label="상태">
        <Select
          style={{ width: 110 }}
          options={[
            { value: '', label: '전체' },
            { value: 'N', label: 'N (등록)' },
            { value: 'Y', label: 'Y (취소)' },
          ]}
        />
      </Form.Item>
      <Form.Item>
        <Space>
          <Button type="primary" htmlType="submit">
            조회
          </Button>
          <Button onClick={reset}>초기화</Button>
        </Space>
      </Form.Item>
    </Form>
  );
}
