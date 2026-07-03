import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Descriptions, Modal, Space, Spin, Tag, message } from 'antd';
import DOMPurify from 'dompurify';
import { useNoticeDetail } from '@/hooks/notice/useNoticeDetail';
import {
  useDeleteNotice,
  usePublishNotice,
  useUnpublishNotice,
  useSendNoticePush,
} from '@/hooks/notice/useNoticeMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  COMPANY: { color: 'blue', label: '회사공지' },
  BRANCH: { color: 'green', label: '지점공지' },
};

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  DRAFT: { color: 'default', label: '임시저장' },
  PUBLISHED: { color: 'success', label: '발행' },
};

export default function NoticeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const noticeId = Number(id);

  const { data: notice, isLoading, error } = useNoticeDetail(noticeId);
  const deleteMutation = useDeleteNotice();
  const publishMutation = usePublishNotice();
  const unpublishMutation = useUnpublishNotice();
  const pushMutation = useSendNoticePush();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  useEffect(() => {
    setDynamicTitle(notice?.title ?? null);
    return () => setDynamicTitle(null);
  }, [notice?.title, setDynamicTitle]);

  const handlePublish = async () => {
    try {
      await publishMutation.mutateAsync(noticeId);
      message.success('공지사항이 발행되었습니다');
    } catch {
      message.error('공지사항 발행에 실패했습니다');
    }
  };

  const handleUnpublish = async () => {
    try {
      await unpublishMutation.mutateAsync(noticeId);
      message.success('공지사항 발행이 취소되었습니다');
    } catch {
      message.error('공지사항 발행취소에 실패했습니다');
    }
  };

  const handleSendPush = () => {
    const already = notice?.pushSentCount ?? 0;
    Modal.confirm({
      title: '푸시 알림 발송',
      content:
        already > 0
          ? `이미 ${already}번 발송된 공지입니다. 대상자에게 다시 푸시 알림을 발송하시겠습니까?`
          : '이 공지를 볼 수 있는 대상자에게 푸시 알림을 발송하시겠습니까?',
      okText: '발송',
      cancelText: '취소',
      onOk: async () => {
        try {
          const result = await pushMutation.mutateAsync(noticeId);
          message.success(
            `푸시 알림 발송 완료 (대상 ${result.targetCount}건 / 성공 ${result.successCount}건 / 실패 ${result.failureCount}건)`,
          );
        } catch {
          message.error('푸시 알림 발송에 실패했습니다');
        }
      },
    });
  };

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
  const statusTag = STATUS_TAG[notice.status];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Button type="link" onClick={() => navigate('/notices')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          {notice.status === 'PUBLISHED' && notice.scope !== '영업사원' && (
            <Button onClick={handleSendPush} loading={pushMutation.isPending}>
              푸시 발송
            </Button>
          )}
          {notice.status === 'PUBLISHED' ? (
            <Button onClick={handleUnpublish} loading={unpublishMutation.isPending}>
              발행취소
            </Button>
          ) : (
            <Button type="primary" onClick={handlePublish} loading={publishMutation.isPending}>
              발행
            </Button>
          )}
          <Button onClick={() => navigate(`/notices/${noticeId}/edit`)}>수정</Button>
          <Button danger onClick={handleDelete}>삭제</Button>
        </Space>
      </div>

      <div style={{ marginBottom: 16 }}>
        <Space>
          {statusTag ? (
            <Tag color={statusTag.color}>{statusTag.label}</Tag>
          ) : (
            <Tag>{notice.statusName}</Tag>
          )}
          {tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{notice.categoryName}</Tag>}
        </Space>
      </div>

      <Descriptions column={1} style={{ marginBottom: 24 }}>
        <Descriptions.Item label="등록일">{notice.createdAt?.substring(0, 10)}</Descriptions.Item>
        <Descriptions.Item label="지점">
          {notice.category === 'BRANCH' && notice.branch ? notice.branch : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="푸시 발송">
          {notice.lastPush
            ? `${notice.lastPush.sentAt?.substring(0, 16).replace('T', ' ')} · 대상 ${notice.lastPush.targetCount}건 / 성공 ${notice.lastPush.successCount}건 (누적 ${notice.pushSentCount}회)`
            : '미발송'}
        </Descriptions.Item>
      </Descriptions>

      {/*
        본문 인라인 이미지는 backend(getNoticeDetail)가 presigned URL 로 rewrite 해서 내려준다.
        data-refid 는 mobile cacheKey 용 식별자로 본문에 보존되므로 sanitize 시 명시적으로 허용한다
        (DOMPurify 3.x 는 data-* 를 기본 허용하나 버전/설정 변동 대비). presigned https src 는 기본 허용.
      */}
      <div
        style={{ borderTop: '1px solid #f0f0f0', paddingTop: 24 }}
        dangerouslySetInnerHTML={{
          __html: DOMPurify.sanitize(notice.content || '', { ADD_ATTR: ['data-refid'] }),
        }}
      />
    </div>
  );
}
