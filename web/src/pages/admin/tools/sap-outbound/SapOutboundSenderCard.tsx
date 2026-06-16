import { useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Modal,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import dayjs from 'dayjs';
import {
  previewSapOutbound,
  sendSapOutbound,
  type SapOutboundTestPreviewResponse,
  type SapOutboundTestSendResponse,
} from '@/api/admin/sapOutboundTest';
import {
  TRIGGER_SEND_EFFECT,
  TRIGGER_TAG_COLOR,
  type SenderCardConfig,
} from './sapOutboundSenderConfigs';

const { Title, Text, Paragraph } = Typography;

/**
 * SAP outbound sender 단일 인터페이스 카드.
 *
 * 하나의 인터페이스(`config.kind`)에 대한 폼 입력 + 미리보기 + 실송신(확인 모달)을
 * 자체 state 로 캡슐화한다. SAP Outbound 전용 페이지의 '테스트 송신' 탭과
 * 외부 API 테스트 통합 페이지의 인터페이스별 개별 탭이 동일하게 재사용한다.
 */
export default function SapOutboundSenderCard({ config }: { config: SenderCardConfig }) {
  const [form, setForm] = useState<Record<string, unknown>>({ targetDate: dayjs() });
  const [preview, setPreview] = useState<SapOutboundTestPreviewResponse | null>(null);
  const [sendResult, setSendResult] = useState<SapOutboundTestSendResponse | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [loadingSend, setLoadingSend] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const updateForm = (patch: Record<string, unknown>) => {
    setForm((prev) => ({ ...prev, ...patch }));
  };

  const onPreview = async () => {
    let body: Record<string, unknown>;
    try {
      body = config.toBody(form);
    } catch (e) {
      message.warning((e as Error).message);
      return;
    }
    setLoadingPreview(true);
    setPreview(null);
    try {
      const res = await previewSapOutbound(config.kind, body);
      setPreview(res);
    } catch (e) {
      message.error(`미리보기 실패: ${(e as Error).message}`);
    } finally {
      setLoadingPreview(false);
    }
  };

  const onSend = async () => {
    let body: Record<string, unknown>;
    try {
      body = config.toBody(form);
    } catch (e) {
      message.warning((e as Error).message);
      return;
    }
    setLoadingSend(true);
    setSendResult(null);
    try {
      const res = await sendSapOutbound(config.kind, body);
      setSendResult(res);
      if (res.success) {
        message.success(res.message);
      } else {
        message.error(res.message);
      }
    } catch (e) {
      message.error(`송신 실패: ${(e as Error).message}`);
    } finally {
      setLoadingSend(false);
      setConfirmOpen(false);
    }
  };

  return (
    <div>
      <Card
        title={
          <Space>
            <Tag color={TRIGGER_TAG_COLOR[config.triggerTag]}>{config.triggerTag}</Tag>
            <span>{config.title}</span>
          </Space>
        }
        extra={
          <Space>
            <Button onClick={onPreview} loading={loadingPreview}>
              미리보기
            </Button>
            <Button
              type="primary"
              danger
              onClick={() => setConfirmOpen(true)}
              loading={loadingSend}
            >
              실송신
            </Button>
          </Space>
        }
      >
        <Paragraph type="secondary">{config.description}</Paragraph>
        {config.renderForm(form, updateForm)}

        {preview && (
          <>
            <Title level={5} style={{ marginTop: 24 }}>
              미리보기 결과
            </Title>
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="interfaceId">
                <code>{preview.interfaceId}</code>
              </Descriptions.Item>
              <Descriptions.Item label="endpointPath">
                <code>{preview.endpointPath}</code>
              </Descriptions.Item>
              <Descriptions.Item label="summary">{preview.summary}</Descriptions.Item>
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
              {JSON.stringify(preview.payload, null, 2)}
            </pre>
          </>
        )}

        {sendResult && (
          <>
            <Title level={5} style={{ marginTop: 24 }}>
              실송신 결과
            </Title>
            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="success">
                {sendResult.success ? (
                  <Tag color="green">SUCCESS</Tag>
                ) : (
                  <Tag color="red">FAIL</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="message">{sendResult.message}</Descriptions.Item>
              {sendResult.sapOutboundLogId && (
                <Descriptions.Item label="sap_outbound_log.id">
                  {sendResult.sapOutboundLogId}
                </Descriptions.Item>
              )}
              {sendResult.sapOutboxId && (
                <Descriptions.Item label="sap_outbox.id">
                  {sendResult.sapOutboxId}
                </Descriptions.Item>
              )}
            </Descriptions>
            {sendResult.result !== null && sendResult.result !== undefined && (
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
                  {JSON.stringify(sendResult.result, null, 2)}
                </pre>
              </>
            )}
            <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
              상세 로그는 SAP 연동 페이지 Outbound 의 '호출 이력' 또는 '대기 중 (Outbox)' 탭에서 확인할 수 있습니다.
            </Text>
          </>
        )}
      </Card>

      <Modal
        open={confirmOpen}
        title="실제 SAP 송신 확인"
        okText="송신"
        okButtonProps={{ danger: true }}
        cancelText="취소"
        onCancel={() => setConfirmOpen(false)}
        onOk={onSend}
        confirmLoading={loadingSend}
      >
        <Paragraph>
          현재 환경의 <Text strong>SAP REST Adapter</Text> 로 실제 호출을 전송합니다.
        </Paragraph>
        <Paragraph type="secondary">
          interfaceId: <code>{config.kind}</code> · 트리거 분류:{' '}
          <Tag color={TRIGGER_TAG_COLOR[config.triggerTag]}>{config.triggerTag}</Tag>
        </Paragraph>
        <Paragraph type="warning">
          {TRIGGER_SEND_EFFECT[config.triggerTag]} SAP 측 운영 데이터에도 영향을 줄 수 있습니다.
        </Paragraph>
      </Modal>
    </div>
  );
}
