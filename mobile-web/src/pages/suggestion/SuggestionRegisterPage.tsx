import { useState } from 'react';
import { Button, Card, DatePicker, Form, Input, Select, Space, Typography, App as AntdApp } from 'antd';
import { useMutation } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import { createSuggestion, SUGGESTION_CATEGORY } from '@/api/suggestions';
import DetailHeader from '@/components/DetailHeader';
import PhotoInput from '@/components/PhotoInput';

interface SuggestionForm {
  category: string;
  title: string;
  content: string;
  productCode?: string;
  claimType?: string;
  claimDate?: Dayjs;
  carNumber?: string;
  logisticsResponsibility?: string;
}

/** 제안하기 / 물류클레임 작성 (레거시 fieldTalk/suggest/write). */
export default function SuggestionRegisterPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm<SuggestionForm>();
  const category = Form.useWatch('category', form);
  const [photos, setPhotos] = useState<(File | null)[]>([null]);

  const isLogistics = category === SUGGESTION_CATEGORY.LOGISTICS_CLAIM;

  const mutation = useMutation({
    mutationFn: (v: SuggestionForm) =>
      createSuggestion(
        {
          category: v.category,
          title: v.title,
          content: v.content,
          productCode: v.productCode,
          claimType: isLogistics ? v.claimType : undefined,
          claimDate: isLogistics ? v.claimDate?.format('YYYY-MM-DD') : undefined,
          carNumber: isLogistics ? v.carNumber : undefined,
          logisticsResponsibility: isLogistics ? v.logisticsResponsibility : undefined,
        },
        photos.filter((p): p is File => p !== null)
      ),
    onSuccess: () => {
      message.success('등록되었습니다');
      navigate(isLogistics ? '/logistics-claims' : '/menu', { replace: true });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '등록에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="제안 / 물류클레임" />
      <Card styles={{ body: { padding: 16 } }}>
        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          initialValues={{ category: SUGGESTION_CATEGORY.SUGGESTION }}
          onFinish={(v) => mutation.mutate(v)}
        >
          <Form.Item name="category" label="구분" rules={[{ required: true }]}>
            <Select
              options={[
                { value: SUGGESTION_CATEGORY.SUGGESTION, label: '제안하기' },
                { value: SUGGESTION_CATEGORY.LOGISTICS_CLAIM, label: '물류클레임' },
              ]}
            />
          </Form.Item>
          <Form.Item name="title" label="제목" rules={[{ required: true, message: '제목을 입력하세요' }]}>
            <Input placeholder="제목" />
          </Form.Item>
          <Form.Item name="content" label="내용" rules={[{ required: true, message: '내용을 입력하세요' }]}>
            <Input.TextArea rows={4} placeholder="내용" />
          </Form.Item>
          <Form.Item name="productCode" label="제품코드">
            <Input placeholder="제품코드 (선택)" />
          </Form.Item>

          {isLogistics && (
            <>
              <Typography.Title level={5}>물류 정보</Typography.Title>
              <Form.Item name="claimType" label="클레임 유형">
                <Input placeholder="클레임 유형" />
              </Form.Item>
              <Form.Item name="claimDate" label="클레임 일자">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="carNumber" label="차량번호">
                <Input placeholder="차량번호" />
              </Form.Item>
              <Form.Item name="logisticsResponsibility" label="물류 귀책">
                <Input placeholder="물류 귀책" />
              </Form.Item>
            </>
          )}

          <Typography.Title level={5}>사진</Typography.Title>
          <Space wrap size={16} style={{ marginBottom: 16 }}>
            {photos.map((p, i) => (
              <PhotoInput
                key={i}
                label="촬영"
                value={p}
                onChange={(file) =>
                  setPhotos((prev) => {
                    const next = [...prev];
                    next[i] = file;
                    if (file && i === prev.length - 1 && prev.length < 5) next.push(null);
                    return next;
                  })
                }
              />
            ))}
          </Space>

          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            등록
          </Button>
        </Form>
      </Card>
    </>
  );
}
