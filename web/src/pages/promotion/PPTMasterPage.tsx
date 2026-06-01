import { useState } from 'react';
import { Button, Card, Checkbox, Input, Popconfirm, Select, Space, Tag, message } from 'antd';
import { PlusOutlined, DownloadOutlined, UploadOutlined, CheckOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  usePPTMasters,
  useDeletePPTMaster,
  useConfirmPPTMastersByIds,
} from '@/hooks/promotion/usePPTMasters';
import { downloadPPTMasterTemplate, exportPPTMasters } from '@/api/pptMaster';
import type { PPTMaster, PPTMasterSearchParams } from '@/api/pptMaster';
import PPTMasterFormModal from './components/PPTMasterFormModal';
import PPTMasterUploadModal from './components/PPTMasterUploadModal';
import {
  PPT_TEAM_TYPE_OPTIONS,
  getPPTTeamTypeColor,
} from '@/constants/pptTeamType';
import ResizableTable from '@/components/common/ResizableTable';

const TEAM_TYPE_FILTER_OPTIONS = [{ value: '', label: '전체' }, ...PPT_TEAM_TYPE_OPTIONS];

const DEFAULT_PARAMS: PPTMasterSearchParams = {
  page: 0,
  size: 20,
  validOnly: true,
};

export default function PPTMasterPage() {
  const [searchParams, setSearchParams] = useState<PPTMasterSearchParams>(DEFAULT_PARAMS);
  const [filterEmployeeName, setFilterEmployeeName] = useState('');
  const [filterEmployeeNumber, setFilterEmployeeNumber] = useState('');
  const [filterTeamType, setFilterTeamType] = useState('');
  const [filterValidOnly, setFilterValidOnly] = useState(true);

  const { data, isLoading } = usePPTMasters(searchParams);
  const deleteMutation = useDeletePPTMaster();
  const confirmByIdsMutation = useConfirmPPTMastersByIds();

  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<PPTMaster | null>(null);
  const [cloneSource, setCloneSource] = useState<PPTMaster | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);

  const handleSearch = () => {
    setSearchParams({
      ...searchParams,
      page: 0,
      employeeName: filterEmployeeName || undefined,
      employeeCode: filterEmployeeNumber || undefined,
      teamType: filterTeamType || undefined,
      validOnly: filterValidOnly,
    });
  };

  const handleReset = () => {
    setFilterEmployeeName('');
    setFilterEmployeeNumber('');
    setFilterTeamType('');
    setFilterValidOnly(true);
    setSearchParams(DEFAULT_PARAMS);
  };

  const handleAdd = () => {
    setEditingItem(null);
    setCloneSource(null);
    setFormOpen(true);
  };

  const handleEdit = (record: PPTMaster) => {
    setEditingItem(record);
    setCloneSource(null);
    setFormOpen(true);
  };

  const handleClone = (record: PPTMaster) => {
    setEditingItem(null);
    setCloneSource(record);
    setFormOpen(true);
  };

  const handleCloseForm = () => {
    setFormOpen(false);
    setEditingItem(null);
    setCloneSource(null);
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success('삭제되었습니다');
    } catch {
      message.error('삭제에 실패했습니다');
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const blob = await downloadPPTMasterTemplate();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `전문행사조마스터_템플릿_${dayjs().format('YYYYMMDD')}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('템플릿 다운로드에 실패했습니다');
    }
  };

  const handleConfirmSelected = async () => {
    if (selectedIds.length === 0) {
      message.warning('확정할 레코드를 선택해주세요');
      return;
    }
    try {
      const result = await confirmByIdsMutation.mutateAsync(selectedIds);
      message.success(
        `일괄 확정 완료 (확정: ${result.confirmedCount}건${
          result.skippedCount > 0 ? ` / 건너뜀: ${result.skippedCount}건` : ''
        })`,
      );
      setSelectedIds([]);
    } catch {
      message.error('일괄 확정에 실패했습니다');
    }
  };

  const handleExport = async () => {
    try {
      const blob = await exportPPTMasters({
        employeeName: searchParams.employeeName,
        employeeCode: searchParams.employeeCode,
        teamType: searchParams.teamType,
        branchCode: searchParams.branchCode,
        validOnly: searchParams.validOnly,
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `전문행사조마스터_${dayjs().format('YYYYMMDD')}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('엑셀 다운로드에 실패했습니다');
    }
  };

  const columns: ColumnsType<PPTMaster> = [
    {
      title: '#',
      width: 50,
      align: 'center',
      render: (_, __, index) => (searchParams.page ?? 0) * (searchParams.size ?? 20) + index + 1,
    },
    { title: '사번', dataIndex: 'employeeCode', width: 100, align: 'center' },
    { title: '사원명', dataIndex: 'employeeName', width: 100, align: 'center' },
    { title: '거래처코드', dataIndex: 'accountCode', width: 120, align: 'center' },
    {
      title: '거래처명',
      dataIndex: 'accountName',
      width: 150,
      ellipsis: true,
    },
    {
      title: '전문행사조',
      dataIndex: 'teamType',
      width: 140,
      align: 'center',
      render: (val: string) => (
        <Tag color={getPPTTeamTypeColor(val)}>{val}</Tag>
      ),
    },
    { title: '시작일', dataIndex: 'startDate', width: 120, align: 'center' },
    {
      title: '종료일',
      dataIndex: 'endDate',
      width: 120,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '확정',
      dataIndex: 'isConfirmed',
      width: 70,
      align: 'center',
      render: (val: boolean) => (val ? '✅' : '-'),
    },
    { title: '지점', dataIndex: 'branchName', width: 100, align: 'center' },
    {
      title: '액션',
      width: 170,
      align: 'center',
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            수정
          </Button>
          <Button type="link" size="small" onClick={() => handleClone(record)}>
            복제
          </Button>
          <Popconfirm
            title="이 마스터를 삭제하시겠습니까?"
            onConfirm={() => handleDelete(record.id)}
            okText="확인"
            cancelText="취소"
          >
            <Button type="link" size="small" danger>
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="사원명"
            value={filterEmployeeName}
            onChange={(e) => setFilterEmployeeName(e.target.value)}
            style={{ width: 120 }}
            onPressEnter={handleSearch}
          />
          <Input
            placeholder="사번"
            value={filterEmployeeNumber}
            onChange={(e) => setFilterEmployeeNumber(e.target.value)}
            style={{ width: 120 }}
            onPressEnter={handleSearch}
          />
          <Select
            placeholder="전문행사조"
            value={filterTeamType}
            onChange={setFilterTeamType}
            style={{ width: 160 }}
            options={TEAM_TYPE_FILTER_OPTIONS}
          />
          <Checkbox checked={filterValidOnly} onChange={(e) => setFilterValidOnly(e.target.checked)}>
            유효만
          </Checkbox>
          <Button onClick={handleReset}>초기화</Button>
          <Button type="primary" onClick={handleSearch}>
            검색
          </Button>
        </Space>
      </Card>

      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 16 }}>
        <Popconfirm
          title={`선택한 ${selectedIds.length}건을 일괄 확정하시겠습니까?`}
          onConfirm={handleConfirmSelected}
          okText="확인"
          cancelText="취소"
          disabled={selectedIds.length === 0}
        >
          <Button
            icon={<CheckOutlined />}
            type="primary"
            ghost
            disabled={selectedIds.length === 0}
            loading={confirmByIdsMutation.isPending}
          >
            선택 일괄 확정 ({selectedIds.length})
          </Button>
        </Popconfirm>
        <Space>
          <Button icon={<PlusOutlined />} type="primary" onClick={handleAdd}>
            마스터 등록
          </Button>
          <Button icon={<DownloadOutlined />} onClick={handleExport}>
            엑셀 다운로드
          </Button>
          <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
            엑셀 템플릿 다운로드
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => setUploadOpen(true)}>
            엑셀 업로드
          </Button>
        </Space>
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        rowSelection={{
          selectedRowKeys: selectedIds,
          onChange: (keys) => setSelectedIds(keys as number[]),
          getCheckboxProps: (record) => ({ disabled: record.isConfirmed }),
        }}
        pagination={{
          current: (data?.number ?? 0) + 1,
          pageSize: data?.size ?? 20,
          total: data?.totalElements ?? 0,
          showSizeChanger: true,
          onChange: (page, pageSize) =>
            setSearchParams((prev) => ({ ...prev, page: page - 1, size: pageSize })),
        }}
        scroll={{ x: 1300 }}
        size="middle"
      />

      <PPTMasterFormModal
        open={formOpen}
        editingItem={editingItem}
        cloneSource={cloneSource}
        onClose={handleCloseForm}
      />

      <PPTMasterUploadModal open={uploadOpen} onClose={() => setUploadOpen(false)} />
    </div>
  );
}
