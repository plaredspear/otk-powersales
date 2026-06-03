import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Checkbox, DatePicker, Input, Select, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { usePromotions } from '@/hooks/promotion/usePromotions';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { usePermission } from '@/hooks/usePermission';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import type { PromotionListItem } from '@/api/promotion';
import dayjs from 'dayjs';
import 'dayjs/locale/ko';
import ResizableTable from '@/components/common/ResizableTable';
import SavedSearchBar from '@/components/savedSearch/SavedSearchBar';

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

function formatDate(value: string): string {
  return dayjs(value).format('YYYY-MM-DD');
}

function formatDateTime(value: string): string {
  return dayjs(value).locale('ko').format('YYYY. M. D. A h:mm');
}

export default function PromotionListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('promotion', 'EDIT');
  // 작성자 → 사용자 상세(/users/:id) 링크는 user READ 권한 보유자(시스템 관리자급)에게만.
  // SF 레거시 동등 + 신규 user 조회가 관리자 전용이라, 미보유자(조장/사원)는 이름 텍스트만 노출.
  const canReadUser = hasEntityPermission('user', 'READ');
  // page/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, filters, setFilter, setFilters } = useListQueryParams({
    defaultFilters: { promotionType: '', startDate: '', endDate: '', keyword: '', ownerOnly: '' },
  });
  const { promotionType, startDate, endDate, keyword, ownerOnly } = filters;

  // 저장된 검색 적용 — 모든 필터 키를 명시적으로 덮어써 이전 조건 잔존을 막는다.
  const applySavedSearch = (saved: Record<string, string>) => {
    setFilters({
      promotionType: saved.promotionType ?? '',
      startDate: saved.startDate ?? '',
      endDate: saved.endDate ?? '',
      keyword: saved.keyword ?? '',
      ownerOnly: saved.ownerOnly ?? '',
    });
  };

  // 저장 대상 필터 + 사람이 읽는 미리보기.
  const savedFilters: Record<string, string> = { promotionType, startDate, endDate, keyword, ownerOnly };
  const savedPreview = [
    { label: '행사유형', value: promotionType || '전체' },
    { label: '시작일', value: startDate },
    { label: '종료일', value: endDate },
    { label: '검색어', value: keyword },
    { label: '범위', value: ownerOnly === 'true' ? '내 행사만' : '전체' },
  ];

  const { data: formMeta } = usePromotionFormMeta();
  // 상세 진입 시 현재 목록의 query string 을 state 로 넘겨, 상세의 "목록으로" 버튼이 직전 조건으로 복귀하게 한다.
  const goToDetail = useThrottleClick((id: number) =>
    navigate(`/promotions/${id}`, { state: { listSearch: location.search } }),
  );
  const goToProduct = useThrottleClick((productCode: string) =>
    navigate(`/product/${encodeURIComponent(productCode)}`, { state: { listSearch: location.search } }),
  );
  const goToUser = useThrottleClick((userId: number) =>
    navigate(`/users/${userId}`, { state: { listSearch: location.search } }),
  );
  const handleCreate = useThrottleClick(() => navigate('/promotions/new'));
  const { data, isLoading } = usePromotions({
    keyword: keyword || undefined,
    promotionType: promotionType || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    ownerOnly: ownerOnly === 'true' || undefined,
    page,
    size: 20,
  });

  const promotionTypeOptions = [
    { value: '', label: '전체' },
    ...(formMeta?.promotionTypes.map((t) => ({ value: t.name, label: t.name })) ?? []),
  ];

  const columns: ColumnsType<PromotionListItem> = [
    {
      title: '행사번호',
      dataIndex: 'promotionNumber',
      width: 150,
      fixed: 'left',
      render: (val: string, record) => (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          <a onClick={() => goToDetail(record.id)}>{val}</a>
          <Typography.Text copyable={{ text: val, tooltips: ['행사번호 복사', '복사됨'] }} />
        </span>
      ),
    },
    {
      title: '행사명',
      dataIndex: 'promotionName',
      width: 200,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 160,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '대표제품',
      dataIndex: 'primaryProductName',
      width: 180,
      ellipsis: true,
      render: (val: string | null, record) =>
        val && record.primaryProductCode ? (
          <a onClick={() => goToProduct(record.primaryProductCode!)}>{val}</a>
        ) : (
          val ?? '-'
        ),
    },
    {
      title: '시작일',
      dataIndex: 'startDate',
      width: 110,
      align: 'center',
      render: formatDate,
    },
    {
      title: '종료일',
      dataIndex: 'endDate',
      width: 110,
      align: 'center',
      render: formatDate,
    },
    {
      title: '거래처코드',
      dataIndex: 'accountCode',
      width: 120,
      align: 'center',
      render: (val: string | null) =>
        val ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            {val}
            <Typography.Text copyable={{ text: val, tooltips: ['거래처코드 복사', '복사됨'] }} />
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '행사유형',
      dataIndex: 'promotionType',
      width: 90,
      align: 'center',
      render: (val: string | null) => {
        if (!val) return <Tag>-</Tag>;
        const color = PROMOTION_TYPE_TAG[val] ?? undefined;
        return <Tag color={color}>{val}</Tag>;
      },
    },
    {
      title: '매대위치',
      dataIndex: 'standLocation',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '제품유형',
      dataIndex: 'category1',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: 'CC Code',
      dataIndex: 'costCenterCode',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '목표금액',
      dataIndex: 'targetAmount',
      width: 120,
      align: 'right',
      render: (val: number | null) => (val != null ? val.toLocaleString() : '-'),
    },
    {
      title: '실적금액 (원)',
      dataIndex: 'actualAmount',
      width: 120,
      align: 'right',
      render: (val: number | null) => (val != null ? val.toLocaleString() : '-'),
    },
    {
      title: '작성 일자',
      dataIndex: 'createdAt',
      width: 140,
      align: 'center',
      render: (val: string) => formatDateTime(val),
    },
    {
      title: '작성자',
      dataIndex: 'createdByName',
      width: 100,
      align: 'center',
      ellipsis: true,
      render: (val: string | null, record) =>
        val && canReadUser && record.createdById != null ? (
          <a onClick={() => goToUser(record.createdById!)}>{val}</a>
        ) : (
          val ?? '-'
        ),
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          marginBottom: 16,
        }}
      >
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            행사마스터 등록
          </Button>
        )}
      </div>

      <div style={{ marginBottom: 16 }}>
        <SavedSearchBar
          resourceKey="promotion"
          filters={savedFilters}
          preview={savedPreview}
          onApply={applySavedSearch}
        />
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 130 }}
          value={promotionType ?? ''}
          options={promotionTypeOptions}
          onChange={(val) => setFilter('promotionType', val || '')}
        />
        <DatePicker
          placeholder="시작일"
          value={startDate ? dayjs(startDate) : null}
          onChange={(date) => setFilter('startDate', date ? date.format('YYYY-MM-DD') : '')}
        />
        <DatePicker
          placeholder="종료일"
          value={endDate ? dayjs(endDate) : null}
          onChange={(date) => setFilter('endDate', date ? date.format('YYYY-MM-DD') : '')}
        />
        <Input.Search
          placeholder="행사명/행사번호 검색"
          allowClear
          defaultValue={keyword ?? ''}
          style={{ width: 250 }}
          onSearch={(val) => setFilter('keyword', val)}
        />
        <Checkbox
          checked={ownerOnly === 'true'}
          onChange={(e) => setFilter('ownerOnly', e.target.checked ? 'true' : '')}
          style={{ alignSelf: 'center' }}
        >
          내 행사만
        </Checkbox>
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 2030 }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </div>
  );
}
