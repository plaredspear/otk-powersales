import { useState } from 'react';
import { Alert, Button, Card, Empty, Input, Modal, Space, Switch, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import { apiErrorMessage as errorMessage } from '@/lib/apiErrorMessage';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addFeatureToggleExemptEmployee,
  fetchFeatureToggles,
  removeFeatureToggleExemptEmployee,
  updateFeatureToggle,
  type FeatureToggleExemptEmployee,
  type FeatureToggleItem,
  type FeatureToggleListResponse,
} from '@/api/admin/featureToggle';

const { Paragraph, Text } = Typography;
const { TextArea } = Input;

const QUERY_KEY = ['admin', 'tools', 'feature-toggles'] as const;

/** "홍길동(12345678)" — 사원이 삭제되어 이름을 못 찾으면 사번만 표시. */
function exemptLabel(employee: FeatureToggleExemptEmployee): string {
  return employee.name ? `${employee.name}(${employee.employeeCode})` : employee.employeeCode;
}

/**
 * 개발자 도구 > 대시보드 > 기능 활성화.
 *
 * 제품 클레임 / 물류 클레임 / 주문 등록 API 를 런타임에 on/off 한다. 비활성화 시 관리자가 입력한
 * 사유 문구가 모바일 차단 응답에 노출된다. 상태는 Redis 에 지속 저장되어 앱 재시작 후에도 유지된다.
 * 시스템 관리자 전용 (백엔드 가드) — 비관리자는 API 403 으로 차단된다.
 *
 * 기능별 **예외 사번** 을 등록하면 비활성 상태에서도 해당 사원만 등록할 수 있다 (긴급 대행 등).
 * 예외 목록은 활성/비활성 전환과 무관하게 유지되므로, 재활성화 후에도 다시 입력할 필요가 없다.
 */
