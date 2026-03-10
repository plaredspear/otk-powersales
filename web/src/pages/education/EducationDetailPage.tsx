import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Descriptions, List, Modal, Space, Spin, Tag, Typography, message } from 'antd';
import {
  FileImageOutlined,
  FileOutlined,
  FileTextOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import DOMPurify from 'dompurify';
import { useEducationDetail } from '@/hooks/education/useEducationDetail';
import { useDeleteEducation } from '@/hooks/education/useEducationMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import type { EducationAttachment } from '@/api/education';

const { Title } = Typography;

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  c00001: { color: 'orange', label: '시식매뉴얼' },
  c00002: { color: 'red', label: 'CS/안전' },
  c00003: { color: 'blue', label: '교육평가' },
  c00004: { color: 'green', label: '신제품소개' },
};

const FILE_TYPE_ICON: Record<string, React.ReactNode> = {
  f00001: <FileImageOutlined />,
  f00002: <VideoCameraOutlined />,
  f00003: <FileTextOutlined />,
  f00004: <FileOutlined />,
};

function getFileTypeFromName(fileName: string): string {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return 'f00001';
  if (['mp4', 'avi', 'wmv', 'mkv', 'mov', 'm4v'].includes(ext)) return 'f00002';
  if (['pdf', 'docx', 'txt', 'hwp', 'pptx', 'xlsx'].includes(ext)) return 'f00003';
  return 'f00004';
}

export default function EducationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: education, isLoading, error } = useEducationDetail(id ?? '');
  const deleteMutation = useDeleteEducation();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  useEffect(() => {
    setDynamicTitle(education?.title ?? null);
    return () => setDynamicTitle(null);
  }, [education?.title, setDynamicTitle]);

  const handleDelete = () => {
    Modal.confirm({
      title: '교육 자료 삭제',
      content: '이 교육 자료를 삭제하시겠습니까? 첨부파일도 함께 삭제됩니다.',
      okText: '확인',
      cancelText: '취소',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteMutation.mutateAsync(id!);
          message.success('교육 자료가 삭제되었습니다');
          navigate('/education');
        } catch {
          message.error('교육 자료 삭제에 실패했습니다');
        }
      },
    });
  };

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error || !education) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/education')}>← 목록으로</Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          교육 자료를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const tag = CATEGORY_TAG[education.category];

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Button type="link" onClick={() => navigate('/education')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          <Button onClick={() => navigate(`/education/${id}/edit`)}>수정</Button>
          <Button danger onClick={handleDelete}>삭제</Button>
        </Space>
      </div>

      <div style={{ marginBottom: 16 }}>
        {tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{education.categoryName}</Tag>}
      </div>

      <Title level={3}>{education.title}</Title>

      <Descriptions column={2} style={{ marginBottom: 24 }}>
        <Descriptions.Item label="등록일">{education.createdAt?.substring(0, 10)}</Descriptions.Item>
      </Descriptions>

      <div
        style={{ borderTop: '1px solid #f0f0f0', paddingTop: 24, marginBottom: 24 }}
        dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(education.content || '') }}
      />

      {education.attachments.length > 0 && (
        <>
          <Title level={5}>첨부파일 ({education.attachments.length})</Title>
          <List
            bordered
            dataSource={education.attachments}
            renderItem={(att: EducationAttachment) => {
              const fileType = getFileTypeFromName(att.fileName);
              return (
                <List.Item
                  actions={[
                    <Button
                      key="download"
                      type="link"
                      onClick={() => window.open(att.fileUrl, '_blank')}
                    >
                      다운로드
                    </Button>,
                  ]}
                >
                  <Space>
                    {FILE_TYPE_ICON[fileType] ?? <FileOutlined />}
                    {att.fileName}
                  </Space>
                </List.Item>
              );
            }}
          />
        </>
      )}
    </div>
  );
}
