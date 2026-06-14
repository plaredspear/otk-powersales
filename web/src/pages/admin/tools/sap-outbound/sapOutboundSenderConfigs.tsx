import { type ReactNode } from 'react';
import { Alert, DatePicker, Form, Input, InputNumber } from 'antd';
import { type Dayjs } from 'dayjs';
import { type SapOutboundTestKind } from '@/api/admin/sapOutboundTest';

/**
 * SAP outbound 테스트 sender 설정 정의.
 *
 * SAP Outbound 전용 페이지의 '테스트 송신' 탭(`SapOutboundTestTab`)과
 * 외부 API 테스트 통합 페이지(`ExternalApiTestPage`)의 인터페이스별 개별 탭이
 * 동일한 폼 / 미리보기 / 실송신 로직을 공유하기 위해 본 설정을 단일 출처로 둔다.
 */
export type SenderCardConfig = {
  kind: SapOutboundTestKind;
  title: string;
  description: string;
  triggerTag: 'BATCH' | 'REALTIME' | 'OUTBOX';
  /** 통합 페이지 탭 라벨 (한글) */
  tabLabel: string;
  renderForm: (
    state: Record<string, unknown>,
    update: (patch: Record<string, unknown>) => void,
  ) => ReactNode;
  /** 폼 state → request body 변환. throw 시 message.warning 으로 표시 */
  toBody: (state: Record<string, unknown>) => Record<string, unknown>;
};

export const TRIGGER_TAG_COLOR: Record<SenderCardConfig['triggerTag'], string> = {
  BATCH: 'blue',
  REALTIME: 'green',
  OUTBOX: 'purple',
};

