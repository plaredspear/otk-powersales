import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  message,
  Select,
  Space,
  Upload,
} from 'antd';
import type { RcFile, UploadFile, UploadProps } from 'antd/es/upload/interface';
import { UploadOutlined } from '@ant-design/icons';
import { useSuggestionCreate } from '@/hooks/suggestions/useSuggestionCreate';
import { fetchAccounts, type Account } from '@/api/account';
import { fetchProducts, type Product } from '@/api/product';
import type {
  SuggestionActionStatus,
  SuggestionCategory,
  SuggestionCreatePayload,
} from '@/api/suggestions';

const CATEGORY_OPTIONS: Array<{ value: SuggestionCategory; label: string }> = [
  { value: 'LOGISTICS_CLAIM', label: '물류 클레임' },
  { value: 'NEW_PRODUCT', label: '신제품 제안' },
  { value: 'EXISTING_PRODUCT', label: '기존제품 상품가치 향상' },
];

const ACTION_STATUS_OPTIONS: Array<{ value: SuggestionActionStatus; label: string }> = [
  { value: 'UNCONFIRMED', label: '미확인' },
  { value: 'IN_PROGRESS', label: '조치중' },
  { value: 'COMPLETED', label: '조치완료' },
  { value: 'DUPLICATE_RECEPTION', label: '중복접수' },
];

const MAX_PHOTOS = 10;