export default function FeatureToggleSection() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery<FeatureToggleListResponse>({
    queryKey: QUERY_KEY,
    queryFn: fetchFeatureToggles,
  });

  // 비활성화 사유 입력 모달 상태.
  const [reasonModal, setReasonModal] = useState<{ item: FeatureToggleItem } | null>(null);
  const [reasonInput, setReasonInput] = useState('');

  // 예외 사번 관리 모달 — 목록이 갱신되면 그대로 반영되도록 code 만 들고 있고 항목은 조회 결과에서 찾는다.
  const [exemptModalCode, setExemptModalCode] = useState<string | null>(null);
  const [exemptInput, setExemptInput] = useState('');
  const exemptModalItem = data?.features.find((f) => f.code === exemptModalCode) ?? null;

  const updateMutation = useMutation({
    mutationFn: updateFeatureToggle,
    onSuccess: (result: FeatureToggleItem) => {
      message.success(
        `${result.label} 기능이 ${result.enabled ? '활성화' : '비활성화'}되었습니다`,
      );
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: unknown) => {
      message.error(errorMessage(err, '기능 활성화 변경에 실패했습니다'));
    },
  });

  const addExemptMutation = useMutation({
    mutationFn: addFeatureToggleExemptEmployee,
    onSuccess: () => {
      message.success('예외 사번이 추가되었습니다');
      setExemptInput('');
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: unknown) => {
      message.error(errorMessage(err, '예외 사번 추가에 실패했습니다'));
    },
  });

  const removeExemptMutation = useMutation({
    mutationFn: removeFeatureToggleExemptEmployee,
    onSuccess: () => {
      message.success('예외 사번이 삭제되었습니다');
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: unknown) => {
      message.error(errorMessage(err, '예외 사번 삭제에 실패했습니다'));
    },
  });

  const handleToggle = (item: FeatureToggleItem, checked: boolean) => {
    if (checked) {
      // 활성화는 즉시 반영 (사유 불필요).
      updateMutation.mutate({ code: item.code, enabled: true, reason: null });
    } else {
      // 비활성화는 사유 입력 모달을 먼저 띄운다.
      setReasonInput('');
      setReasonModal({ item });
    }
  };

  const confirmDisable = () => {
    if (!reasonModal) return;
    updateMutation.mutate(
      { code: reasonModal.item.code, enabled: false, reason: reasonInput.trim() || null },
      { onSettled: () => setReasonModal(null) },
    );
  };

  const openExemptModal = (item: FeatureToggleItem) => {
    setExemptInput('');
    setExemptModalCode(item.code);
  };

  const submitExempt = () => {
    const employeeCode = exemptInput.trim();
    if (!exemptModalItem || !employeeCode) return;
    addExemptMutation.mutate({ code: exemptModalItem.code, employeeCode });
  };

  const columns: ColumnsType<FeatureToggleItem> = [
    {
      title: '기능',
      dataIndex: 'label',
      key: 'label',
      width: 240,
      render: (label: string) => <Text strong>{label}</Text>,
    },
    {
      title: '상태',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 120,
      render: (enabled: boolean) =>
        enabled ? <Tag color="green">활성</Tag> : <Tag color="red">비활성</Tag>,
    },
    {
      title: '비활성 사유',
      dataIndex: 'reason',
      key: 'reason',
      render: (reason: string | null) =>
        reason ? <Text>{reason}</Text> : <Text type="secondary">-</Text>,
    },
    {
      title: '예외 사번',
      dataIndex: 'exemptEmployees',
      key: 'exemptEmployees',
      width: 320,
      render: (exemptEmployees: FeatureToggleExemptEmployee[], record: FeatureToggleItem) => (
        <Space size={4} wrap>
          {exemptEmployees.length === 0 ? (
            <Text type="secondary">-</Text>
          ) : (
            <>
              {exemptEmployees.slice(0, 3).map((employee) => (
                <Tag key={employee.employeeCode} color="blue">
                  {exemptLabel(employee)}
                </Tag>
              ))}
              {exemptEmployees.length > 3 && (
                <Text type="secondary">외 {exemptEmployees.length - 3}명</Text>
              )}
            </>
          )}
          <Button type="link" size="small" onClick={() => openExemptModal(record)}>
            관리
          </Button>
        </Space>
      ),
    },
    {
      title: '활성화',
      key: 'action',
      width: 140,
      render: (_: unknown, record: FeatureToggleItem) => (
        <Switch
          checked={record.enabled}
          loading={updateMutation.isPending}
          onChange={(checked) => handleToggle(record, checked)}
          checkedChildren="활성"
          unCheckedChildren="비활성"
        />
      ),
    },
  ];

  return (
    <>
      <Paragraph type="secondary">
        등록 기능(제품 클레임 · 물류 클레임 · 주문)을 일시적으로 중지하거나 재개합니다. 비활성화하면
        해당 등록 API 가 즉시 차단되며, 입력한 사유 문구가 모바일 앱에 안내됩니다. 검증·조회 기준을
        바꾸는 항목(예: 출근등록 일정 소유자/일자 검증, 주문서 거래처 진열마스터 기준)은 차단이 아니라
        <Text strong> 활성 = 신규 동작 / 비활성 = 이전 동작</Text> 으로 전환됩니다.
        예외 사번으로 등록한 사원은 비활성 상태에서도 해당 기능(또는 신규 동작)이 유지됩니다.
      </Paragraph>

      <Alert
        type="info"
        style={{ marginBottom: 16 }}
        message="변경 즉시 반영되며 서버 재시작 후에도 상태가 유지됩니다(Redis 저장). 다시 활성화하기 전까지 해당 기능의 신규 등록은 예외 사번을 제외하고 차단됩니다."
      />

      {isError && (
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message="기능 활성화 상태 조회에 실패했습니다. 시스템 관리자 권한이 필요합니다."
        />
      )}

      <Card size="small">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <ResizableTable<FeatureToggleItem>
            rowKey="code"
            columns={columns}
            dataSource={data?.features ?? []}
            loading={isLoading}
            pagination={false}
            locale={listTableLocale()}
          />
        </Space>
      </Card>

      <Modal
        title={reasonModal ? `${reasonModal.item.label} 비활성화` : ''}
        open={reasonModal !== null}
        onOk={confirmDisable}
        onCancel={() => setReasonModal(null)}
        okText="비활성화"
        okButtonProps={{ danger: true, loading: updateMutation.isPending }}
        cancelText="취소"
      >
        <Paragraph type="secondary">
          비활성화 사유를 입력하세요. 이 문구는 모바일에서 등록 시도 시 안내 메시지로 노출됩니다.
          (선택 — 미입력 시 기본 안내 문구가 표시됩니다.)
        </Paragraph>
        <TextArea
          rows={3}
          maxLength={200}
          showCount
          placeholder="예: 시스템 점검으로 12시까지 클레임 등록이 중지됩니다."
          value={reasonInput}
          onChange={(e) => setReasonInput(e.target.value)}
        />
      </Modal>

      <Modal
        title={exemptModalItem ? `${exemptModalItem.label} 예외 사번` : ''}
        open={exemptModalItem !== null}
        onCancel={() => setExemptModalCode(null)}
        footer={
          <Button onClick={() => setExemptModalCode(null)}>닫기</Button>
        }
      >
        <Paragraph type="secondary">
          이 기능이 비활성이어도 아래 사번의 사원은 등록할 수 있습니다. 예외 목록은 기능별로
          독립이며, 다시 활성화해도 지워지지 않습니다.
        </Paragraph>

        <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
          <Input
            placeholder="사번 입력 (예: 12345678)"
            value={exemptInput}
            maxLength={100}
            onChange={(e) => setExemptInput(e.target.value)}
            onPressEnter={submitExempt}
          />
          <Button
            type="primary"
            loading={addExemptMutation.isPending}
            disabled={!exemptInput.trim()}
            onClick={submitExempt}
          >
            추가
          </Button>
        </Space.Compact>

        {exemptModalItem && exemptModalItem.exemptEmployees.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="등록된 예외 사번이 없습니다"
          />
        ) : (
          <Space size={4} wrap>
            {exemptModalItem?.exemptEmployees.map((employee) => (
              <Tag
                key={employee.employeeCode}
                color="blue"
                closable
                onClose={(e) => {
                  e.preventDefault();
                  if (removeExemptMutation.isPending) return;
                  removeExemptMutation.mutate({
                    code: exemptModalItem.code,
                    employeeCode: employee.employeeCode,
                  });
                }}
              >
                {exemptLabel(employee)}
              </Tag>
            ))}
          </Space>
        )}
      </Modal>
    </>
  );
}