export const SENDER_CONFIGS: SenderCardConfig[] = [
  {
    kind: 'loan-inquiry',
    title: '거래처 여신 한도 조회 (LoanInquiry)',
    tabLabel: '여신 한도 조회',
    description: '거래처 1건의 여신 한도를 SAP 에서 동기 조회합니다. 실제 SAP 호출 발생.',
    triggerTag: 'REALTIME',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item
          label="externalKey (account.external_key)"
          required
          help="Account.externalKey ≡ SAP 거래처 코드"
        >
          <Input
            placeholder="예: 1032619"
            value={(state.externalKey as string) ?? ''}
            onChange={(e) => update({ externalKey: e.target.value })}
          />
        </Form.Item>
      </Form>
    ),
    toBody: (state) => {
      const externalKey = (state.externalKey as string | undefined)?.trim();
      if (!externalKey) throw new Error('externalKey 를 입력하세요');
      return { externalKey };
    },
  },
  {
    kind: 'order-request-detail',
    title: '주문요청 상세 조회 (OrderRequestDetail)',
    tabLabel: '주문요청 상세 조회',
    description: '주문 번호(RequestNumber) 1건의 SAP 상세 라인을 동기 조회합니다.',
    triggerTag: 'REALTIME',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item
          label="requestNumber (order_request.order_request_number)"
          required
          help="SF DKRetail__OrderRequest__c.Name 등가"
        >
          <Input
            placeholder="예: OR000123"
            value={(state.requestNumber as string) ?? ''}
            onChange={(e) => update({ requestNumber: e.target.value })}
          />
        </Form.Item>
      </Form>
    ),
    toBody: (state) => {
      const requestNumber = (state.requestNumber as string | undefined)?.trim();
      if (!requestNumber) throw new Error('requestNumber 를 입력하세요');
      return { requestNumber };
    },
  },
  {
    kind: 'order-request-cancel',
    title: '주문 요청 취소 (OrderChange)',
    tabLabel: '주문 요청 취소',
    description:
      'orderRequestId + orderProductIds 로 SAP 취소 송신. 송신만 수행 — DB 상태 변경은 발생하지 않습니다.',
    triggerTag: 'REALTIME',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item label="orderRequestId" required>
          <InputNumber
            style={{ width: '100%' }}
            min={1}
            value={state.orderRequestId as number | undefined}
            onChange={(v) => update({ orderRequestId: v ?? undefined })}
          />
        </Form.Item>
        <Form.Item
          label="orderProductIds (콤마 구분, 비우면 미취소 라인 전체)"
          help="예: 101,102,103"
        >
          <Input
            placeholder="비우면 전체 미취소 라인"
            value={(state.orderProductIdsRaw as string) ?? ''}
            onChange={(e) => update({ orderProductIdsRaw: e.target.value })}
          />
        </Form.Item>
      </Form>
    ),
    toBody: (state) => {
      const orderRequestId = state.orderRequestId as number | undefined;
      if (!orderRequestId) throw new Error('orderRequestId 를 입력하세요');
      const raw = ((state.orderProductIdsRaw as string) ?? '').trim();
      const orderProductIds = raw
        ? raw
            .split(',')
            .map((s) => Number(s.trim()))
            .filter((n) => Number.isFinite(n) && n > 0)
        : [];
      return { orderRequestId, orderProductIds };
    },
  },
  {
    kind: 'order-request-register',
    title: '주문 요청 등록 (OrderRequestRegist) — Outbox',
    tabLabel: '주문 요청 등록',
    description:
      'orderRequestId 로 sap_outbox 큐에 적재만 수행. 실제 SAP 호출은 SapOutboxWorker 가 비동기로 진행.',
    triggerTag: 'OUTBOX',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item label="orderRequestId" required>
          <InputNumber
            style={{ width: '100%' }}
            min={1}
            value={state.orderRequestId as number | undefined}
            onChange={(v) => update({ orderRequestId: v ?? undefined })}
          />
        </Form.Item>
        <Alert
          showIcon
          type="warning"
          message="실송신 호출은 sap_outbox 행을 신규 INSERT 합니다. SapOutboxWorker 가 곧 폴링하여 실제 SAP 송신을 수행합니다."
        />
      </Form>
    ),
    toBody: (state) => {
      const orderRequestId = state.orderRequestId as number | undefined;
      if (!orderRequestId) throw new Error('orderRequestId 를 입력하세요');
      return { orderRequestId };
    },
  },
  {
    kind: 'attendance',
    title: '근태 SAP전송 batch (TeamMemberSchedule)',
    tabLabel: '근태 SAP전송',
    description:
      '특정 날짜의 출근 페이지 1건 (page-size 행) 을 SAP 으로 송신. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => <BatchDateForm state={state} update={update} />,
    toBody: (state) => batchDateToBody(state),
  },
  {
    kind: 'display-master',
    title: '진열 마스터 batch (TeamMemberMasterSchedule)',
    tabLabel: '진열 마스터',
    description:
      '특정 날짜의 진열사원 일정 마스터 페이지 1건을 SAP 으로 송신. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => <BatchDateForm state={state} update={update} />,
    toBody: (state) => batchDateToBody(state),
  },
  {
    kind: 'ppt-master',
    title: '전문행사조 마스터 batch (SD03300)',
    tabLabel: '전문행사조 마스터',
    description:
      '기준일(target date)이 속한 월의 활성 PPT 마스터 첫 페이지를 SAP 으로 송신.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item label="targetDate (기본 = 오늘)">
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
    ),
    toBody: (state) => {
      const body: Record<string, unknown> = {};
      const d = state.targetDate as Dayjs | undefined;
      if (d) body.targetDate = d.format('YYYY-MM-DD');
      const size = state.pageSize as number | undefined;
      if (size) body.pageSize = size;
      return body;
    },
  },
];

function BatchDateForm({
  state,
  update,
}: {
  state: Record<string, unknown>;
  update: (patch: Record<string, unknown>) => void;
}) {
  return (
    <Form layout="vertical">
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

function batchDateToBody(state: Record<string, unknown>): Record<string, unknown> {
  const d = state.targetDate as Dayjs | undefined;
  if (!d) throw new Error('targetDate 를 입력하세요');
  const body: Record<string, unknown> = { targetDate: d.format('YYYY-MM-DD') };
  const size = state.pageSize as number | undefined;
  if (size) body.pageSize = size;
  return body;
}
