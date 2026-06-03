import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, Card, Checkbox, Input, Popconfirm, Select, Space, Tag, Tooltip, message } from 'antd';
import { PlusOutlined, DownloadOutlined, UploadOutlined, CheckOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  usePPTMasters,
  useDeletePPTMaster,
  useConfirmPPTMastersByIds,
} from '@/hooks/promotion/usePPTMasters';
import { downloadPPTMasterTemplate, exportPPTMasters } from '@/api/pptMaster';
import type { PPTMaster } from '@/api/pptMaster';
import PPTMasterFormModal from './components/PPTMasterFormModal';
import PPTMasterUploadModal from './components/PPTMasterUploadModal';
import {
  PPT_TEAM_TYPE_OPTIONS,
  getPPTTeamTypeColor,
} from '@/constants/pptTeamType';
import ResizableTable from '@/components/common/ResizableTable';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';

const TEAM_TYPE_FILTER_OPTIONS = [{ value: '', label: '전체' }, ...PPT_TEAM_TYPE_OPTIONS];

const DEFAULT_SIZE = 20;

// SF Valid__c / ValidData__c 정합 — 확정·시작일·종료일로 유효 상태(신호등) 산출.
//   미확정(주황) / 유효(녹색) / 예정(노랑) / 종료(빨강)
type ValidStatus = { label: string; status: 'success' | 'warning' | 'error' | 'default' };

function getValidStatus(record: PPTMaster): ValidStatus {
  if (!record.isConfirmed) return { label: '미확정', status: 'default' };
  const today = dayjs().startOf('day');
  const start = dayjs(record.startDate).startOf('day');
  const end = record.endDate ? dayjs(record.endDate).startOf('day') : null;
  const notEnded = !end || !end.isBefore(today); // 종료일 없거나 오늘 이후
  if (start.isAfter(today) && notEnded) return { label: '예정', status: 'warning' };
  if (!start.isAfter(today) && notEnded) return { label: '유효', status: 'success' };
  return { label: '종료', status: 'error' };
}

// SF ValidConditionData__c 정합 — 사원 재직상태 산출.
//   퇴직/앱비활성 + 종료일 경과 → "퇴직", 미경과 → "퇴직예정", 휴직 → "휴직", 그 외 → "재직"
function getEmployeeStatusLabel(record: PPTMaster): string {
  const { employeeStatus, employeeAppLoginActive, employeeEndDate } = record;
  const isQuit = employeeStatus === '퇴직' || employeeAppLoginActive === false;
  if (isQuit && employeeEndDate) {
    const end = dayjs(employeeEndDate).startOf('day');
    const today = dayjs().startOf('day');
    return end.isBefore(today) ? '퇴직' : '퇴직예정';
  }
  if (employeeStatus === '휴직') return '휴직';
  return employeeStatus ?? '재직';
}

export default function PPTMasterPage() {
  // page/필터를 URL query string 에 보관 — 새로고침/링크 공유/복귀 시 직전 조건 복원.
  // validOnly(boolean) 와 size(number) 는 URL 보관을 위해 string 으로 직렬화하고 사용처에서 역변환한다.
  const { page, setPage, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      employeeName: '',
      employeeCode: '',
      teamType: '',
      validOnly: 'true',
      size: String(DEFAULT_SIZE),
    },
  });
  const validOnly = filters.validOnly !== 'false';
  const pageSize = Number.parseInt(filters.size, 10) || DEFAULT_SIZE;

  // 검색버튼 분리형: 입력 위젯은 로컬 편집 버퍼, URL filters 가 source of truth.
  // 마운트 시 URL 값으로 1회 초기화하여 새로고침/복귀 시 위젯 표시가 맞도록 한다.
  const [filterEmployeeName, setFilterEmployeeName] = useState(filters.employeeName);
  const [filterEmployeeNumber, setFilterEmployeeNumber] = useState(filters.employeeCode);
  const [filterTeamType, setFilterTeamType] = useState(filters.teamType);
  const [filterValidOnly, setFilterValidOnly] = useState(validOnly);

  const { data, isLoading } = usePPTMasters({
    page,
    size: pageSize,
    employeeName: filters.employeeName || undefined,
    employeeCode: filters.employeeCode || undefined,
    teamType: filters.teamType || undefined,
    validOnly,
  });
  const deleteMutation = useDeletePPTMaster();
  const confirmByIdsMutation = useConfirmPPTMastersByIds();

  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<PPTMaster | null>(null);
  const [cloneSource, setCloneSource] = useState<PPTMaster | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);

  const handleSearch = () => {
    setFilters({
      employeeName: filterEmployeeName,
      employeeCode: filterEmployeeNumber,
      teamType: filterTeamType,
      validOnly: String(filterValidOnly),
    });
  };

  const handleReset = () => {
    setFilterEmployeeName('');
    setFilterEmployeeNumber('');
    setFilterTeamType('');
    setFilterValidOnly(true);
    setFilters({
      employeeName: '',
      employeeCode: '',
      teamType: '',
      validOnly: 'true',
      size: String(DEFAULT_SIZE),
    });
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
        employeeName: filters.employeeName || undefined,
        employeeCode: filters.employeeCode || undefined,
        teamType: filters.teamType || undefined,
        validOnly,
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
      render: (_, __, index) => page * pageSize + index + 1,
    },
    {
      title: '전문행사조 마스터 번호',
      dataIndex: 'name',
      width: 160,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '유효',
      width: 70,
      align: 'center',
      // SF Valid__c 정합 — 신호등 점(●)만 표시. 상태명은 Tooltip 으로 제공.
      render: (_, record) => {
        const v = getValidStatus(record);
        return (
          <Tooltip title={v.label}>
            <Badge status={v.status} />
          </Tooltip>
        );
      },
    },
    { title: '지점명', dataIndex: 'branchName', width: 110, align: 'center', render: (v: string | null) => v ?? '-' },
    { title: '사번', dataIndex: 'employeeCode', width: 100, align: 'center' },
    {
      title: '사원명',
      dataIndex: 'employeeName',
      width: 100,
      align: 'center',
      render: (val: string, record) =>
        record.employeeId ? <Link to={`/employee/${record.employeeId}`}>{val}</Link> : val,
    },
    {
      title: '재직상태',
      width: 100,
      align: 'center',
      render: (_, record) => getEmployeeStatusLabel(record),
    },
    { title: '거래처코드', dataIndex: 'accountCode', width: 120, align: 'center' },
    {
      title: '거래처명',
      dataIndex: 'accountName',
      width: 150,
      ellipsis: true,
      render: (val: string | null, record) =>
        val && record.accountId ? <Link to={`/account/${record.accountId}`}>{val}</Link> : (val ?? '-'),
    },
    {
      title: '거래처유형',
      dataIndex: 'accountType',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
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
    {
      title: '최종 수정 일자',
      dataIndex: 'updatedAt',
      width: 150,
      align: 'center',
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '액션',
      width: 170,
      align: 'center',
      fixed: 'right',
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
          current: page + 1,
          pageSize,
          total: data?.totalElements ?? 0,
          showSizeChanger: true,
          onChange: (nextPage, nextPageSize) => {
            if (nextPageSize !== pageSize) {
              setFilters({ size: String(nextPageSize) });
            } else {
              setPage(nextPage - 1);
            }
          },
        }}
        scroll={{ x: 1870 }}
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
