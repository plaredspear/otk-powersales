import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Descriptions, Image, Spin, Tag, Typography } from 'antd';
import { useClaimDetail } from '@/hooks/claims/useClaimDetail';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  SUBMITTED: { color: 'blue', label: '접수' },
  IN_PROGRESS: { color: 'orange', label: '처리중' },
  RESOLVED: { color: 'green', label: '처리완료' },
  REJECTED: { color: 'red', label: '반려' },
};

const DATE_TYPE_LABEL: Record<string, string> = {
  EXPIRY_DATE: '유통기한',
  MANUFACTURE_DATE: '제조일자',
};

const PHOTO_TYPE_LABEL: Record<string, string> = {
  DEFECT: '클레임',
  LABEL: '일부인',
  RECEIPT: '영수증',
};

export default function ClaimDetailPage() {
  const { claimId } = useParams<{ claimId: string }>();
  const navigate = useNavigate();
  const id = Number(claimId);

  const { data: claim, isLoading, error } = useClaimDetail(id);
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  useEffect(() => {
    if (claim) setDynamicTitle(`클레임 #${claim.claimId}`);
    return () => setDynamicTitle(null);
  }, [claim, setDynamicTitle]);

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error || !claim) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/claims')} style={{ paddingLeft: 0 }}>
          ← 목록
        </Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          클레임을 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const statusTag = STATUS_TAG[claim.status];
  const dateTypeLabel = claim.dateType ? DATE_TYPE_LABEL[claim.dateType] : null;

  return (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Button type="link" onClick={() => navigate('/claims')} style={{ paddingLeft: 0 }}>
          ← 목록
        </Button>
      </div>

      <Card title="기본정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="사원명">{claim.employeeName}</Descriptions.Item>
          <Descriptions.Item label="사번">{claim.employeeCode}</Descriptions.Item>
          <Descriptions.Item label="거래처명">{claim.storeName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="등록일">{claim.createdAt?.substring(0, 16).replace('T', ' ')}</Descriptions.Item>
          <Descriptions.Item label="상태">
            {statusTag ? <Tag color={statusTag.color}>{statusTag.label}</Tag> : <Tag>{claim.status}</Tag>}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="제품정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="제품코드">{claim.productCode ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="제품명">{claim.productName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label={dateTypeLabel ?? '날짜'}>
            {dateTypeLabel && claim.date ? claim.date : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="불량수량">
            {claim.defectQuantity != null ? `${claim.defectQuantity} EA` : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="클레임 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="대분류">{claim.categoryName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="소분류">{claim.subcategoryName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="구매방법">{claim.purchaseMethodName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="구매금액">
            {claim.purchaseAmount != null ? `${claim.purchaseAmount.toLocaleString()}원` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="요청사항">{claim.requestTypeName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="불만내역" span={2}>
            {claim.defectDescription ?? '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="첨부사진">
        {claim.photos.length === 0 ? (
          <Typography.Text type="secondary">등록된 사진이 없습니다</Typography.Text>
        ) : (
          <Image.PreviewGroup>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
              {claim.photos.map((photo) => (
                <div key={photo.photoId} style={{ textAlign: 'center' }}>
                  {photo.photoType ? (
                    <Tag style={{ marginBottom: 8 }}>
                      {PHOTO_TYPE_LABEL[photo.photoType] ?? photo.photoType}
                    </Tag>
                  ) : null}
                  <div>{photo.originalFileName}</div>
                  <Image
                    width={200}
                    src={photo.url}
                    alt={photo.originalFileName ?? '사진'}
                    style={{ borderRadius: 4 }}
                  />
                </div>
              ))}
            </div>
          </Image.PreviewGroup>
        )}
      </Card>
    </div>
  );
}
