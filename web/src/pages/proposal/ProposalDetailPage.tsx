import { useContext, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Form,
  Image,
  Input,
  message,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import { DeleteOutlined, EditOutlined, UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { useSuggestionDetail } from '@/hooks/suggestions/useSuggestionDetail';
import { useSuggestionUpdate } from '@/hooks/suggestions/useSuggestionUpdate';
import { useSuggestionDelete } from '@/hooks/suggestions/useSuggestionDelete';
import {
  useSuggestionPhotoUpload,
  useSuggestionPhotoDelete,
} from '@/hooks/suggestions/useSuggestionPhotoMutations';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import {
  SUGGESTION_MAX_PHOTOS as MAX_PHOTOS,
  type SuggestionActionStatus,
  type SuggestionCategory,
  type SuggestionUpdatePayload,
} from '@/api/suggestions';

const CATEGORY_TAG: Record<SuggestionCategory, { color: string; label: string }> = {
  NEW_PRODUCT: { color: 'blue', label: '신제품 제안' },
  EXISTING_PRODUCT: { color: 'cyan', label: '기존제품 상품가치 향상' },
  LOGISTICS_CLAIM: { color: 'volcano', label: '물류 클레임' },
};

const ACTION_STATUS_OPTIONS: Array<{ value: SuggestionActionStatus; label: string }> = [
  { value: 'UNCONFIRMED', label: '미확인' },
  { value: 'IN_PROGRESS', label: '조치중' },
  { value: 'COMPLETED', label: '조치완료' },
  { value: 'DUPLICATE_RECEPTION', label: '중복접수' },
];

const ACTION_STATUS_TAG: Record<SuggestionActionStatus, { color: string; label: string }> = {
  UNCONFIRMED: { color: 'default', label: '미확인' },
  IN_PROGRESS: { color: 'orange', label: '조치중' },
  COMPLETED: { color: 'green', label: '조치완료' },
  DUPLICATE_RECEPTION: { color: 'red', label: '중복접수' },
};

export default function ProposalDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const id = Number(idParam);

  const { data: suggestion, isLoading, error } = useSuggestionDetail(id);
  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const [editMode, setEditMode] = useState(false);
  const [form] = Form.useForm();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const updateMutation = useSuggestionUpdate(id);
  const deleteMutation = useSuggestionDelete();
  const photoUploadMutation = useSuggestionPhotoUpload(id);
  const photoDeleteMutation = useSuggestionPhotoDelete(id);

  const isClaim = suggestion?.category === 'LOGISTICS_CLAIM';

  useEffect(() => {
    if (suggestion) setDynamicTitle(`제안사항 ${suggestion.proposalNumber}`);
    return () => setDynamicTitle(null);
  }, [suggestion, setDynamicTitle]);

  useEffect(() => {
    if (suggestion && editMode) {
      form.setFieldsValue({
        title: suggestion.title,
        content: suggestion.content,
        claimType: suggestion.claimType,
        claimDate: suggestion.claimDate ? dayjs(suggestion.claimDate) : null,
        carNumber: suggestion.carNumber,
        logisticsResponsibility: suggestion.logisticsResponsibility,
        actionStatus: suggestion.actionStatus,
        duplicateProposalNum: suggestion.duplicateProposalNum,
      });
    }
  }, [suggestion, editMode, form]);

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error || !suggestion) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/proposal')} style={{ paddingLeft: 0 }}>
          ← 목록
        </Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          제안사항을 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload: SuggestionUpdatePayload = {
        category: suggestion.category,
        title: values.title,
        content: values.content,
        // 물류 클레임 전용 필드는 클레임일 때만 전송 (레거시 Trigger 의 분기 검증과 정합).
        claimType: isClaim ? values.claimType : undefined,
        claimDate: isClaim && values.claimDate ? values.claimDate.format('YYYY-MM-DD') : undefined,
        carNumber: isClaim ? values.carNumber || undefined : undefined,
        logisticsResponsibility: isClaim ? values.logisticsResponsibility || undefined : undefined,
        actionStatus: isClaim ? values.actionStatus || undefined : undefined,
        duplicateProposalNum:
          isClaim && values.actionStatus === 'DUPLICATE_RECEPTION' ? values.duplicateProposalNum : undefined,
      };
      await updateMutation.mutateAsync(payload);
      message.success('제안사항이 수정되었습니다');
      setEditMode(false);
    } catch (err) {
      if (err instanceof Error) {
        message.error(err.message);
      }
    }
  };

  const handleDelete = async () => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success('제안사항이 삭제되었습니다');
      navigate('/proposal');
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFilesSelected = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.target.value = '';
    if (files.length === 0) return;
    const remaining = MAX_PHOTOS - suggestion.attachments.length;
    if (files.length > remaining) {
      message.error(`사진은 최대 ${MAX_PHOTOS}장까지 첨부 가능합니다 (남은 슬롯 ${remaining}장)`);
      return;
    }
    try {
      await photoUploadMutation.mutateAsync(files);
      message.success('사진이 추가되었습니다');
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  };

  const handlePhotoDelete = async (photoId: number) => {
    try {
      await photoDeleteMutation.mutateAsync(photoId);
      message.success('사진이 삭제되었습니다');
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  };

  const categoryTag = CATEGORY_TAG[suggestion.category];
  const actionStatusTag = suggestion.actionStatus ? ACTION_STATUS_TAG[suggestion.actionStatus] : null;

  return (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Button type="link" onClick={() => navigate('/proposal')} style={{ paddingLeft: 0 }}>
          ← 목록
        </Button>
        <Space>
          {!editMode && (
            <Button icon={<EditOutlined />} onClick={() => setEditMode(true)}>
              수정
            </Button>
          )}
          {editMode && (
            <>
              <Button onClick={() => setEditMode(false)}>취소</Button>
              <Button type="primary" loading={updateMutation.isPending} onClick={handleSubmit}>저장</Button>
            </>
          )}
          <Popconfirm
            title="이 제안사항을 삭제하시겠습니까?"
            okText="삭제"
            cancelText="취소"
            okButtonProps={{ danger: true }}
            onConfirm={handleDelete}
          >
            <Button danger icon={<DeleteOutlined />} disabled={editMode}>삭제</Button>
          </Popconfirm>
        </Space>
      </div>

      <Row gutter={16}>
        <Col xs={24} lg={16}>
          {editMode ? (
            <Form form={form} layout="vertical">
              <Card title="기본 정보" style={{ marginBottom: 16 }}>
                <Form.Item name="title" label="제목" rules={[{ required: true, message: '제목을 입력해주세요' }, { max: 250 }]}>
                  <Input />
                </Form.Item>
                <Form.Item name="content" label="제안내용" rules={[{ required: true, message: '내용을 입력해주세요' }]}>
                  <Input.TextArea rows={6} />
                </Form.Item>
              </Card>

              {isClaim && (
                <>
                  <Card title="물류 클레임 정보" style={{ marginBottom: 16 }}>
                    <Form.Item name="claimType" label="클레임 항목" rules={[{ required: true, max: 200 }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item name="claimDate" label="클레임 발생일자" rules={[{ required: true }]}>
                      <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
                    </Form.Item>
                    <Form.Item name="carNumber" label="차량번호" rules={[{ max: 20 }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item name="logisticsResponsibility" label="물류책임" rules={[{ max: 20 }]}>
                      <Input />
                    </Form.Item>
                  </Card>

                  <Card title="조치 정보" style={{ marginBottom: 16 }}>
                    <Form.Item name="actionStatus" label="조치상태">
                      <Select options={ACTION_STATUS_OPTIONS} allowClear />
                    </Form.Item>
                    <Form.Item shouldUpdate={(p, n) => p.actionStatus !== n.actionStatus} noStyle>
                      {({ getFieldValue }) =>
                        getFieldValue('actionStatus') === 'DUPLICATE_RECEPTION' ? (
                          <Form.Item
                            name="duplicateProposalNum"
                            label="중복 제안번호"
                            rules={[{ required: true, message: '중복 제안번호를 입력해주세요' }, { max: 255 }]}
                          >
                            <Input />
                          </Form.Item>
                        ) : null
                      }
                    </Form.Item>
                  </Card>
                </>
              )}
            </Form>
          ) : (
            <>
              <Card title="기본 정보" style={{ marginBottom: 16 }}>
                <Descriptions column={2}>
                  <Descriptions.Item label="제안번호">{suggestion.proposalNumber}</Descriptions.Item>
                  <Descriptions.Item label="제안구분">
                    {categoryTag ? <Tag color={categoryTag.color}>{categoryTag.label}</Tag> : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="제목" span={2}>{suggestion.title}</Descriptions.Item>
                  <Descriptions.Item label="작성자">{suggestion.employeeName ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="사번">{suggestion.employeeCode ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="거래처명">{suggestion.accountName ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="거래처코드">{suggestion.accountCode ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="제품명">{suggestion.productName ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="제품코드">{suggestion.productCode ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="등록일시" span={2}>
                    {suggestion.createdAt?.substring(0, 16).replace('T', ' ')}
                  </Descriptions.Item>
                </Descriptions>
              </Card>

              <Card title="제안내용" style={{ marginBottom: 16 }}>
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
                  {suggestion.content}
                </Typography.Paragraph>
              </Card>

              {isClaim && (
                <>
                  <Card title="물류 클레임 정보" style={{ marginBottom: 16 }}>
                    <Descriptions column={2}>
                      <Descriptions.Item label="클레임 항목">{suggestion.claimType ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="클레임 발생일자">{suggestion.claimDate ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="차량번호">{suggestion.carNumber ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="물류책임">{suggestion.logisticsResponsibility ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="접수 물류센터">{suggestion.receptionLogisticsCenter ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="책임 물류센터">{suggestion.responsibleLogisticsCenter ?? '-'}</Descriptions.Item>
                    </Descriptions>
                  </Card>

                  <Card title="OLS 조치사항" style={{ marginBottom: 16 }}>
                    <Descriptions column={2}>
                      <Descriptions.Item label="조치번호">{suggestion.actionNum ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="조치상태">
                        {actionStatusTag ? <Tag color={actionStatusTag.color}>{actionStatusTag.label}</Tag> : '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="조치담당자(직급/이름)">{suggestion.actionManager ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="중복 제안번호">{suggestion.duplicateProposalNum ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="클레임 항목(조치사항)">{suggestion.claimTypeMeasures ?? '-'}</Descriptions.Item>
                      <Descriptions.Item label="조치내용" span={2}>{suggestion.actionContent ?? '-'}</Descriptions.Item>
                    </Descriptions>
                  </Card>
                </>
              )}
            </>
          )}
        </Col>

        <Col xs={24} lg={8}>
          <Card
            title="첨부사진"
            extra={
              <Button
                icon={<UploadOutlined />}
                size="small"
                loading={photoUploadMutation.isPending}
                onClick={handleUploadClick}
                disabled={editMode || suggestion.attachments.length >= MAX_PHOTOS}
              >
                업로드
              </Button>
            }
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleFilesSelected}
            />
            {suggestion.attachments.length === 0 ? (
              <Typography.Text type="secondary">등록된 사진이 없습니다</Typography.Text>
            ) : (
              <Image.PreviewGroup>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {suggestion.attachments.map((att) => (
                    <div key={att.id} style={{ position: 'relative', textAlign: 'center' }}>
                      <Image
                        src={att.s3Url ?? undefined}
                        alt={att.fileName ?? '사진'}
                        style={{ borderRadius: 4, maxHeight: 200 }}
                      />
                      <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                        {att.fileName ?? '-'}
                      </div>
                      <Popconfirm
                        title="이 사진을 삭제하시겠습니까?"
                        okText="삭제"
                        cancelText="취소"
                        okButtonProps={{ danger: true }}
                        onConfirm={() => handlePhotoDelete(att.id)}
                      >
                        <Button
                          danger
                          size="small"
                          icon={<DeleteOutlined />}
                          style={{ marginTop: 4 }}
                          loading={photoDeleteMutation.isPending}
                        >
                          삭제
                        </Button>
                      </Popconfirm>
                    </div>
                  ))}
                </div>
              </Image.PreviewGroup>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
