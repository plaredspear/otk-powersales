import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Image, Spin, Tag, Typography, message } from 'antd';
import { useClaimDetail } from '@/hooks/claims/useClaimDetail';
import { useResendClaim } from '@/hooks/claims/useResendClaim';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

// status : SF DKRetail__Status__c — 코스모스(고객상담 처리 시스템) 전송상태. 표시 전용.
const STATUS_TAG: Record<string, { color: string; label: string }> = {
  DRAFT: { color: 'default', label: '임시저장' },
  SENT: { color: 'green', label: '전송완료' },
  SEND_FAILED: { color: 'red', label: '전송실패' },
  // 레거시/구버전 호환
  SUBMITTED: { color: 'blue', label: '접수' },
  IN_PROGRESS: { color: 'orange', label: '처리중' },
  RESOLVED: { color: 'green', label: '처리완료' },
  REJECTED: { color: 'red', label: '반려' },
};

// sfSendStatus : 신규→SF 전송상태. null 이면 SF origin 마이그레이션 건(재전송 대상 아님) → 미표시.
const SF_SEND_STATUS_TAG: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'processing', label: '전송대기' },
  SENT: { color: 'green', label: '전송완료' },
  SEND_FAILED: { color: 'red', label: '전송실패' },
};

const DATE_TYPE_LABEL: Record<string, string> = {
  EXPIRY_DATE: '유통기한',
  MANUFACTURE_DATE: '제조일자',
};

/** 값이 없으면 '-' 로 표시. */
const orDash = (v: string | number | null | undefined): string =>
  v === null || v === undefined || v === '' ? '-' : String(v);

/** 'YYYY-MM-DDTHH:mm:ss...' → 'YYYY-MM-DD HH:mm'. */
const fmtDateTime = (v: string | null | undefined): string =>
  v ? v.substring(0, 16).replace('T', ' ') : '-';

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
  const { mutate: resendClaim, isPending: isResending } = useResendClaim();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  const handleResend = () => {
    resendClaim(id, {
      onSuccess: (data) => {
        if (data.sfSendStatus === 'SENT') {
          message.success('알라딘 재전송에 성공했습니다');
        } else {
          message.warning(`알라딘 재전송에 실패했습니다: ${data.sfResultMsg ?? '연동 오류'}`);
        }
      },
      onError: (err) => {
        message.error(`재전송 실패: ${err.message}`);
      },
    });
  };

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
  const sfSendStatusTag = claim.sfSendStatus ? SF_SEND_STATUS_TAG[claim.sfSendStatus] : null;
  const dateTypeLabel = claim.dateType ? DATE_TYPE_LABEL[claim.dateType] : null;

  return (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Button type="link" onClick={() => navigate('/claims')} style={{ paddingLeft: 0 }}>
          ← 목록
        </Button>
      </div>

      {claim.sfSendStatus === 'SEND_FAILED' && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="알라딘 전송 실패"
          description="이 클레임은 알라딘에 전송되지 않았습니다. 우측 버튼으로 재전송하세요."
          action={
            <Button danger size="small" loading={isResending} onClick={handleResend}>
              알라딘 재전송
            </Button>
          }
        />
      )}

      <Card title="기본정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="접수번호">{orDash(claim.claimNo)}</Descriptions.Item>
          <Descriptions.Item label="코스모스전송상태">
            {statusTag ? <Tag color={statusTag.color}>{statusTag.label}</Tag> : <Tag>{claim.status}</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="알라딘전송상태">
            {sfSendStatusTag ? (
              <Tag color={sfSendStatusTag.color}>{sfSendStatusTag.label}</Tag>
            ) : (
              orDash(null)
            )}
          </Descriptions.Item>
          <Descriptions.Item label="사원명">{claim.employeeName}</Descriptions.Item>
          <Descriptions.Item label="사번">{claim.employeeCode}</Descriptions.Item>
          <Descriptions.Item label="거래처명">{orDash(claim.storeName)}</Descriptions.Item>
          <Descriptions.Item label="등록일">{fmtDateTime(claim.createdAt)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="제품정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="제품코드">{orDash(claim.productCode)}</Descriptions.Item>
          <Descriptions.Item label="제품명">{orDash(claim.productName)}</Descriptions.Item>
          <Descriptions.Item label="제조일자">{orDash(claim.manufacturingDate)}</Descriptions.Item>
          <Descriptions.Item label="유통기한">{orDash(claim.expirationDate)}</Descriptions.Item>
          <Descriptions.Item label="출고처">{orDash(claim.logisticsCenter)}</Descriptions.Item>
          <Descriptions.Item label="주문번호">{orDash(claim.orderNumber)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="클레임 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="대분류">{orDash(claim.categoryLabel)}</Descriptions.Item>
          <Descriptions.Item label="소분류">{orDash(claim.subcategoryLabel)}</Descriptions.Item>
          <Descriptions.Item label="불량수량">
            {claim.defectQuantity != null ? `${claim.defectQuantity} EA` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="샘플회수여부">
            {claim.sampleCollectionFlag == null ? '-' : claim.sampleCollectionFlag ? '회수' : '미회수'}
          </Descriptions.Item>
          <Descriptions.Item label={dateTypeLabel ? `발생일자(${dateTypeLabel})` : '발생일자'}>
            {orDash(claim.date)}
          </Descriptions.Item>
          <Descriptions.Item label="거래처납품일자">{orDash(claim.customerDeliveryDate)}</Descriptions.Item>
          <Descriptions.Item label="세부점포명">{orDash(claim.detailSnsName)}</Descriptions.Item>
          <Descriptions.Item label="구매방법">{orDash(claim.purchaseMethodName)}</Descriptions.Item>
          <Descriptions.Item label="구매금액">
            {claim.purchaseAmount != null ? `${claim.purchaseAmount.toLocaleString()}원` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="요청사항">{orDash(claim.requestTypeName)}</Descriptions.Item>
          <Descriptions.Item label="부서">{orDash(claim.division)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="불만 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={1}>
          <Descriptions.Item label="불만내역">{orDash(claim.defectDescription)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="채널정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="접수채널">{orDash(claim.channelLabel ?? claim.channel)}</Descriptions.Item>
          <Descriptions.Item label="전송일자">{fmtDateTime(claim.interfaceDate)}</Descriptions.Item>
          <Descriptions.Item label="작성자">{orDash(claim.employeeName)}</Descriptions.Item>
          <Descriptions.Item label="직위">{orDash(claim.jikwee)}</Descriptions.Item>
          <Descriptions.Item label="영업사원 연락처">{orDash(claim.employeePhone)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="처리/조치 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="상담번호">{orDash(claim.counselNumber)}</Descriptions.Item>
          <Descriptions.Item label="조치코드">{orDash(claim.actionCode)}</Descriptions.Item>
          <Descriptions.Item label="조치상태">{orDash(claim.actionStatus)}</Descriptions.Item>
          <Descriptions.Item label="원인별 분류">{orDash(claim.reasonType)}</Descriptions.Item>
          <Descriptions.Item label="조치내용" span={2}>
            {orDash(claim.actContent)}
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
