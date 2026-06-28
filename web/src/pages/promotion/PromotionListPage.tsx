import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, DatePicker, Input, Select, Space, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons';
import RefreshButton from '@/components/common/RefreshButton';
import { usePromotions } from '@/hooks/promotion/usePromotions';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { usePermission } from '@/hooks/usePermission';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { buildListPagination, PAGE_SIZE_OPTIONS } from '@/lib/listPagination';
import { promotionExportParams, type PromotionListItem } from '@/api/promotion';
import dayjs from 'dayjs';
import 'dayjs/locale/ko';
import ResizableTable from '@/components/common/ResizableTable';
import SavedSearchBar from '@/components/savedSearch/SavedSearchBar';

// 엑셀 다운로드 최대 건수 — 서버 export 상한(EXPORT_MAX_ROWS) 정합. 초과 시 안내 후 진행.
const EXPORT_MAX_ROWS = 50000;

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
  // page/필터/페이지 사이즈를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침/링크 공유 시 직전 조건 복원.
  // size 는 검색 조건이 아니라 표시 옵션이므로 savedFilters(저장된 검색)에는 포함하지 않는다.
  const { page, setPage, filters, setFilter, setFilters } = useListQueryParams({
    defaultFilters: {
      promotionType: '',
      startDate: '',
      endDate: '',
      keyword: '',
      accountName: '',
      accountNumber: '',
      category1: '',
      primaryProduct: '',
      employeeKeyword: '',
      ownerOnly: '',
      size: '50',
    },
  });
  // 페이지 사이즈 — URL 보관값을 숫자로 파싱(허용 옵션 외/비정상은 기본 50).
  const parsedSize = Number.parseInt(filters.size ?? '', 10);
  const pageSize = PAGE_SIZE_OPTIONS.includes(parsedSize) ? parsedSize : 50;
  const {
    promotionType,
    startDate,
    endDate,
    keyword,
    accountName,
    accountNumber,
    category1,
    primaryProduct,
    employeeKeyword,
    ownerOnly,
  } = filters;

  // 저장된 검색 적용 — 모든 필터 키를 명시적으로 덮어써 이전 조건 잔존을 막는다.
  const applySavedSearch = (saved: Record<string, string>) => {
    setFilters({
      promotionType: saved.promotionType ?? '',
      startDate: saved.startDate ?? '',
      endDate: saved.endDate ?? '',
      keyword: saved.keyword ?? '',
      accountName: saved.accountName ?? '',
      accountNumber: saved.accountNumber ?? '',
      category1: saved.category1 ?? '',
      primaryProduct: saved.primaryProduct ?? '',
      employeeKeyword: saved.employeeKeyword ?? '',
      ownerOnly: saved.ownerOnly ?? '',
    });
  };

  // 저장 대상 필터 + 사람이 읽는 미리보기.
  const savedFilters: Record<string, string> = {
    promotionType,
    startDate,
    endDate,
    keyword,
    accountName,
    accountNumber,
    category1,
    primaryProduct,
    employeeKeyword,
    ownerOnly,
  };
  const savedPreview = [
    { label: '행사유형', value: promotionType || '전체' },
    { label: '시작일', value: startDate },
    { label: '종료일', value: endDate },
    { label: '검색어', value: keyword },
    { label: '거래처', value: accountName },
    { label: '거래처번호', value: accountNumber },
    { label: '제품유형', value: category1 },
    { label: '대표제품', value: primaryProduct },
    { label: '행사사원', value: employeeKeyword },
    { label: '범위', value: ownerOnly === 'true' ? '내 행사만' : '전체' },
  ];

  const { data: formMeta } = usePromotionFormMeta();
  // 행사번호/대표제품 링크는 <Link>(href 부여)로 직접 이동 — Ctrl/Cmd/중간클릭 새 탭 지원.
  // state.listSearch 로 현재 목록 query string 을 넘겨 상세의 "목록으로" 가 직전 조건으로 복귀하게 한다.
  const goToUser = useThrottleClick((userId: number) =>
    navigate(`/users/${userId}`, { state: { listSearch: location.search } }),
  );
  const handleCreate = useThrottleClick(() => navigate('/promotions/new'));
  const { run: runExport, downloading: exporting } = useExcelDownload();
  const { data, isLoading, refetch, isFetching } = usePromotions({
    keyword: keyword || undefined,
    promotionType: promotionType || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    accountName: accountName || undefined,
    accountNumber: accountNumber || undefined,
    category1: category1 || undefined,
    primaryProduct: primaryProduct || undefined,
    employeeKeyword: employeeKeyword || undefined,
    ownerOnly: ownerOnly === 'true' || undefined,
    page,
    size: pageSize,
  });

  const promotionTypeOptions = [
    { value: '', label: '전체' },
    ...(formMeta?.promotionTypes.map((t) => ({ value: t.name, label: t.name })) ?? []),
  ];

  const handleExport = () =>
    runExport(
      '/api/v1/admin/promotions/export',
      `행사마스터_${dayjs().format('YYYYMMDD')}.xlsx`,
      {
        params: promotionExportParams({
          keyword: keyword || undefined,
          promotionType: promotionType || undefined,
          startDate: startDate || undefined,
          endDate: endDate || undefined,
          accountName: accountName || undefined,
          accountNumber: accountNumber || undefined,
          category1: category1 || undefined,
          primaryProduct: primaryProduct || undefined,
          employeeKeyword: employeeKeyword || undefined,
          ownerOnly: ownerOnly === 'true' || undefined,
        }),
        totalCount: data?.totalElements ?? 0,
        maxRows: EXPORT_MAX_ROWS,
      },
    );

  const columns: ColumnsType<PromotionListItem> = [
    {
      title: '행사번호',
      dataIndex: 'promotionNumber',
      width: 150,
      fixed: 'left',
      render: (val: string, record) => (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          {/* Link 로 실제 href 부여 — Ctrl/Cmd/중간클릭은 브라우저가 새 탭으로, 일반 클릭은 SPA 이동. */}
          <Link to={`/promotions/${record.id}`} state={{ listSearch: location.search }}>
            {val}
          </Link>
          <Typography.Text copyable={{ text: val, tooltips: ['행사번호 복사', '복사됨'] }} />
        </span>
      ),
    },
    {
      title: '행사명',
      dataIndex: 'promotionName',
      width: 200,
      fixed: 'left',
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '제품유형',
      dataIndex: 'category1',
      width: 100,
      fixed: 'left',
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 160,
      fixed: 'left',
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처코드',
      dataIndex: 'accountCode',
      width: 120,
      fixed: 'left',
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
      title: '대표제품',
      dataIndex: 'primaryProductName',
      width: 180,
      fixed: 'left',
      ellipsis: true,
      render: (val: string | null, record) =>
        val && record.primaryProductCode ? (
          // Link 로 실제 href 부여 — Ctrl/Cmd/중간클릭은 새 탭, 일반 클릭은 SPA 이동.
          <Link
            to={`/product/${encodeURIComponent(record.primaryProductCode)}`}
            state={{ listSearch: location.search }}
          >
            {val}
          </Link>
        ) : (
          val ?? '-'
        ),
    },
    {
      title: '시작일',
      dataIndex: 'startDate',
      width: 110,
      fixed: 'left',
      align: 'center',
      render: formatDate,
    },
    {
      title: '종료일',
      dataIndex: 'endDate',
      width: 110,
      fixed: 'left',
      align: 'center',
      render: formatDate,
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
      // 진도율 = 실적금액 / 목표금액 × 100. 목표가 0/null 이면 산출 불가('-').
      title: '진도율',
      key: 'progressRate',
      width: 90,
      align: 'right',
      render: (_: unknown, record) => {
        const { targetAmount, actualAmount } = record;
        if (targetAmount == null || targetAmount === 0 || actualAmount == null) return '-';
        return `${((actualAmount / targetAmount) * 100).toFixed(1)}%`;
      },
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
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          <Button icon={<DownloadOutlined />} onClick={handleExport} loading={exporting}>
            엑셀 다운로드
          </Button>
          {canWrite && (
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              행사마스터 등록
            </Button>
          )}
        </Space>
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
        <Input.Search
          key={`account-${accountName}`}
          placeholder="거래처명/거래처코드"
          allowClear
          defaultValue={accountName ?? ''}
          style={{ width: 180 }}
          onSearch={(val) => setFilter('accountName', val)}
        />
        <Input.Search
          key={`accountNumber-${accountNumber}`}
          placeholder="거래처번호"
          allowClear
          defaultValue={accountNumber ?? ''}
          style={{ width: 150 }}
          onSearch={(val) => setFilter('accountNumber', val)}
        />
        <Input.Search
          key={`category1-${category1}`}
          placeholder="제품유형"
          allowClear
          defaultValue={category1 ?? ''}
          style={{ width: 150 }}
          onSearch={(val) => setFilter('category1', val)}
        />
        <Input.Search
          key={`primaryProduct-${primaryProduct}`}
          placeholder="제품명/제품코드"
          allowClear
          defaultValue={primaryProduct ?? ''}
          style={{ width: 180 }}
          onSearch={(val) => setFilter('primaryProduct', val)}
        />
        <Input.Search
          key={`employeeKeyword-${employeeKeyword}`}
          placeholder="행사사원(사번/성명)"
          allowClear
          defaultValue={employeeKeyword ?? ''}
          style={{ width: 180 }}
          onSearch={(val) => setFilter('employeeKeyword', val)}
        />
        {/* "내 행사만" 체크박스 UI 는 임시 제거 (ownerOnly state/API/backend 로직은 유지 — 추후 재노출 대비). */}
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 2020 }}
        pagination={buildListPagination({
          // current 는 data?.page 기준이라 헬퍼의 page 인자에 data?.page 를 그대로 넘긴다.
          page: data?.page ?? 0,
          pageSize,
          total: data?.totalElements ?? 0,
          // 사이즈 변경 시 setFilter 가 page 를 0 으로 자동 리셋(useListQueryParams). 순수 이동은 setPage.
          onPageChange: setPage,
          onSizeChange: (size) => setFilter('size', String(size)),
        })}
      />
    </div>
  );
}
