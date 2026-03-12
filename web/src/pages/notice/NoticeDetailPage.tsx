import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Descriptions, Modal, Space, Spin, Tag, message } from 'antd';
import DOMPurify from 'dompurify';
import { useNoticeDetail } from '@/hooks/notice/useNoticeDetail';
import { useDeleteNotice } from '@/hooks/notice/useNoticeMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  COMPANY: { color: 'blue', label: '전체공지' },
  BRANCH: { color: 'green', label: '지점공지' },
};

export default function NoticeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const noticeId = Number(id);

  const { data: notice, isLoading, error } = useNoticeDetail(noticeId);
  const deleteMutation = useDeleteNotice();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  useEffect(() => {
    setDynamicTitle(notice?.title ?? null);
    return () => setDynamicTitle(null);
  }, [notice?.title, setDynamicTitle]);

  const handleDelete = () => {
    Modal.confirm({
      title: '공지사항 삭제',
      content: '이 공지사항을 삭제하시겠습니까?',
      okText: '확인',
      cancelText: '취소',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteMutation.mutateAsync(noticeId);
          message.success('공지사항이 삭제되었습니다');
          navigate('/notices');
        } catch {
          message.error('공지사항 삭제에 실패했습니다');
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

  if (error || !notice) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/notices')}>← 목록으로</Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          공지사항을 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const tag = CATEGORY_TAG[notice.category];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Button type="link" onClick={() => navigate('/notices')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          <Button onClick={() => navigate(`/notices/${noticeId}/edit`)}>수정</Button>
          <Button danger onClick={handleDelete}>삭제</Button>
        </Space>
      </div>

      <div style={{ marginBottom: 16 }}>
        {tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{notice.categoryName}</Tag>}
      </div>

      <Descriptions column={1} style={{ marginBottom: 24 }}>
        <Descriptions.Item label="등록일">{notice.createdAt?.substring(0, 10)}</Descriptions.Item>
        <Descriptions.Item label="지점">
          {notice.category === 'BRANCH' && notice.branch ? notice.branch : '-'}
        </Descriptions.Item>
      </Descriptions>

      <div
        style={{ borderTop: '1px solid #f0f0f0', paddingTop: 24 }}
        dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(notice.content || '') }}
      />
    </div>
  );
}
