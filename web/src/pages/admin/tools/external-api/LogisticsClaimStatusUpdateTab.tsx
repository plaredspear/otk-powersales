import { useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Space,
  Table,
  Tag,
  Typography,
  notification,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { testLogisticsClaimMasterSync } from '@/api/claims';
import type {
  LogisticsClaimMasterSyncTestInput,
  LogisticsClaimMasterSyncTestResult,
} from '@/api/claims';

const { Text } = Typography;

const DATE_PICKER_FORMAT = 'YYYY-MM-DD';
/** SF Request Body MOD_DT 형식 (YYYYMMDD). */
const MOD_DT_FORMAT = 'YYYYMMDD';

/**
 * SF `IF_SendLogisticsClaimToPWS` Response 필드 (영문 key ↔ 한글 라벨).
 *
 * PDF "알라딘 물류클레임 마스터 API" 문서의 Response 표 순서를 그대로 따른다. 결과 테이블 컬럼으로 사용한다.
 */
const RESPONSE_FIELDS: { key: string; label: string }[] = [
  // key 철자/대소문자는 알라딘 API 문서 표기를 그대로 따른 것. SF 실제 응답 key 가
  // 문서와 다르면(예: WERK3TEXT2, DKRetailProductId 등) 해당 컬럼이 빈 셀로 표시되므로,
  // 실제 응답 확인 후 철자를 맞춰야 한다.
  { key: 'Name', label: '제안 레코드명' },
  { key: 'DKRetailTitle', label: '제안 제목' },
  { key: 'AccountCode', label: '거래처 코드' },
  { key: 'AccountId', label: '거래처 ID' },
  { key: 'DKRetailProductId', label: '제품 ID' },
  { key: 'ActionNum', label: '조치번호' },
  { key: 'ActionStatus', label: '조치상태' },
  { key: 'ActionManager', label: '조치 담당자' },
  { key: 'LogisticsResponsibility', label: '물류 책임 구분' },
  { key: 'ClaimTypeMeasures', label: '클레임 항목(조치사항)' },
  { key: 'WERK3TEXT2', label: '접수 물류센터' },
  { key: 'username', label: '사용자명' },
  { key: 'empcode', label: '사번' },
  { key: 'ActionContent', label: '조치 내용' },
  { key: 'ClaimDate', label: '등록일시' },
  { key: 'accountname', label: '거래처명' },
  { key: 'productname', label: '제품명' },
  { key: 'prname', label: '제안명' },
  { key: 'title', label: '제목' },
  { key: 'description', label: '상세내용' },
  { key: 'CarNumber', label: '차량번호' },
];

interface FormValues {
  modDt: Dayjs;
}

const PRE_STYLE: React.CSSProperties = {
  background: '#f5f5f5',
  padding: 12,
  borderRadius: 4,
  margin: 0,
  maxHeight: 360,
  overflow: 'auto',
  fontFamily: 'Menlo, Consolas, monospace',
  fontSize: 12,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-all',
};

function prettyPrintJson(raw: string | null): string {
  if (!raw) return '(응답 본문 없음)';
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

/**
 * SF 응답 raw JSON 에서 물류 클레임 마스터 행 배열을 추출한다.
 *
 * SF 응답 포맷이 확정 전이므로 흔한 후보를 순서대로 탐색한다:
 *  - 최상위가 배열인 경우
 *  - { data: [...] } / { LIST: [...] } / { result: [...] } 등 흔한 래퍼 key
 *  - 그 외에는 빈 배열 (raw JSON 만 노출).
 */
function extractRows(raw: string | null): Record<string, unknown>[] {
  if (!raw) return [];
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return [];
  }
  if (Array.isArray(parsed)) return parsed as Record<string, unknown>[];
  if (parsed && typeof parsed === 'object') {
    const obj = parsed as Record<string, unknown>;
    for (const key of ['data', 'DATA', 'list', 'LIST', 'result', 'RESULT', 'items']) {
      const v = obj[key];
      if (Array.isArray(v)) return v as Record<string, unknown>[];
    }
  }
  return [];
}

function cellText(value: unknown): string {
  if (value === null || value === undefined) return '';
  return String(value);
}

/**
 * SF 물류 클레임 상태 업데이트(물류 클레임 마스터 조회) 테스트 탭 (외부 API 테스트 통합 페이지).
 *
 * PDF "알라딘 물류클레임 마스터 API"(`IF_SendLogisticsClaimToPWS`) 계약 정합. 기준 일자(MOD_DT) 하나를 SF 로
 * POST 하면 SF 가 해당 일자 기준으로 변경된 물류 클레임(제안) 마스터 목록을 응답하는 SF → PWS 조회 인터페이스다.
 * 백엔드 `POST /api/v1/admin/logistics-claim-master-sync/test` 를 호출하며, SF 응답을 결과 테이블 + raw JSON
 * 으로 노출한다. 신규 DB 에는 저장하지 않는다. SYSTEM(MODIFY_ALL_DATA) 권한 필요.
 */
export default function LogisticsClaimStatusUpdateTab() {
  const [form] = Form.useForm<FormValues>();
  const [result, setResult] = useState<LogisticsClaimMasterSyncTestResult | null>(
    null,
  );

  const mutation = useMutation<
    LogisticsClaimMasterSyncTestResult,
    Error,
    LogisticsClaimMasterSyncTestInput
  >({
    mutationFn: testLogisticsClaimMasterSync,
  });

  const handleFinish = async (values: FormValues) => {
    try {
      const response = await mutation.mutateAsync({
        modDt: values.modDt.format(MOD_DT_FORMAT),
      });
      setResult(response);
      notification[response.success ? 'success' : 'warning']({
        key: 'logistics-claim-master-sync-test',
        message: response.success
          ? `SF 조회 성공 (RESULT_CODE=${response.resultCode ?? '-'})`
          : `SF 조회 실패 (RESULT_CODE=${response.resultCode ?? '-'})`,
        description: response.resultMsg ?? undefined,
      });
    } catch (err) {
      setResult(null);
      notification.error({
        key: 'logistics-claim-master-sync-test-error',
        message: 'SF IF_SendLogisticsClaimToPWS 호출 실패',
        description: err instanceof Error ? err.message : '잠시 후 다시 시도해주세요.',
      });
    }
  };

  const rows = extractRows(result?.rawResponse ?? null);

  const columns: ColumnsType<Record<string, unknown>> = RESPONSE_FIELDS.map(
    (field) => ({
      title: (
        <Space direction="vertical" size={0}>
          <Text>{field.label}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {field.key}
          </Text>
        </Space>
      ),
      dataIndex: field.key,
      key: field.key,
      width: 160,
      ellipsis: true,
      render: (value: unknown) => cellText(value),
    }),
  );

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="물류 클레임 마스터 조회 (POST /api/v1/admin/logistics-claim-master-sync/test)">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          disabled={mutation.isPending}
        >
          <Form.Item
            label="조회 기준 일자 (MOD_DT)"
            name="modDt"
            rules={[{ required: true, message: '조회 기준 일자는 필수입니다' }]}
            tooltip="SF Request Body MOD_DT (YYYYMMDD). 이 일자 기준으로 변경된 물류 클레임 마스터를 조회합니다."
          >
            <DatePicker style={{ width: 220 }} format={DATE_PICKER_FORMAT} />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={mutation.isPending}>
              SF 조회
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {result && (
        <Card
          title={
            <Space>
              <Text strong>SF 응답</Text>
              <Tag color={result.success ? 'green' : 'red'}>
                RESULT_CODE = {result.resultCode ?? '-'}
              </Tag>
              <Tag color="blue">{rows.length}건</Tag>
            </Space>
          }
        >
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {result.resultMsg && (
              <Text>
                <strong>RESULT_MSG:</strong> {result.resultMsg}
              </Text>
            )}

            {rows.length > 0 && (
              <Table
                rowKey={(_, index) => String(index)}
                columns={columns}
                dataSource={rows}
                size="small"
                scroll={{ x: 'max-content', y: 400 }}
                pagination={{ pageSize: 20, showSizeChanger: true }}
              />
            )}

            <div>
              <Text type="secondary">전송 요청 body (MOD_DT)</Text>
              <pre style={PRE_STYLE}>{prettyPrintJson(result.requestPayload)}</pre>
            </div>
            <div>
              <Text type="secondary">SF 응답 본문 (raw)</Text>
              <pre style={PRE_STYLE}>{prettyPrintJson(result.rawResponse)}</pre>
            </div>
          </Space>
        </Card>
      )}
    </Space>
  );
}
