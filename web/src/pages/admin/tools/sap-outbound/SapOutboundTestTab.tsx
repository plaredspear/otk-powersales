import { useState, type ReactNode } from 'react';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import {
  previewSapOutbound,
  sendSapOutbound,
  type SapOutboundTestKind,
  type SapOutboundTestPreviewResponse,
  type SapOutboundTestSendResponse,
} from '@/api/admin/sapOutboundTest';

const { Title, Text, Paragraph } = Typography;

type SenderCardConfig = {
  kind: SapOutboundTestKind;
  title: string;
  description: string;
  triggerTag: 'BATCH' | 'REALTIME' | 'OUTBOX';
  renderForm: (
    state: Record<string, unknown>,
    update: (patch: Record<string, unknown>) => void,
  ) => ReactNode;
  /** 폼 state → request body 변환. throw 시 message.warning 으로 표시 */
  toBody: (state: Record<string, unknown>) => Record<string, unknown>;
};

const TRIGGER_TAG_COLOR: Record<SenderCardConfig['triggerTag'], string> = {
  BATCH: 'blue',
  REALTIME: 'green',
  OUTBOX: 'purple',
};

const SENDER_CONFIGS: SenderCardConfig[] = [
  {
    kind: 'loan-inquiry',
    title: '거래처 여신 한도 조회 (LoanInquiry)',
    description: '거래처 1건의 여신 한도를 SAP 에서 동기 조회합니다. 실제 SAP 호출 발생.',
    triggerTag: 'REALTIME',
    renderForm: (state, update) => (
      <Form layout="vertical">
        <Form.Item
          label="externalKey (account.external_key)"
          required
          help="SF Account.ExternalKey__c ≡ SAP 거래처 코드"
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
    title: '주문 취소 (OrderChange)',
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
    title: '주문 등록 (OrderRequestRegist) — Outbox',
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
    title: '일반 출근 batch (TeamMemberSchedule)',
    description:
      '특정 날짜의 출근 페이지 1건 (page-size 행) 을 SAP 으로 송신. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => (
      <BatchDateForm state={state} update={update} />
    ),
    toBody: (state) => batchDateToBody(state),
  },
  {
    kind: 'display-master',
    title: '진열 마스터 batch (TeamMemberMasterSchedule)',
    description:
      '특정 날짜의 진열사원 일정 마스터 페이지 1건을 SAP 으로 송신. sap_outbound_log 적재됨.',
    triggerTag: 'BATCH',
    renderForm: (state, update) => (
      <BatchDateForm state={state} update={update} />
    ),
    toBody: (state) => batchDateToBody(state),
  },
  {
    kind: 'ppt-master',
    title: '전문행사조 마스터 batch (SD03300)',
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

type CardState = {
  form: Record<string, unknown>;
  preview: SapOutboundTestPreviewResponse | null;
  sendResult: SapOutboundTestSendResponse | null;
  loadingPreview: boolean;
  loadingSend: boolean;
};

function initialState(): CardState {
  return {
    form: { targetDate: dayjs() },
    preview: null,
    sendResult: null,
    loadingPreview: false,
    loadingSend: false,
  };
}

export default function SapOutboundTestTab() {
  const [selectedKind, setSelectedKind] = useState<SapOutboundTestKind>(
    SENDER_CONFIGS[0].kind,
  );
  const [states, setStates] = useState<Record<SapOutboundTestKind, CardState>>(() => {
    const init = {} as Record<SapOutboundTestKind, CardState>;
    SENDER_CONFIGS.forEach((c) => {
      init[c.kind] = initialState();
    });
    return init;
  });
  const [confirmSend, setConfirmSend] = useState<SapOutboundTestKind | null>(null);

  const config = SENDER_CONFIGS.find((c) => c.kind === selectedKind)!;
  const state = states[selectedKind];

  const updateForm = (patch: Record<string, unknown>) => {
    setStates((prev) => ({
      ...prev,
      [selectedKind]: { ...prev[selectedKind], form: { ...prev[selectedKind].form, ...patch } },
    }));
  };

  const setCard = (kind: SapOutboundTestKind, patch: Partial<CardState>) => {
    setStates((prev) => ({ ...prev, [kind]: { ...prev[kind], ...patch } }));
  };

  const onPreview = async () => {
    let body: Record<string, unknown>;
    try {
      body = config.toBody(state.form);
    } catch (e) {
      message.warning((e as Error).message);
      return;
    }
    setCard(selectedKind, { loadingPreview: true, preview: null });
    try {
      const res = await previewSapOutbound(selectedKind, body);
      setCard(selectedKind, { preview: res });
    } catch (e) {
      message.error(`미리보기 실패: ${(e as Error).message}`);
    } finally {
      setCard(selectedKind, { loadingPreview: false });
    }
  };

  const onSend = async () => {
    let body: Record<string, unknown>;
    try {
      body = config.toBody(state.form);
    } catch (e) {
      message.warning((e as Error).message);
      return;
    }
    setCard(selectedKind, { loadingSend: true, sendResult: null });
    try {
      const res = await sendSapOutbound(selectedKind, body);
      setCard(selectedKind, { sendResult: res });
      if (res.success) {
        message.success(res.message);
      } else {
        message.error(res.message);
      }
    } catch (e) {
      message.error(`송신 실패: ${(e as Error).message}`);
    } finally {
      setCard(selectedKind, { loadingSend: false });
      setConfirmSend(null);
    }
  };

  return (
    <div>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="이 페이지는 실제 SAP 시스템으로 송신을 트리거합니다."
        description="SYSTEM_ADMIN 권한 필요. '실송신' 버튼은 현재 환경의 SAP REST Adapter 로 호출이 전송됩니다 (sap_outbound_log / sap_outbox 적재됨). 페이로드 형식 확인만 필요하면 '미리보기' 만 사용하세요."
      />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Radio.Group
          value={selectedKind}
          onChange={(e) => setSelectedKind(e.target.value as SapOutboundTestKind)}
          buttonStyle="solid"
        >
          <Space wrap>
            {SENDER_CONFIGS.map((c) => (
              <Tooltip key={c.kind} title={c.description}>
                <Radio.Button value={c.kind}>
                  <Tag color={TRIGGER_TAG_COLOR[c.triggerTag]} style={{ marginRight: 6 }}>
                    {c.triggerTag}
                  </Tag>
                  {c.title.split(' (')[0]}
                </Radio.Button>
              </Tooltip>
            ))}
          </Space>
        </Radio.Group>
      </Card>

      <Card
        title={
          <Space>
            <Tag color={TRIGGER_TAG_COLOR[config.triggerTag]}>{config.triggerTag}</Tag>
            <span>{config.title}</span>
          </Space>
        }
        extra={
          <Space>
            <Button onClick={onPreview} loading={state.loadingPreview}>
              미리보기
            </Button>
            <Button
              type="primary"
              danger
              onClick={() => setConfirmSend(selectedKind)}
              loading={state.loadingSend}
            >
              실송신
            </Button>
          </Space>
        }
      >
        <Paragraph type="secondary">{config.description}</Paragraph>
        {config.renderForm(state.form, updateForm)}

        {state.preview && (
          <>
            <Title level={5} style={{ marginTop: 24 }}>
              미리보기 결과
            </Title>
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="interfaceId">
                <code>{state.preview.interfaceId}</code>
              </Descriptions.Item>
              <Descriptions.Item label="endpointPath">
                <code>{state.preview.endpointPath}</code>
              </Descriptions.Item>
              <Descriptions.Item label="summary">{state.preview.summary}</Descriptions.Item>
            </Descriptions>
            <Title level={5} style={{ marginTop: 16 }}>
              payload (송신 직전 JSON)
            </Title>
            <pre
              style={{
                background: '#0d1117',
                color: '#c9d1d9',
                padding: 12,
                borderRadius: 6,
                maxHeight: 400,
                overflow: 'auto',
                fontSize: 12,
              }}
            >
              {JSON.stringify(state.preview.payload, null, 2)}
            </pre>
          </>
        )}

        {state.sendResult && (
          <>
            <Title level={5} style={{ marginTop: 24 }}>
              실송신 결과
            </Title>
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="success">
                {state.sendResult.success ? (
                  <Tag color="green">SUCCESS</Tag>
                ) : (
                  <Tag color="red">FAIL</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="message">{state.sendResult.message}</Descriptions.Item>
              {state.sendResult.sapOutboundLogId && (
                <Descriptions.Item label="sap_outbound_log.id">
                  {state.sendResult.sapOutboundLogId}
                </Descriptions.Item>
              )}
              {state.sendResult.sapOutboxId && (
                <Descriptions.Item label="sap_outbox.id">
                  {state.sendResult.sapOutboxId}
                </Descriptions.Item>
              )}
            </Descriptions>
            {state.sendResult.result !== null && state.sendResult.result !== undefined && (
              <>
                <Title level={5} style={{ marginTop: 16 }}>
                  raw result
                </Title>
                <pre
                  style={{
                    background: '#0d1117',
                    color: '#c9d1d9',
                    padding: 12,
                    borderRadius: 6,
                    maxHeight: 400,
                    overflow: 'auto',
                    fontSize: 12,
                  }}
                >
                  {JSON.stringify(state.sendResult.result, null, 2)}
                </pre>
              </>
            )}
            <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
              상세 로그는 '호출 이력' 또는 '대기 중 (Outbox)' 탭에서 확인할 수 있습니다.
            </Text>
          </>
        )}
      </Card>

      <Modal
        open={confirmSend !== null}
        title="실제 SAP 송신 확인"
        okText="송신"
        okButtonProps={{ danger: true }}
        cancelText="취소"
        onCancel={() => setConfirmSend(null)}
        onOk={onSend}
        confirmLoading={state.loadingSend}
      >
        <Paragraph>
          현재 환경의 <Text strong>SAP REST Adapter</Text> 로 실제 호출을 전송합니다.
        </Paragraph>
        <Paragraph type="secondary">
          interfaceId: <code>{config.kind}</code> · 트리거 분류:{' '}
          <Tag color={TRIGGER_TAG_COLOR[config.triggerTag]}>{config.triggerTag}</Tag>
        </Paragraph>
        <Paragraph type="warning">
          실송신은 <Text code>sap_outbound_log</Text> /{' '}
          <Text code>sap_outbox</Text> 에 흔적을 남기며, SAP 측 운영 데이터에도 영향을 줄 수 있습니다.
        </Paragraph>
      </Modal>
    </div>
  );
}
