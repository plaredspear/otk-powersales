import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Descriptions, Modal, Space, Spin, Tag, Typography, message } from 'antd';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import { useDeletePromotion } from '@/hooks/promotion/usePromotionMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const { Title } = Typography;

const CATEGORY_TAG: Record<string, string> = {
  라면: 'red',
  냉장: 'blue',
  냉동: 'cyan',
  만두: 'orange',
};

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

export default function PromotionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const promotionId = Number(id);

  const { data: promotion, isLoading, error } = usePromotion(promotionId);
  const deleteMutation = useDeletePromotion();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  useEffect(() => {
    setDynamicTitle(promotion?.promotionNumber ?? null);
    return () => setDynamicTitle(null);
  }, [promotion?.promotionNumber, setDynamicTitle]);

  const handleDelete = () => {
    Modal.confirm({
      title: '행사마스터 삭제',
      content: '이 행사마스터를 삭제하시겠습니까?',
      okText: '확인',
      cancelText: '취소',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteMutation.mutateAsync(promotionId);
          message.success('행사마스터가 삭제되었습니다');
          navigate('/promotions');
        } catch {
          message.error('행사마스터 삭제에 실패했습니다');
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

  if (error || !promotion) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/promotions')}>
          ← 목록으로
        </Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          행사마스터를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const categoryColor = promotion.category ? CATEGORY_TAG[promotion.category] : undefined;
  const typeColor = promotion.promotionTypeName
    ? PROMOTION_TYPE_TAG[promotion.promotionTypeName]
    : undefined;

  return (
    <div style={{ padding: 24 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Button type="link" onClick={() => navigate('/promotions')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          <Button onClick={() => navigate(`/promotions/${promotionId}/edit`)}>수정</Button>
          <Button danger onClick={handleDelete}>
            삭제
          </Button>
        </Space>
      </div>

      <Title level={4} style={{ marginBottom: 24 }}>
        {promotion.promotionNumber}
      </Title>

      <Descriptions column={1} bordered>
        <Descriptions.Item label="행사번호">{promotion.promotionNumber}</Descriptions.Item>
        <Descriptions.Item label="행사명">{promotion.promotionName}</Descriptions.Item>
        <Descriptions.Item label="행사유형">
          {promotion.promotionTypeName ? (
            <Tag color={typeColor}>{promotion.promotionTypeName}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="거래처">{promotion.accountName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="기간">
          {promotion.startDate} ~ {promotion.endDate}
        </Descriptions.Item>
        <Descriptions.Item label="대표상품">
          {promotion.primaryProductName ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="기타상품">{promotion.otherProduct ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="카테고리">
          {promotion.category ? (
            <Tag color={categoryColor}>{promotion.category}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="제품유형">{promotion.productType ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="지점명">{promotion.branchName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="전문행사조">
          {promotion.professionalTeam ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="매대위치">{promotion.standLocation ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="목표금액">
          {promotion.targetAmount != null
            ? `${promotion.targetAmount.toLocaleString()}원`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="실적금액">
          {promotion.actualAmount != null
            ? `${promotion.actualAmount.toLocaleString()}원`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="마감 여부">
          {promotion.isClosed ? <Tag color="red">마감</Tag> : '미마감'}
        </Descriptions.Item>
        <Descriptions.Item label="외부 연동 ID">
          {promotion.externalId ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="메시지">{promotion.message ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="CC코드">{promotion.costCenterCode ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="생성일">
          {promotion.createdAt?.substring(0, 16)}
        </Descriptions.Item>
        <Descriptions.Item label="수정일">
          {promotion.updatedAt?.substring(0, 16)}
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
}
