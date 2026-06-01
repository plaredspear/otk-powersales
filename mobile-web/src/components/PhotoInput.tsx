import { useRef } from 'react';
import { Button, Image, Space } from 'antd';
import { CameraOutlined, DeleteOutlined } from '@ant-design/icons';

/**
 * 사진 촬영/선택 입력 (web 표준 <input capture=environment>).
 * 모바일 브라우저/WebView 에서 카메라 또는 갤러리 선택. Capacitor 단계에서 네이티브 카메라로 교체 가능.
 */
export default function PhotoInput({
  label = '사진 촬영',
  value,
  onChange,
}: {
  label?: string;
  value: File | null;
  onChange: (file: File | null) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const previewUrl = value ? URL.createObjectURL(value) : null;

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        capture="environment"
        style={{ display: 'none' }}
        onChange={(e) => onChange(e.target.files?.[0] ?? null)}
      />
      {previewUrl ? (
        <Space direction="vertical">
          <Image src={previewUrl} width={120} height={120} style={{ objectFit: 'cover', borderRadius: 8 }} />
          <Button danger size="small" icon={<DeleteOutlined />} onClick={() => onChange(null)}>
            삭제
          </Button>
        </Space>
      ) : (
        <Button icon={<CameraOutlined />} onClick={() => inputRef.current?.click()}>
          {label}
        </Button>
      )}
    </div>
  );
}
