import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  Skeleton,
  Space,
  Spin,
  Tabs,
  Tag,
  Tooltip,
  notification,
} from 'antd';
import type { TabsProps } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { isAxiosError } from 'axios';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAccountDetail } from '@/hooks/account/useAccountDetail';
import { useUpdateAccountMutation } from '@/hooks/account/useUpdateAccountMutation';
import { usePermission } from '@/hooks/usePermission';
import { isApiErrorBody } from '@/api/types';
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
  const canEditAccount = hasEntityPermission('account', 'EDIT');

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
      children: <AccountInfoTab account={data} canEdit={canEditAccount} />,
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

function AccountInfoTab({ account, canEdit }: { account: AccountDetail; canEdit: boolean }) {
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
          <Descriptions.Item
            label={
              <span>
                모바일앱 &gt; 주문가능여부{' '}
                <Tooltip title="거래처의 ABC유형이 주문 가능 유형에 속하는 경우, 모바일앱에서 주문 거래처로 선택할 수 있습니다.">
                  <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help', fontSize: 14 }} />
                </Tooltip>
              </span>
            }
          >
            {account.orderableType ? (
              <Tag color="green">주문가능</Tag>
            ) : (
              <Tag>주문불가</Tag>
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
          <Descriptions.Item label="상위 계정">{dash(account.parentName)}</Descriptions.Item>
          <Descriptions.Item label="거래처 소유자">{dash(account.ownerName)}</Descriptions.Item>
          <Descriptions.Item label="위탁거래여부">{dash(account.consignmentAcc)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card size="small" title="조직 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="거래처사업부CC코드">
            {dash(account.divisionCostCenter)}
          </Descriptions.Item>
          <Descriptions.Item label="거래처사업부명">{dash(account.divisionName)}</Descriptions.Item>
          <Descriptions.Item label="거래처영업부CC코드">
            {dash(account.salesDeptCostCenter)}
          </Descriptions.Item>
          <Descriptions.Item label="거래처영업부명">{dash(account.salesDeptName)}</Descriptions.Item>
          <Descriptions.Item label="거래처지점CC코드">
            {dash(account.branchCostCenter)}
          </Descriptions.Item>
          <Descriptions.Item label="거래처지점명">{dash(account.branchName)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card size="small" title="물류센터 정보" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="물류센터_상온">{dash(account.werk1)}</Descriptions.Item>
          <Descriptions.Item label="물류센터명_상온">{dash(account.werk1Tx)}</Descriptions.Item>
          <Descriptions.Item label="물류센터_냉장">{dash(account.werk2)}</Descriptions.Item>
          <Descriptions.Item label="물류센터명_냉장">{dash(account.werk2Tx)}</Descriptions.Item>
          <Descriptions.Item label="물류센터_냉동">{dash(account.werk3)}</Descriptions.Item>
          <Descriptions.Item label="물류센터명_냉동">{dash(account.werk3Tx)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <AccountAddressCard account={account} canEdit={canEdit} />

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

interface AddressFormValues {
  zipCode?: string;
  address1?: string;
  address2?: string;
}

/**
 * "연락처 / 주소" 카드 — 거래처 상세에서 **주소(우편번호/주소1/주소2)만** 인라인 수정.
 *
 * 거래처 수정은 상세 페이지로 일원화되었고, 상세 페이지에서 변경 가능한 항목은 주소로 한정한다
 * (거래처명·담당사원·연락처 등 다른 정보는 SAP/영업 시스템이 권위 출처라 본 화면에서 변경 불가).
 * 전화/이메일 등 연락처 항목은 읽기 전용으로 함께 표시한다.
 *
 * 저장은 `PUT /api/v1/admin/accounts/{id}` (account EDIT 권한) — 주소 3개 필드만 body 에 포함.
 * 주소 변경 시 좌표(latitude/longitude)는 Backend 가 감지해 재산출하므로 클라이언트는 전송하지 않는다.
 */
function AccountAddressCard({ account, canEdit }: { account: AccountDetail; canEdit: boolean }) {
  const [form] = Form.useForm<AddressFormValues>();
  const [editing, setEditing] = useState(false);
  const mutation = useUpdateAccountMutation();

  const startEdit = () => {
    form.setFieldsValue({
      zipCode: account.zipCode ?? '',
      address1: account.address1 ?? '',
      address2: account.address2 ?? '',
    });
    mutation.reset();
    setEditing(true);
  };

  const cancelEdit = () => {
    form.resetFields();
    setEditing(false);
  };

  const handleSave = async () => {
    let values: AddressFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    // 변경된 주소 필드만 PUT body 에 포함 (미변경 = 미전송 = 보존).
    const payload: AddressFormValues = {};
    if ((values.zipCode ?? '') !== (account.zipCode ?? '')) payload.zipCode = values.zipCode ?? '';
    if ((values.address1 ?? '') !== (account.address1 ?? '')) payload.address1 = values.address1 ?? '';
    if ((values.address2 ?? '') !== (account.address2 ?? '')) payload.address2 = values.address2 ?? '';

    if (Object.keys(payload).length === 0) {
      notification.info({ message: '변경된 내용이 없습니다.' });
      setEditing(false);
      return;
    }

    try {
      await mutation.mutateAsync({ id: account.id, payload });
      notification.success({ message: '주소가 수정되었습니다.' });
      setEditing(false);
    } catch (err) {
      handleAddressError(err);
    }
  };

  const isSaving = mutation.isPending;

  return (
    <Card
      size="small"
      title="연락처 / 주소"
      style={{ marginBottom: 16 }}
      extra={
        canEdit &&
        (editing ? (
          <Space>
            <Button size="small" onClick={cancelEdit} disabled={isSaving}>
              취소
            </Button>
            <Button size="small" type="primary" loading={isSaving} onClick={handleSave}>
              저장
            </Button>
          </Space>
        ) : (
          <Button size="small" onClick={startEdit}>
            주소 수정
          </Button>
        ))
      }
    >
      {/* 카드 레벨 단일 Form — component={false} 라 <form> DOM 을 렌더하지 않아 Descriptions 의 table 구조를 깨지 않는다. */}
      <Form form={form} component={false}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="전화번호">{dash(account.phone)}</Descriptions.Item>
          <Descriptions.Item label="휴대전화">{dash(account.mobilePhone)}</Descriptions.Item>
          <Descriptions.Item label="팩스">{dash(account.fax)}</Descriptions.Item>
          <Descriptions.Item label="이메일">{dash(account.email)}</Descriptions.Item>
          <Descriptions.Item label="웹사이트">{dash(account.website)}</Descriptions.Item>
          {editing ? (
            <>
              <Descriptions.Item label="우편번호">
                <Form.Item
                  name="zipCode"
                  noStyle
                  rules={[{ max: 100, message: '우편번호는 100자 이하여야 합니다.' }]}
                >
                  <Input maxLength={100} disabled={isSaving} placeholder="우편번호" />
                </Form.Item>
              </Descriptions.Item>
              <Descriptions.Item label="주소" span={2}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Form.Item
                    name="address1"
                    noStyle
                    rules={[{ max: 120, message: '주소1은 120자 이하여야 합니다.' }]}
                  >
                    <Input maxLength={120} disabled={isSaving} placeholder="주소1 (기본 주소)" />
                  </Form.Item>
                  <Form.Item
                    name="address2"
                    noStyle
                    rules={[{ max: 120, message: '주소2는 120자 이하여야 합니다.' }]}
                  >
                    <Input maxLength={120} disabled={isSaving} placeholder="주소2 (상세 주소)" />
                  </Form.Item>
                </Space>
              </Descriptions.Item>
            </>
          ) : (
            <>
              <Descriptions.Item label="우편번호">{dash(account.zipCode)}</Descriptions.Item>
              <Descriptions.Item label="주소" span={2}>
                {dash(account.address1)}
                {account.address2 ? ` ${account.address2}` : ''}
              </Descriptions.Item>
            </>
          )}
          <Descriptions.Item label="좌표">
            {account.latitude && account.longitude
              ? `${account.latitude}, ${account.longitude}`
              : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Form>
      {editing && (
        <Alert
          type="info"
          showIcon
          style={{ marginTop: 12 }}
          message="※ 주소 변경을 저장하면 좌표(위경도)가 변경된 주소 기준으로 자동 재산출됩니다."
        />
      )}
    </Card>
  );
}

/** 주소 수정 PUT 에러 표시 — 거래처 상세 주소 수정 한정 (name/employee 검증 분기는 발생하지 않음). */
function handleAddressError(err: unknown): void {
  if (isAxiosError(err)) {
    const status = err.response?.status;
    const body = err.response?.data;
    if (isApiErrorBody(body)) {
      const code = body.error!.code;
      const message = body.error!.message;
      if (code === 'ACCOUNT_NOT_FOUND') {
        notification.error({
          message: '주소 수정 실패',
          description: '거래처를 찾을 수 없습니다. 페이지를 새로고침해 주세요.',
        });
        return;
      }
      notification.error({ message: '주소 수정 실패', description: message });
      return;
    }
    if (status === 401) {
      // axios interceptor 가 로그인 리다이렉트 처리
      return;
    }
    if (status === 403) {
      notification.error({ message: '주소 수정 실패', description: '수정 권한이 없습니다.' });
      return;
    }
    if (status && status >= 500) {
      notification.error({
        message: '주소 수정 실패',
        description: '수정 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
      });
      return;
    }
  }
  notification.error({
    message: '주소 수정 실패',
    description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.',
  });
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
