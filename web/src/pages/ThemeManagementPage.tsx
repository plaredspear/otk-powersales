import { useState } from 'react';
import {
  App,
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { downloadThemeExcel, type ThemeListItem, type ThemeListParams } from '@/api/inspectionThemes';
import {
  useCreateTheme,
  useDeleteTheme,
  useThemeDetail,
  useThemes,
  useUpdateTheme,
} from '@/hooks/inspections/useThemes';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { usePermission } from '@/hooks/usePermission';

const PAGE_SIZE = 20;

interface ThemeFormValues {
  title: string;
  period?: [Dayjs, Dayjs] | null;
}

export default function ThemeManagementPage() {
  const { message } = App.useApp();
  const { hasEntityPermission } = usePermission();
  const canCreate = hasEntityPermission('inspection_theme', 'CREATE');
  const canEdit = hasEntityPermission('inspection_theme', 'EDIT');
  const canDelete = hasEntityPermission('inspection_theme', 'DELETE');

  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [searchParams, setSearchParams] = useState<ThemeListParams>({ page: 0, size: PAGE_SIZE });
  const [detailId, setDetailId] = useState<number | null>(null);
  const [editId, setEditId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<ThemeFormValues>();

  const { data, isLoading } = useThemes(searchParams);
  const { data: detail, isLoading: detailLoading } = useThemeDetail(detailId);
  const createMutation = useCreateTheme();
  const updateMutation = useUpdateTheme();
  const deleteMutation = useDeleteTheme();

  const openDetail = useThrottleClick((id: number) => setDetailId(id));

  const handleSearch = () => {
    setPage(0);
    setSearchParams({ keyword: keyword || undefined, page: 0, size: PAGE_SIZE });
  };

  const handlePageChange = (newPage: number) => {
    const zeroIndexed = newPage - 1;
    setPage(zeroIndexed);
    setSearchParams((prev) => ({ ...prev, page: zeroIndexed }));
  };

  const openCreate = () => {
    setEditId(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (item: ThemeListItem) => {
    setEditId(item.id);
    form.setFieldsValue({
      title: item.title ?? '',
      period:
        item.startDate && item.endDate
          ? [dayjs(item.startDate), dayjs(item.endDate)]
          : undefined,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const request = {
      title: values.title,
      startDate: values.period?.[0]?.format('YYYY-MM-DD') ?? null,
      endDate: values.period?.[1]?.format('YYYY-MM-DD') ?? null,
    };
    try {
      if (editId != null) {
        await updateMutation.mutateAsync({ id: editId, request });
        message.success('테마가 수정되었습니다');
      } else {
        await createMutation.mutateAsync(request);
        message.success('테마가 등록되었습니다');
      }
      setModalOpen(false);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '저장에 실패했습니다');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success('테마가 삭제되었습니다');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '삭제에 실패했습니다');
    }
  };

  const handleExport = async (item: ThemeListItem) => {
    try {
      await downloadThemeExcel(item.id, item.name ?? `theme_${item.id}`);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드에 실패했습니다');
    }
  };

  const dash = (val: string | null) => val ?? '-';

  const columns: ColumnsType<ThemeListItem> = [
    {
      title: '테마번호',
      dataIndex: 'name',
      width: 130,
      render: (val: string | null, record) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => openDetail(record.id)}>
          {val ?? '-'}
        </Button>
      ),
    },
    { title: '테마이름', dataIndex: 'title', ellipsis: true, render: dash },
    { title: '부서', dataIndex: 'department', width: 130, render: dash },
    { title: '시작일', dataIndex: 'startDate', width: 110, render: dash },
    { title: '종료일', dataIndex: 'endDate', width: 110, render: dash },
    { title: '소유자', dataIndex: 'ownerName', width: 110, render: dash },
    { title: '점검결과', dataIndex: 'siteActivityCount', width: 90, render: (v: number) => `${v}건` },
    {
      title: '관리',
      key: 'actions',
      width: 220,
      fixed: 'right',
      render: (_v, record) => (
        <Space size="small">
          <Button size="small" onClick={() => handleExport(record)}>
            엑셀
          </Button>
          {canEdit && (
            <Button size="small" onClick={() => openEdit(record)}>
              수정
            </Button>
          )}
          {canDelete && (
            <Popconfirm
              title="이 테마를 삭제하시겠습니까?"
              onConfirm={() => handleDelete(record.id)}
              okText="삭제"
              cancelText="취소"
            >
              <Button size="small" danger>
                삭제
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space>
          <Input
            placeholder="테마번호 / 테마이름 / 부서"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 260 }}
            allowClear
          />
          <Button type="primary" onClick={handleSearch}>
            검색
          </Button>
        </Space>
        {canCreate && (
          <Button type="primary" onClick={openCreate}>
            테마 등록
          </Button>
        )}
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 1100 }}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: handlePageChange,
        }}
      />

      <Modal
        title={editId != null ? '테마 수정' : '테마 등록'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        okText="저장"
        cancelText="취소"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="테마이름"
            name="title"
            rules={[{ required: true, message: '테마이름을 입력하세요' }]}
          >
            <Input maxLength={250} placeholder="테마이름" />
          </Form.Item>
          <Form.Item label="점검 기간" name="period">
            <DatePicker.RangePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="테마 상세"
        width={640}
        open={detailId != null}
        onClose={() => setDetailId(null)}
        loading={detailLoading}
      >
        {detail && (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="테마번호">{dash(detail.name)}</Descriptions.Item>
              <Descriptions.Item label="테마이름">{dash(detail.title)}</Descriptions.Item>
              <Descriptions.Item label="부서">{dash(detail.department)}</Descriptions.Item>
              <Descriptions.Item label="지점코드">{dash(detail.branchCode)}</Descriptions.Item>
              <Descriptions.Item label="시작일">{dash(detail.startDate)}</Descriptions.Item>
              <Descriptions.Item label="종료일">{dash(detail.endDate)}</Descriptions.Item>
              <Descriptions.Item label="소유자">{dash(detail.ownerName)}</Descriptions.Item>
            </Descriptions>

            <h4 style={{ margin: '20px 0 8px' }}>현장점검 결과 ({detail.siteActivities.length}건)</h4>
            <Table
              rowKey="id"
              size="small"
              dataSource={detail.siteActivities}
              pagination={false}
              scroll={{ x: 720 }}
              columns={[
                { title: '활동번호', dataIndex: 'name', width: 120, render: dash },
                { title: '거래처', dataIndex: 'accountName', ellipsis: true, render: dash },
                { title: '제품', dataIndex: 'productName', ellipsis: true, render: dash },
                { title: '소속', dataIndex: 'orgName', width: 110, render: dash },
                { title: '점검사원', dataIndex: 'employeeName', width: 90, render: dash },
                { title: '현장유형', dataIndex: 'category', width: 90, render: dash },
                { title: '점검일', dataIndex: 'activityDate', width: 110, render: dash },
              ]}
            />
          </>
        )}
      </Drawer>
    </div>
  );
}
