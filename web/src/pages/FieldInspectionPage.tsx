import { useState } from 'react';
import {
  App,
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Image,
  Input,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useInspections, useDeleteInspection } from '@/hooks/inspections/useInspections';
import { useInspectionDetail } from '@/hooks/inspections/useInspectionDetail';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { usePermission } from '@/hooks/usePermission';
import InspectionCreateModal from '@/pages/inspection/InspectionCreateModal';
import type {
  InspectionCategory,
  InspectionFieldTypeCode,
  InspectionListItem,
  InspectionListParams,
} from '@/api/inspections';

const { RangePicker } = DatePicker;

const CATEGORY_OPTIONS: Array<{ value: InspectionCategory | ''; label: string }> = [
  { value: '', label: '전체' },
  { value: 'OWN', label: '자사' },
  { value: 'COMPETITOR', label: '경쟁사' },
];

const FIELD_TYPE_OPTIONS: Array<{ value: InspectionFieldTypeCode | ''; label: string }> = [
  { value: '', label: '전체' },
  { value: 'MAIN_SHELF', label: '본매대' },
  { value: 'EVENT_SHELF', label: '행사매대' },
  { value: 'TASTING', label: '시식' },
  { value: 'ETC', label: '기타' },
];

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  OWN: { color: 'blue', label: '자사' },
  COMPETITOR: { color: 'volcano', label: '경쟁사' },
};

const PAGE_SIZE = 20;

