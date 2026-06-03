import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, Descriptions, Spin, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { useSalesProgressRateMaster } from '@/hooks/sales-progress-rate-master/useSalesProgressRateMaster';

const { Title } = Typography;

function formatAmount(value: number | null | undefined): string {
  return value != null ? value.toLocaleString() : '-';
}

function formatRate(value: number | null | undefined): string {
  // 매출 진도율(서버 산출 비율 current/sum, 예: 0.85) — 화면은 % 표기 위해 ×100.
  return value != null ? `${(value * 100).toFixed(1)}%` : '-';
}

function formatBusinessRate(value: number | null | undefined): string {
  // 영업일 기준 진도율(SF BusinessRate__c) — Trigger 가 이미 (영업일/전체)*100 한 값을 저장. 그대로 % 표기.
  return value != null ? `${value}%` : '-';
}

function formatDateTime(value: string | null | undefined): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

export default function SalesProgressRateMasterDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);

  const { data, isLoading } = useSalesProgressRateMaster(numericId);

  const goBack = () => {
    const listSearch = (location.state as { listSearch?: string } | null)?.listSearch ?? '';
    navigate(`/sales-progress-rate-masters${listSearch}`);
  };

  if (isLoading) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (!data) {
    return (
      <div style={{ padding: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={goBack}>
          목록으로
        </Button>
        <div style={{ marginTop: 24 }}>거래처목표등록마스터를 찾을 수 없습니다.</div>
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          {data.name ?? '거래처목표등록마스터'}
        </Title>
        <Button icon={<ArrowLeftOutlined />} onClick={goBack}>
          목록으로
        </Button>
      </div>

      <Descriptions bordered column={2} size="middle">
        <Descriptions.Item label="이름">{data.name ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="외부키">{data.externalKey ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="목표 년도">{data.targetYear ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="목표 월">{data.targetMonth ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="거래처">{data.accountName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="거래처지점명">{data.accountBranchName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="거래처코드">{data.accountCode ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="거래처유형">{data.accountType ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="상온 목표 금액">{formatAmount(data.rtTargetAmount)}</Descriptions.Item>
        <Descriptions.Item label="라면 목표 금액">{formatAmount(data.rmTargetAmount)}</Descriptions.Item>
        <Descriptions.Item label="냉동/냉장 목표 금액">{formatAmount(data.frTargetAmount)}</Descriptions.Item>
        <Descriptions.Item label="유지 목표 금액">{formatAmount(data.foTargetAmount)}</Descriptions.Item>
        <Descriptions.Item label="합계 목표 금액">{formatAmount(data.targetSum)}</Descriptions.Item>
        <Descriptions.Item label="합계 목표(미사용)">{formatAmount(data.targetSumAmount)}</Descriptions.Item>
        <Descriptions.Item label="당월 매출 실적">{formatAmount(data.currentMonthSalesAmount)}</Descriptions.Item>
        <Descriptions.Item label="전월 매출 실적">{formatAmount(data.previousMonthSalesAmount)}</Descriptions.Item>
        <Descriptions.Item label="매출 진도율">{formatRate(data.progressRate)}</Descriptions.Item>
        <Descriptions.Item label="영업일 기준 진도율">{formatBusinessRate(data.businessRate)}</Descriptions.Item>
        <Descriptions.Item label="작성자">{data.createdByName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="최종 수정자">{data.lastModifiedByName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="작성 일시">{formatDateTime(data.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="수정 일시">{formatDateTime(data.updatedAt)}</Descriptions.Item>
      </Descriptions>
    </div>
  );
}
