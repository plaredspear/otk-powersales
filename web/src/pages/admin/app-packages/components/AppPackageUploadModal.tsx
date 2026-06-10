import { useState } from 'react';
import { Modal, Form, Input, Switch, Upload, Alert, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useUploadAppPackage } from '@/hooks/appPackage/useAppPackages';
import type { AppPlatform } from '@/api/appPackage';

const { Dragger } = Upload;
const { TextArea } = Input;

interface Props {
  open: boolean;
  platform: AppPlatform;
  onClose: () => void;
}

interface FormValues {
  // versionName/versionCode 는 iOS(.ipa)·Android(.apk) 모두 파일에서 자동 추출하므로
  // 폼에서 받지 않는다.
  forceUpdate: boolean;
  releaseNote?: string;
}

const ACCEPT: Record<AppPlatform, string> = { ANDROID: '.apk', IOS: '.ipa' };

export default function AppPackageUploadModal({ open, platform, onClose }: Props) {
  const [form] = Form.useForm<FormValues>();
  const [file, setFile] = useState<File | null>(null);
  const upload = useUploadAppPackage();

  const handleClose = () => {
    form.resetFields();
    setFile(null);
    onClose();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (!file) {
      message.error('패키지 파일을 선택하세요');
      return;
    }
    try {
      // versionName/versionCode 는 백엔드가 업로드 파일에서 자동 추출한다(전송 불요).
      await upload.mutateAsync({
        platform,
        forceUpdate: values.forceUpdate ?? false,
        releaseNote: values.releaseNote,
        file,
      });
      message.success('패키지가 업로드되었습니다');
      handleClose();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '업로드에 실패했습니다');
    }
  };

  return (
    <Modal
      open={open}
      title={`${platform} 패키지 업로드`}
      onCancel={handleClose}
      onOk={handleSubmit}
      okText="업로드"
      confirmLoading={upload.isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical" initialValues={{ forceUpdate: false }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message={`${platform === 'IOS' ? 'Bundle Identifier' : 'Application ID'} · 버전명 · 버전 코드는 업로드한 ${ACCEPT[platform]} 파일에서 자동으로 추출됩니다.`}
        />
        <Form.Item name="forceUpdate" label="강제 업데이트" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="releaseNote" label="릴리스 노트">
          <TextArea rows={3} placeholder="변경 사항" />
        </Form.Item>
        <Form.Item label={`패키지 파일 (${ACCEPT[platform]})`} required>
          <Dragger
            multiple={false}
            accept={ACCEPT[platform]}
            showUploadList={false}
            beforeUpload={(f) => {
              if (!f.name.toLowerCase().endsWith(ACCEPT[platform])) {
                message.error(`${ACCEPT[platform]} 파일만 업로드할 수 있습니다`);
                return Upload.LIST_IGNORE;
              }
              setFile(f);
              return false; // 수동 업로드 (FormData 로 직접 전송)
            }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">
              {file ? file.name : '클릭 또는 드래그하여 업로드'}
            </p>
          </Dragger>
        </Form.Item>
      </Form>
    </Modal>
  );
}