export default function SuggestionCreatePage() {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const createMutation = useSuggestionCreate();

  const [accountKeyword, setAccountKeyword] = useState('');
  const [accountOptions, setAccountOptions] = useState<Account[]>([]);
  const [productKeyword, setProductKeyword] = useState('');
  const [productOptions, setProductOptions] = useState<Product[]>([]);

  const [fileList, setFileList] = useState<UploadFile[]>([]);

  useEffect(() => {
    if (accountKeyword.trim().length < 1) {
      setAccountOptions([]);
      return;
    }
    let cancelled = false;
    const timer = setTimeout(async () => {
      try {
        const data = await fetchAccounts({ keyword: accountKeyword.trim(), page: 0, size: 20 });
        if (!cancelled) setAccountOptions(data.content);
      } catch {
        if (!cancelled) setAccountOptions([]);
      }
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [accountKeyword]);

  useEffect(() => {
    if (productKeyword.trim().length < 1) {
      setProductOptions([]);
      return;
    }
    let cancelled = false;
    const timer = setTimeout(async () => {
      try {
        const data = await fetchProducts({ keyword: productKeyword.trim(), page: 0, size: 20 });
        if (!cancelled) setProductOptions(data.content);
      } catch {
        if (!cancelled) setProductOptions([]);
      }
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [productKeyword]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const photos: File[] = fileList
        .map((item) => (item.originFileObj ? (item.originFileObj as File) : null))
        .filter((f): f is File => f !== null);
      if (photos.length > MAX_PHOTOS) {
        message.error(`사진은 최대 ${MAX_PHOTOS}장까지 첨부 가능합니다`);
        return;
      }
      const payload: SuggestionCreatePayload = {
        category: values.category,
        title: values.title,
        content: values.content,
        productCode: values.productCode || undefined,
        accountId: values.accountId ?? undefined,
        claimType: values.category === 'LOGISTICS_CLAIM' ? values.claimType : undefined,
        claimDate: values.category === 'LOGISTICS_CLAIM' && values.claimDate
          ? values.claimDate.format('YYYY-MM-DD')
          : undefined,
        carNumber: values.category === 'LOGISTICS_CLAIM' ? values.carNumber : undefined,
        logisticsResponsibility: values.category === 'LOGISTICS_CLAIM' ? values.logisticsResponsibility : undefined,
        actionStatus: values.actionStatus || undefined,
        duplicateProposalNum: values.actionStatus === 'DUPLICATE_RECEPTION' ? values.duplicateProposalNum : undefined,
      };
      const result = await createMutation.mutateAsync({ payload, photos });
      message.success('제안사항이 등록되었습니다');
      navigate(`/suggestion/${result.id}`);
    } catch (err) {
      if (err instanceof Error) message.error(err.message);
    }
  };

  const uploadProps: UploadProps = {
    multiple: true,
    accept: 'image/*',
    fileList,
    beforeUpload: (_file: RcFile, currentList: RcFile[]) => {
      if (fileList.length + currentList.length > MAX_PHOTOS) {
        message.error(`사진은 최대 ${MAX_PHOTOS}장까지 첨부 가능합니다`);
        return Upload.LIST_IGNORE;
      }
      return false; // 자동 업로드 차단 — 등록 버튼 클릭 시 일괄 전송
    },
    onChange: ({ fileList: list }) => setFileList(list),
    onRemove: (file) => {
      setFileList((prev) => prev.filter((item) => item.uid !== file.uid));
    },
  };

  return (
    <div style={{ padding: 16, maxWidth: 900 }}>
      <div style={{ marginBottom: 16 }}>
        <Button type="link" onClick={() => navigate('/suggestion')} style={{ paddingLeft: 0 }}>
          ← 목록
        </Button>
      </div>

      <Form form={form} layout="vertical" initialValues={{ category: 'LOGISTICS_CLAIM' }}>
        <Card title="제안사항 정보" style={{ marginBottom: 16 }}>
          <Form.Item name="category" label="카테고리" rules={[{ required: true, message: '카테고리를 선택해주세요' }]}>
            <Select options={CATEGORY_OPTIONS} />
          </Form.Item>
          <Form.Item name="title" label="제목" rules={[{ required: true, message: '제목을 입력해주세요' }, { max: 250 }]}>
            <Input placeholder="제안 제목" />
          </Form.Item>
          <Form.Item name="content" label="제안내용" rules={[{ required: true, message: '내용을 입력해주세요' }]}>
            <Input.TextArea rows={6} placeholder="제안 본문" />
          </Form.Item>
          <Form.Item name="accountId" label="거래처">
            <Select
              showSearch
              filterOption={false}
              placeholder="거래처명으로 검색"
              onSearch={setAccountKeyword}
              options={accountOptions.map((a) => ({ value: a.id, label: `${a.name} (${a.externalKey ?? '-'})` }))}
              allowClear
            />
          </Form.Item>
          <Form.Item name="productCode" label="제품">
            <Select
              showSearch
              filterOption={false}
              placeholder="제품명/코드로 검색"
              onSearch={setProductKeyword}
              options={productOptions.map((p) => ({
                value: p.productCode,
                label: `${p.name} (${p.productCode})`,
              }))}
              allowClear
            />
          </Form.Item>
        </Card>

        <Form.Item shouldUpdate={(p, n) => p.category !== n.category} noStyle>
          {({ getFieldValue }) =>
            getFieldValue('category') === 'LOGISTICS_CLAIM' ? (
              <Card title="물류 클레임 정보" style={{ marginBottom: 16 }}>
                <Form.Item name="claimType" label="클레임 항목" rules={[{ required: true, max: 200 }]}>
                  <Input placeholder="예) 포장상태 불량" />
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
            ) : null
          }
        </Form.Item>

        <Card title="조치 정보 (선택)" style={{ marginBottom: 16 }}>
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

        <Card title="첨부사진" style={{ marginBottom: 16 }}>
          <Upload {...uploadProps} listType="picture">
            <Button icon={<UploadOutlined />}>사진 선택 (최대 {MAX_PHOTOS}장)</Button>
          </Upload>
        </Card>

        <Space>
          <Button type="primary" loading={createMutation.isPending} onClick={handleSubmit}>
            저장
          </Button>
          <Button onClick={() => navigate('/suggestion')}>취소</Button>
        </Space>
      </Form>
    </div>
  );
}
