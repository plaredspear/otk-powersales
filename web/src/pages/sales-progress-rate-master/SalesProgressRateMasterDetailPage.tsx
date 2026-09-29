import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, Descriptions, Space, Spin, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { useSalesProgressRateMaster } from '@/hooks/sales-progress-rate-master/useSalesProgressRateMaster';
import { usePermission } from '@/hooks/usePermission';
import {
  MONTHLY_SALES_HISTORY_ENTITY,
  buildMonthlySalesSourceHref,
  toSalesMonthToken,
} from '@/lib/monthlySalesSourceLink';

const { Text, Title } = Typography;

function formatAmount(value: number | null | undefined): string {
  return value != null ? value.toLocaleString() : '-';
}

function formatRate(value: number | null | undefined): string {
  // 매출 진도율(서버 산출 비율 current/sum, 예: 0.85) — SF ProgressRate__c(scale=0)와 동일하게 소수점 없이 반올림한 정수 % 표기.
  return value != null ? `${Math.round(value * 100)}%` : '-';
}

function formatBusinessRate(value: number | null | undefined): string {
  // 영업일 기준 진도율(SF BusinessRate__c, scale=0) — Trigger 가 이미 (영업일/전체)*100 한 값을 저장. SF 와 동일하게 소수점 없이 반올림한 정수 % 표기.
  return value != null ? `${Math.round(value)}%` : '-';
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
  const { hasEntityPermission } = usePermission();
  const canViewMonthlySales = hasEntityPermission(MONTHLY_SALES_HISTORY_ENTITY, 'READ');

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

  /**
   * 실적 금액 + 원천 기준정보(ORORA 월매출) 링크.
   *
   * 당월/전월 실적은 입력값이 아니라 월매출이력의 마감 합계(전산합 + 물류합)에서 산출된 파생값이라,
   * 값의 근거를 같은 화면에서 바로 열어볼 수 있게 한다.
   *
   * @param monthOffset 0 = 당월(목표월), -1 = 전월.
   */
  const renderActual = (value: number | null | undefined, monthOffset: number) => {
    const salesMonth = toSalesMonthToken(data.targetYear, data.targetMonth, monthOffset);
    const sourceUpdatedAt =
      monthOffset === 0 ? data.currentMonthSourceUpdatedAt : data.previousMonthSourceUpdatedAt;
    // 원천 적재 시각 — 없으면 월매출이력 row 가 없어 SF 이관 시점 값을 보여주고 있다는 뜻이다.
    const source = (
      <Text type="secondary" style={{ fontSize: 12 }}>
        {sourceUpdatedAt
          ? `ORORA 적재 ${formatDateTime(sourceUpdatedAt)}`
          : 'ORORA 적재 없음 (SF 이관 값)'}
      </Text>
    );

    if (!canViewMonthlySales || !data.accountCode || !salesMonth) {
      return (
        <Space size={8}>
          <span>{formatAmount(value)}</span>
          {source}
        </Space>
      );
    }
    return (
      <Space size={8}>
        <span>{formatAmount(value)}</span>
        <Link to={buildMonthlySalesSourceHref(data.accountCode, salesMonth, data.accountName)}>
          {`${salesMonth.slice(0, 4)}.${salesMonth.slice(4, 6)} 월매출이력`}
        </Link>
        {source}
      </Space>
    );
  };

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
        <Descriptions.Item label="당월 매출 실적">
          {renderActual(data.currentMonthSalesAmount, 0)}
        </Descriptions.Item>
        <Descriptions.Item label="전월 매출 실적">
          {renderActual(data.previousMonthSalesAmount, -1)}
        </Descriptions.Item>
        <Descriptions.Item label="매출 진도율">{formatRate(data.progressRate)}</Descriptions.Item>
        <Descriptions.Item label="영업일 기준 진도율">{formatBusinessRate(data.businessRate)}</Descriptions.Item>
        <Descriptions.Item label="작성자">{data.createdByName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="최종 수정자">{data.lastModifiedByName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="작성 일시">{formatDateTime(data.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="수정 일시">{formatDateTime(data.updatedAt)}</Descriptions.Item>
      </Descriptions>

      <div style={{ marginTop: 12 }}>
        <Text type="secondary">
          당월/전월 매출 실적은 저장값이 아니라 조회 시점에 기준정보 &gt; ORORA 월매출(월매출이력)의 마감
          합계(전산마감 합계 + 물류마감 합계)에서 산출됩니다. 매출 진도율 = 당월 매출 실적 ÷ 합계 목표 금액.
        </Text>
      </div>
    </div>
  );
}
