import { Alert, Button, Card, Descriptions, Empty, Skeleton, Space, Spin, Tabs, Tag } from 'antd';
import type { TabsProps } from 'antd';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAccountDetail } from '@/hooks/account/useAccountDetail';
import { usePermission } from '@/hooks/usePermission';
import { fetchDetail } from '@/api/monthlySalesDashboard';
import MonthlySalesDetailBody from './MonthlySalesDetailBody';
import type { AccountDetail } from '@/api/account';

const ABC_TYPE_TAG: Record<string, string> = {
  대형마트: 'blue',
  슈퍼: 'green',
  편의점: 'orange',
};

const STATUS_TAG: Record<string, string> = {
  활성: 'green',
  비활성: 'red',
  거래: 'green',
  폐업: 'red',
};

/** 빈 값 표시 — null/빈 문자열은 '-'. */
function dash(v: string | number | null | undefined): string {
  if (v === null || v === undefined) return '-';
  const s = String(v).trim();
  return s === '' ? '-' : s;
}

/** BigDecimal 문자열 → 천 단위 콤마. 숫자가 아니면 원본 표시. */
function won(v: string | null): string {
  if (v === null || v.trim() === '') return '-';
  const n = Number(v);
  return Number.isFinite(n) ? `${n.toLocaleString()}원` : v;
}

/**
 * 거래처 상세 페이지 (`/account/:id`).
 *
 * 레거시 SF Account 레코드 페이지(`Account_Record_Page`) 동등 — "기본 정보" + "매출 이력" 탭.
 * 매출 이력 탭은 `monthly_sales_history` READ 권한 보유자에게만 노출 (SF 매출 그래프 컴포넌트가
 * 별도 권한 컨텍스트였던 것과 정합). 수금/여신/원장 탭은 현 시점 데이터 소스 부재로 미포함.
 */
export default function AccountDetailPage() {
  const { id } = useParams<{ id: string }>();
  const accountId = id ? Number(id) : undefined;
  const navigate = useNavigate();
  const location = useLocation();
  const { hasEntityPermission } = usePermission();
  const canViewSales = hasEntityPermission('monthly_sales_history', 'READ');

  // 목록에서 넘어온 경우 직전 목록의 query string(page/필터)을 붙여 복귀 — "목록으로" 시 조건 초기화 방지.
  const listSearch = (location.state as { listSearch?: string } | null)?.listSearch ?? '';
  const listPath = `/account${listSearch}`;

  const { data, isLoading, isError, error, refetch } = useAccountDetail(accountId);

  if (isLoading) {
    return (
      <div style={{ padding: 24 }}>
        <Skeleton active />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="거래처 상세를 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={
            <Space>
              <Button onClick={() => refetch()}>재시도</Button>
              <Button onClick={() => navigate(listPath)}>목록으로</Button>
            </Space>
          }
        />
      </div>
    );
  }

  const tabItems: TabsProps['items'] = [
    {
      key: 'info',
      label: '기본 정보',
      children: <AccountInfoTab account={data} />,
    },
    ...(canViewSales
      ? [
          {
            key: 'sales',
            label: '매출 이력',
            children: <AccountSalesTab accountId={data.id} accountName={data.name} />,
          },
        ]
      : []),
  ];

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate(listPath)}>← 목록으로</Button>
      </Space>

      <Card
        title={
          <Space>
            <span>{data.name ?? '거래처 상세'}</span>
            {data.accountStatusName && (
              <Tag color={STATUS_TAG[data.accountStatusName] ?? undefined}>
                {data.accountStatusName}
              </Tag>
            )}
          </Space>
        }
      >
        <Tabs items={tabItems} />
      </Card>
    </div>
  );
}

