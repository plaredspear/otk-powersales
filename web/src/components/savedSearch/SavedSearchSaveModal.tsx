import { useState } from 'react';
import { Modal, Form, Input, Radio, Descriptions, message } from 'antd';
import { AxiosError } from 'axios';
import { useCreateSavedSearch } from '@/hooks/savedSearch/useSavedSearchMutation';
import type { SavedSearchScope } from '@/api/savedSearch';

interface FilterPreview {
  label: string;
  value: string;
}

interface SavedSearchSaveModalProps {
  open: boolean;
  resourceKey: string;
  /** 현재 화면의 필터 상태 (저장 대상). */
  filters: Record<string, string>;
  /** 저장할 조건의 사람이 읽을 수 있는 미리보기. */
  preview: FilterPreview[];
  /** 공용(SHARED) 저장 가능 여부 (saved_search EDIT 권한). */
  canSaveShared: boolean;
  onClose: () => void;
  /** 저장 성공 시 생성된 검색 id 를 전달 (자동 선택용). */
  onSaved: (createdId: number) => void;
}

export default function SavedSearchSaveModal({
  open,
  resourceKey,
  filters,
  preview,
  canSaveShared,
  onClose,
  onSaved,
}: SavedSearchSaveModalProps) {
  const [form] = Form.useForm<{ name: string; scope: SavedSearchScope }>();
  const createMutation = useCreateSavedSearch();
  const [duplicateError, setDuplicateError] = useState<string | null>(null);

  const handleOk = async () => {
    const values = await form.validateFields();
    setDuplicateError(null);
    try {
      const created = await createMutation.mutateAsync({
        resourceKey,
        name: values.name.trim(),
        scope: values.scope,
        filters,
      });
      message.success('검색 조건을 저장했습니다');
      form.resetFields();
      onSaved(created.id);
      onClose();
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string }; message?: string }>;
      if (err.response?.status === 409) {
        setDuplicateError('같은 이름의 검색이 이미 있습니다');
        return;
      }
      message.error(err.response?.data?.message || '저장에 실패했습니다');
    }
  };

  return (
    <Modal
      title="검색 조건 저장"
      open={open}
      onOk={handleOk}
      onCancel={() => {
        form.resetFields();
        setDuplicateError(null);
        onClose();
      }}
      okText="저장"
      cancelText="취소"
      confirmLoading={createMutation.isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical" initialValues={{ scope: 'PRIVATE' }}>
        <Form.Item
          label="이름"
          name="name"
          rules={[
            { required: true, message: '이름을 입력하세요' },
            { max: 100, message: '이름은 100자 이하여야 합니다' },
          ]}
          validateStatus={duplicateError ? 'error' : undefined}
          help={duplicateError ?? undefined}
        >
          <Input placeholder="예: 관리자_검색용" maxLength={100} />
        </Form.Item>

        <Form.Item label="공개 범위" name="scope">
          <Radio.Group>
            <Radio value="PRIVATE">개인 (나만 보기)</Radio>
            <Radio value="SHARED" disabled={!canSaveShared}>
              공용 (전체 공개){!canSaveShared ? ' — 권한 없음' : ''}
            </Radio>
          </Radio.Group>
        </Form.Item>

        <Descriptions title="저장할 조건" column={1} size="small" bordered>
          {preview.map((p) => (
            <Descriptions.Item key={p.label} label={p.label}>
              {p.value || '-'}
            </Descriptions.Item>
          ))}
        </Descriptions>
      </Form>
    </Modal>
  );
}
