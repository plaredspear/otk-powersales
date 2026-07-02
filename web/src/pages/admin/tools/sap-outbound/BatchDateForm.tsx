import { type ReactNode } from 'react';
import { Alert, DatePicker, Form, InputNumber } from 'antd';
import { type Dayjs } from 'dayjs';

/**
 * SAP outbound BATCH 계열 sender 공통 폼 (targetDate + pageSize).
 *
 * `sapOutboundSenderConfigs` 의 BATCH 트리거 sender 들이 공유한다.
 */
export default function BatchDateForm({
  state,
  update,
  criteria,
}: {
  state: Record<string, unknown>;
  update: (patch: Record<string, unknown>) => void;
  /** 조회 대상 선별 조건을 자연어로 설명하는 안내(옵셔널). 폼 상단에 노출된다. */
  criteria?: ReactNode;
}) {
  return (
    <Form layout="vertical">
      {criteria && (
        <Form.Item>
          <Alert showIcon type="info" message="조회 조건" description={criteria} />
        </Form.Item>
      )}
      <Form.Item label="targetDate" required>
        <DatePicker
          style={{ width: '100%' }}
          value={state.targetDate as Dayjs | undefined}
          onChange={(v) => update({ targetDate: v ?? undefined })}
        />
      </Form.Item>
      <Form.Item label="pageSize (기본 100)">
        <InputNumber
          style={{ width: '100%' }}
          min={1}
          max={1000}
          value={state.pageSize as number | undefined}
          onChange={(v) => update({ pageSize: v ?? undefined })}
        />
      </Form.Item>
    </Form>
  );
}
