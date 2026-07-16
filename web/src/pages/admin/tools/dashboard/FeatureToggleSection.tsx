import { useState } from 'react';
import { Alert, Card, Input, Modal, Space, Switch, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchFeatureToggles,
  updateFeatureToggle,
  type FeatureToggleItem,
  type FeatureToggleListResponse,
} from '@/api/admin/featureToggle';

const { Paragraph, Text } = Typography;
const { TextArea } = Input;

const QUERY_KEY = ['admin', 'tools', 'feature-toggles'] as const;

/**
 * 개발자 도구 > 대시보드 > 기능 활성화.
 *
 * 제품 클레임 / 물류 클레임 / 주문 등록 API 를 런타임에 on/off 한다. 비활성화 시 관리자가 입력한
 * 사유 문구가 모바일 차단 응답에 노출된다. 상태는 Redis 에 지속 저장되어 앱 재시작 후에도 유지된다.
 * 시스템 관리자 전용 (백엔드 가드) — 비관리자는 API 403 으로 차단된다.
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

  const updateMutation = useMutation({
    mutationFn: updateFeatureToggle,
    onSuccess: (result: FeatureToggleItem) => {
      message.success(
        `${result.label} 기능이 ${result.enabled ? '활성화' : '비활성화'}되었습니다`,
      );
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: Error) => {
      message.error(err.message || '기능 활성화 변경에 실패했습니다');
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
        제품 클레임 등록 · 물류 클레임 등록 · 주문 등록 기능을 일시적으로 중지하거나 재개합니다.
        비활성화하면 해당 등록 API 가 즉시 차단되며, 입력한 사유 문구가 모바일 앱에 안내됩니다.
      </Paragraph>

      <Alert
        type="info"
        style={{ marginBottom: 16 }}
        message="변경 즉시 반영되며 서버 재시작 후에도 상태가 유지됩니다(Redis 저장). 다시 활성화하기 전까지 해당 기능의 신규 등록은 차단됩니다."
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
    </>
  );
}