export default function FieldInspectionPage() {
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(30, 'day'), dayjs()]);
  const [category, setCategory] = useState<InspectionCategory | ''>('');
  const [fieldType, setFieldType] = useState<InspectionFieldTypeCode | ''>('');
  const [employeeName, setEmployeeName] = useState('');
  const [accountCode, setAccountCode] = useState('');
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);

  const { message } = App.useApp();
  const { hasEntityPermission } = usePermission();
  const canCreate = hasEntityPermission('site_activity', 'CREATE');
  const canEdit = hasEntityPermission('site_activity', 'EDIT');
  const canDelete = hasEntityPermission('site_activity', 'DELETE');
  const deleteMutation = useDeleteInspection();

  const [searchParams, setSearchParams] = useState<InspectionListParams>({
    startDate: dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD'),
    page: 0,
    size: PAGE_SIZE,
  });

  const { data, isLoading } = useInspections(searchParams);
  const { data: detail, isLoading: detailLoading } = useInspectionDetail(detailId);
  const openDetail = useThrottleClick((id: number) => setDetailId(id));

  const handleSearch = () => {
    setPage(0);
    setSearchParams({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      category: (category || undefined) as InspectionCategory | undefined,
      fieldType: (fieldType || undefined) as InspectionFieldTypeCode | undefined,
      employeeName: employeeName || undefined,
      accountCode: accountCode || undefined,
      page: 0,
      size: PAGE_SIZE,
    });
  };

  const handleReset = () => {
    setDateRange([dayjs().subtract(30, 'day'), dayjs()]);
    setCategory('');
    setFieldType('');
    setEmployeeName('');
    setAccountCode('');
  };

  const handlePageChange = (newPage: number) => {
    const zeroIndexedPage = newPage - 1;
    setPage(zeroIndexedPage);
    setSearchParams((prev) => ({ ...prev, page: zeroIndexedPage }));
  };

  const handleDelete = async () => {
    if (detailId == null) return;
    try {
      await deleteMutation.mutateAsync(detailId);
      message.success('현장점검이 삭제되었습니다');
      setDetailId(null);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '삭제에 실패했습니다');
    }
  };

  const dash = (val: string | null) => val ?? '-';
  const tastingLabel = (val: boolean | null) => (val == null ? '-' : val ? '예' : '아니오');

  const columns: ColumnsType<InspectionListItem> = [
    {
      title: 'No',
      width: 60,
      fixed: 'left',
      render: (_v, _r, index) => (searchParams.page ?? 0) * PAGE_SIZE + index + 1,
    },
    {
      title: '분류',
      dataIndex: 'category',
      width: 80,
      render: (val: string) => {
        const tag = CATEGORY_TAG[val];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : '-';
      },
    },
    { title: '점검일', dataIndex: 'inspectionDate', width: 110, render: dash },
    { title: '현장유형', dataIndex: 'fieldType', width: 100, render: dash },
    { title: '테마', dataIndex: 'themeName', width: 180, ellipsis: true, render: dash },
    { title: '거래처', dataIndex: 'accountName', width: 180, ellipsis: true, render: dash },
    { title: '소속', dataIndex: 'employeeOrgName', width: 120, render: dash },
    { title: '점검사원', dataIndex: 'employeeName', width: 100, render: dash },
    {
      title: '작성일자',
      dataIndex: 'createdAt',
      width: 110,
      render: (val: string) => val?.substring(0, 10) ?? '-',
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16, alignItems: 'center' }}>
        <Space wrap>
          <span>점검일:</span>
          <RangePicker
            value={dateRange}
            onChange={(dates) => {
              if (dates && dates[0] && dates[1]) {
                setDateRange([dates[0], dates[1]]);
              }
            }}
            format="YYYY-MM-DD"
          />
          <span>분류:</span>
          <Select
            style={{ width: 110 }}
            value={category}
            options={CATEGORY_OPTIONS}
            onChange={(v) => setCategory(v as InspectionCategory | '')}
          />
          <span>현장유형:</span>
          <Select
            style={{ width: 120 }}
            value={fieldType}
            options={FIELD_TYPE_OPTIONS}
            onChange={(v) => setFieldType(v as InspectionFieldTypeCode | '')}
          />
        </Space>
      </div>
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 8,
          marginBottom: 16,
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Space wrap>
          <span>점검사원명:</span>
          <Input
            placeholder="점검사원명"
            value={employeeName}
            onChange={(e) => setEmployeeName(e.target.value)}
            style={{ width: 140 }}
            onPressEnter={handleSearch}
          />
          <span>거래처 코드:</span>
          <Input
            placeholder="거래처 코드"
            value={accountCode}
            onChange={(e) => setAccountCode(e.target.value)}
            style={{ width: 140 }}
            onPressEnter={handleSearch}
          />
          <Button type="primary" onClick={handleSearch}>검색</Button>
          <Button onClick={handleReset}>초기화</Button>
        </Space>
        {canCreate && (
          <Button type="primary" onClick={() => setCreateOpen(true)}>
            현장점검 등록
          </Button>
        )}
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 1140 }}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: handlePageChange,
        }}
        onRow={(record) => ({
          onClick: () => openDetail(record.id),
          style: { cursor: 'pointer' },
        })}
      />

      <Drawer
        title="현장점검 상세"
        width={520}
        open={detailId != null}
        onClose={() => setDetailId(null)}
        loading={detailLoading}
        extra={
          detail && (
            <Space>
              {canEdit && (
                <Button onClick={() => setEditOpen(true)}>수정</Button>
              )}
              {canDelete && (
                <Popconfirm
                  title="이 현장점검을 삭제하시겠습니까?"
                  onConfirm={handleDelete}
                  okText="삭제"
                  cancelText="취소"
                >
                  <Button danger>삭제</Button>
                </Popconfirm>
              )}
            </Space>
          )
        }
      >
        {detail && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="분류">
              {CATEGORY_TAG[detail.category]?.label ?? detail.category}
            </Descriptions.Item>
            <Descriptions.Item label="테마">{dash(detail.themeName)}</Descriptions.Item>
            <Descriptions.Item label="점검일">{dash(detail.inspectionDate)}</Descriptions.Item>
            <Descriptions.Item label="현장유형">{dash(detail.fieldType)}</Descriptions.Item>
            <Descriptions.Item label="거래처">{dash(detail.accountName)}</Descriptions.Item>
            <Descriptions.Item label="점검사원">{dash(detail.employeeName)}</Descriptions.Item>
            <Descriptions.Item label="소속">{dash(detail.employeeOrgName)}</Descriptions.Item>
            {detail.category === 'OWN' && (
              <>
                <Descriptions.Item label="제품">{dash(detail.productName)}</Descriptions.Item>
                <Descriptions.Item label="설명">{dash(detail.description)}</Descriptions.Item>
              </>
            )}
            {detail.category === 'COMPETITOR' && (
              <>
                <Descriptions.Item label="경쟁사명">{dash(detail.competitorName)}</Descriptions.Item>
                <Descriptions.Item label="경쟁사 활동내용">{dash(detail.competitorActivity)}</Descriptions.Item>
                <Descriptions.Item label="시식여부">{tastingLabel(detail.competitorTasting)}</Descriptions.Item>
                <Descriptions.Item label="경쟁사 상품명">{dash(detail.competitorProductName)}</Descriptions.Item>
                <Descriptions.Item label="경쟁사 상품가격">
                  {detail.competitorProductPrice ?? '-'}
                </Descriptions.Item>
                <Descriptions.Item label="판매수량">
                  {detail.competitorSalesQuantity ?? '-'}
                </Descriptions.Item>
              </>
            )}
            <Descriptions.Item label="사진">
              {detail.photos.length > 0 ? (
                <Image.PreviewGroup>
                  <Space wrap>
                    {detail.photos.map((p) => (
                      <Image key={p.id} src={p.url} width={120} />
                    ))}
                  </Space>
                </Image.PreviewGroup>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="작성일자">
              {detail.createdAt?.substring(0, 19).replace('T', ' ') ?? '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      <InspectionCreateModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <InspectionCreateModal
        open={editOpen}
        editDetail={detail ?? null}
        onClose={() => setEditOpen(false)}
      />
    </div>
  );
}
