import { type ReactNode } from 'react';
import { Alert, DatePicker, Form, Input, InputNumber } from 'antd';
import { type Dayjs } from 'dayjs';
import { type SapOutboundTestKind } from '@/api/admin/sapOutboundTest';
import BatchDateForm from './BatchDateForm';

/**
 * SAP outbound 테스트 sender 설정 정의.
 *
 * SAP 연동 페이지(`SapIntegrationPage`)의 통합 '테스트' 탭(`SapOutboundTestTab`)과
 * Outbound 인터페이스별 카탈로그 상세 탭(`SapOutboundCatalogDetail`)이 동일한 폼 /
 * 미리보기 / 실송신 로직을 공유하기 위해 본 설정을 단일 출처로 둔다. 카탈로그 상세에서는
 * `interfaceId` 로 본 설정을 역조회한다.
 */
export type SenderCardConfig = {
  kind: SapOutboundTestKind;
  /**
   * SAP outbound 카탈로그(`SapOutboundCatalogItem.interfaceId`)와 매칭되는 SAP service path
   * (`SDxxxxx`). backend `SapConstants.SAP_INTERFACE_*` 와 동일 값을 가지며, 카탈로그 상세
   * 탭에서 `item.interfaceId` 로 본 설정을 역조회해 메타/송신 폼을 렌더링한다.
   * 카탈로그 interfaceId 와 테스트 API 의 kind 는 별도 네임스페이스라 본 필드로 연결한다.
   */
  interfaceId: string;
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

/**
 * 트리거 분류별 '실송신' 시 흔적이 남는 위치 안내.
 *
 * backend `OutboundTriggerType` 별 실제 적재 대상과 일치시킨다.
 * - REALTIME: 동기 호출 후 응답만 반환 — sap_outbox / sap_outbound_log 어디에도 적재하지 않는다
 *   (호출 메타는 external_api_log 에만 1건 남는다).
 * - OUTBOX: sap_outbox 큐에 INSERT, 비동기 워커가 이후 송신.
 * - BATCH: sap_outbound_log 에 송신 결과 적재.
 */
export const TRIGGER_SEND_EFFECT: Record<SenderCardConfig['triggerTag'], string> = {
  REALTIME: '동기 호출 후 응답만 반환 — sap_outbox / sap_outbound_log 에 적재하지 않습니다.',
  OUTBOX: 'sap_outbox 큐에 INSERT 후 SapOutboxWorker 가 비동기 송신합니다.',
  BATCH: 'sap_outbound_log 에 송신 결과가 적재됩니다.',
};

export const SENDER_CONFIGS: SenderCardConfig[] = [
  {
    kind: 'loan-inquiry',
    interfaceId: 'SD03040',
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
    interfaceId: 'SD03052',
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
    kind: 'inventory-search',
    interfaceId: 'SD03070',
    title: '재고 조회 (InventorySearch)',
    tabLabel: '재고 조회',
    description:
      '거래처 SAP 코드 + 제품 코드 목록 + 납기일로 SAP 재고/공급제한/환산수량/발주단위를 동기 조회합니다. 실제 SAP 호출 발생 (조회만 — 운영 데이터 변경 없음).',
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
        <Form.Item
          label="productCodes (콤마 또는 줄바꿈 구분)"
          required
          help="요청 라인의 제품 코드 전체. 예: 1000123, 1000456"
        >
          <Input.TextArea
            rows={3}
            placeholder="1000123, 1000456"
            value={(state.productCodesRaw as string) ?? ''}
            onChange={(e) => update({ productCodesRaw: e.target.value })}
          />
        </Form.Item>
        <Form.Item label="deliveryDate (납기 요청일, 기본 = 오늘)">
          <DatePicker
            style={{ width: '100%' }}
            value={state.deliveryDate as Dayjs | undefined}
            onChange={(v) => update({ deliveryDate: v ?? undefined })}
          />
        </Form.Item>
      </Form>
    ),
    toBody: (state) => {
      const externalKey = (state.externalKey as string | undefined)?.trim();
      if (!externalKey) throw new Error('externalKey 를 입력하세요');
      const raw = ((state.productCodesRaw as string) ?? '').trim();
      const productCodes = raw
        ? raw
            .split(/[,\n]/)
            .map((s) => s.trim())
            .filter((s) => s.length > 0)
        : [];
      if (productCodes.length === 0) throw new Error('productCodes 를 1개 이상 입력하세요');
      const body: Record<string, unknown> = { externalKey, productCodes };
      const d = state.deliveryDate as Dayjs | undefined;
      if (d) body.deliveryDate = d.format('YYYY-MM-DD');
      return body;
    },
  },
  {
    kind: 'order-request-cancel',
    interfaceId: 'SD03051',
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
    interfaceId: 'SD03050',
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
    interfaceId: 'SD03130',
    title: '여사원일정 스케줄 배치 (TeamMemberSchedule)',
    tabLabel: '여사원일정 스케줄',
    description:
      '특정 날짜의 여사원일정 페이지 1건 (page-size 행) 을 SAP 으로 송신. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => (
      <BatchDateForm
        state={state}
        update={update}
        criteria={
          <>
            근무형태가 <b>‘근무’</b> 인 여사원 일정 중, 아래 두 가지에 해당하는 행을 대상으로 합니다.
            <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
              <li>
                <b>당일분</b> — 근무일이 기준일(targetDate)인 일정. 출퇴근 로그 연결 여부와 무관하게
                모두 전송합니다.
              </li>
              <li>
                <b>전일 보정분</b> — 근무일이 기준일의 <b>하루 전</b>이면서 <b>출퇴근 로그가 연결된</b>{' '}
                일정. 전일 마감 후 확정된 2차 근무형태(WorkingCategory4)를 채워 재전송합니다.
              </li>
            </ul>
            연차·대휴 등 ‘근무’ 가 아닌 일정은 제외됩니다. 사원·거래처 식별값은 일정 자신의 정보를
            사용하며, 결과는 sap_outbound_log 에 적재됩니다.
          </>
        }
      />
    ),
    toBody: (state) => batchDateToBody(state),
  },
  {
    kind: 'attendance-single',
    interfaceId: 'SD03130',
    title: '여사원일정 스케줄 단건 (TeamMemberSchedule)',
    tabLabel: '여사원일정 단건',
    description:
      '여사원 일정 1건(scheduleId)을 골라 SAP 으로 송신합니다. 같은 날짜의 다른 일정은 포함되지 않습니다. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item label="scheduleId (team_member_schedule.id)" required>
          <InputNumber
            style={{ width: '100%' }}
            min={1}
            value={state.scheduleId as number | undefined}
            onChange={(v) => update({ scheduleId: v ?? undefined })}
          />
        </Form.Item>
        <Form.Item
          label="referenceDate (기준일 — 비우면 일정의 근무일 사용)"
          help="전일 보정분(WorkingCategory4 채움)으로 나갔던 payload 를 재현하려면 근무일 + 1일 로 지정하세요. 당일분은 비워두면 됩니다."
        >
          <DatePicker
            style={{ width: '100%' }}
            value={state.referenceDate as Dayjs | undefined}
            onChange={(v) => update({ referenceDate: v ?? undefined })}
          />
        </Form.Item>
        <Alert
          showIcon
          type="info"
          message="근무형태·거래처 등 식별값은 선택한 일정 자신의 정보를 사용합니다. batch 의 근무형태 필터(‘근무’)나 날짜 조건은 적용하지 않고 id 로만 특정합니다."
        />
      </Form>
    ),
    toBody: (state) => {
      const scheduleId = state.scheduleId as number | undefined;
      if (!scheduleId) throw new Error('scheduleId 를 입력하세요');
      const body: Record<string, unknown> = { scheduleId };
      const d = state.referenceDate as Dayjs | undefined;
      if (d) body.referenceDate = d.format('YYYY-MM-DD');
      return body;
    },
  },
  {
    kind: 'display-master',
    interfaceId: 'SD03131',
    title: '여사원 진열마스터 스케줄 배치 (TeamMemberMasterSchedule)',
    tabLabel: '여사원 진열마스터 스케줄',
    description:
      '특정 날짜의 진열사원 일정 마스터 페이지 1건을 SAP 으로 송신. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => <BatchDateForm state={state} update={update} />,
    toBody: (state) => batchDateToBody(state),
  },
  {
    kind: 'ppt-master',
    interfaceId: 'SD03300',
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

function batchDateToBody(state: Record<string, unknown>): Record<string, unknown> {
  const d = state.targetDate as Dayjs | undefined;
  if (!d) throw new Error('targetDate 를 입력하세요');
  const body: Record<string, unknown> = { targetDate: d.format('YYYY-MM-DD') };
  const size = state.pageSize as number | undefined;
  if (size) body.pageSize = size;
  return body;
}