function AccountInfoTab({ account }: { account: AccountDetail }) {
  return (
    <div>
      <Card size="small" title="기본 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="거래처코드">{dash(account.externalKey)}</Descriptions.Item>
          <Descriptions.Item label="거래처명">{dash(account.name)}</Descriptions.Item>
          <Descriptions.Item label="ABC유형">
            {account.abcType ? (
              <Tag color={ABC_TYPE_TAG[account.abcType] ?? undefined}>{account.abcType}</Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="거래처 그룹">{dash(account.accountGroup)}</Descriptions.Item>
          <Descriptions.Item label="지점코드">{dash(account.branchCode)}</Descriptions.Item>
          <Descriptions.Item label="지점명">{dash(account.branchName)}</Descriptions.Item>
          <Descriptions.Item label="담당사원">{dash(account.employeeCode)}</Descriptions.Item>
          <Descriptions.Item label="대표자">{dash(account.representative)}</Descriptions.Item>
          <Descriptions.Item label="사업자번호">
            {dash(account.businessLicenseNumber)}
          </Descriptions.Item>
          <Descriptions.Item label="업태">{dash(account.businessType)}</Descriptions.Item>
          <Descriptions.Item label="업종">{dash(account.businessCategory)}</Descriptions.Item>
          <Descriptions.Item label="산업">{dash(account.industry)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card size="small" title="연락처 / 주소" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="전화번호">{dash(account.phone)}</Descriptions.Item>
          <Descriptions.Item label="휴대전화">{dash(account.mobilePhone)}</Descriptions.Item>
          <Descriptions.Item label="팩스">{dash(account.fax)}</Descriptions.Item>
          <Descriptions.Item label="이메일">{dash(account.email)}</Descriptions.Item>
          <Descriptions.Item label="웹사이트">{dash(account.website)}</Descriptions.Item>
          <Descriptions.Item label="우편번호">{dash(account.zipCode)}</Descriptions.Item>
          <Descriptions.Item label="주소" span={2}>
            {dash(account.address1)}
            {account.address2 ? ` ${account.address2}` : ''}
          </Descriptions.Item>
          <Descriptions.Item label="좌표">
            {account.latitude && account.longitude
              ? `${account.latitude}, ${account.longitude}`
              : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card size="small" title="거래 / 여신 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="상태">{dash(account.accountStatusName)}</Descriptions.Item>
          <Descriptions.Item label="거래처 유형">{dash(account.accountType)}</Descriptions.Item>
          <Descriptions.Item label="여신 한도">{won(account.totalCredit)}</Descriptions.Item>
          <Descriptions.Item label="여신 잔액">{won(account.remainingCredit)}</Descriptions.Item>
          <Descriptions.Item label="냉동고 설치">
            {account.freezerInstalled === null
              ? '-'
              : account.freezerInstalled
                ? '설치'
                : '미설치'}
          </Descriptions.Item>
          <Descriptions.Item label="냉동고 유형">{dash(account.freezerType)}</Descriptions.Item>
          <Descriptions.Item label="최초 설치일">{dash(account.firstInstalled)}</Descriptions.Item>
          <Descriptions.Item label="주문 마감시간">{dash(account.orderEndTime)}</Descriptions.Item>
          <Descriptions.Item label="마감시간1">{dash(account.closingTime1)}</Descriptions.Item>
          <Descriptions.Item label="마감시간2">{dash(account.closingTime2)}</Descriptions.Item>
          <Descriptions.Item label="마감시간3">{dash(account.closingTime3)}</Descriptions.Item>
          <Descriptions.Item label="배송 구분">{dash(account.distribution)}</Descriptions.Item>
        </Descriptions>
      </Card>

      {account.description && (
        <Card size="small" title="비고">
          <div style={{ whiteSpace: 'pre-wrap' }}>{account.description}</div>
        </Card>
      )}
    </div>
  );
}

/**
 * 매출 이력 탭 — 당월 기준 월매출 단건 상세를 임베드 (월매출 대시보드 상세와 동일 본문).
 *
 * 월매출 상세 API(`GET /api/v1/admin/sales/monthly/detail/{customerId}`)는 `monthly_sales_history`
 * READ 권한으로 가드되므로, 본 탭은 해당 권한 보유 시에만 [AccountDetailPage] 가 렌더한다.
 */
function AccountSalesTab({
  accountId,
  accountName,
}: {
  accountId: number;
  accountName: string | null;
}) {
  // 당월 기준 — 거래처 상세는 별도 년월 선택 컨텍스트가 없으므로 현재 월로 고정 조회.
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;

  const detailQuery = useQuery({
    queryKey: ['account', 'sales-detail', accountId, year, month],
    queryFn: () => fetchDetail(accountId, year, month),
  });

  if (detailQuery.isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (detailQuery.isError) {
    return (
      <Alert
        type="error"
        message="매출 이력을 불러오지 못했습니다"
        description={(detailQuery.error as Error)?.message}
        action={<Button onClick={() => detailQuery.refetch()}>재시도</Button>}
      />
    );
  }

  if (!detailQuery.data) {
    return <Empty description={`${accountName ?? '거래처'}의 매출 데이터가 없습니다`} />;
  }

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={`기준 월: ${year}-${String(month).padStart(2, '0')}`}
      />
      <MonthlySalesDetailBody detail={detailQuery.data} />
    </div>
  );
}
