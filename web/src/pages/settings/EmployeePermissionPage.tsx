import { useCallback, useState } from 'react';
import {
  Button,
  Checkbox,
  Drawer,
  Input,
  Modal,
  notification,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useEmployees } from '@/hooks/employee/useEmployees';
import { useAuthStore } from '@/stores/authStore';
import type { Employee } from '@/api/employee';
import {
  fetchEmployeePermissions,
  updateUserPermissions,
  updateEmployeeAuthority,
  type EmployeePermissionDetail,
} from '@/api/employeePermission';
import {
  ROLE_OPTIONS_FOR_ADMIN_LOGIN,
  roleLabel,
  type UserRole,
} from '@/constants/userRole';

const { Title } = Typography;

const ALL_PERMISSIONS = [
  'DASHBOARD_READ',
  'EMPLOYEE_READ',
  'ACCOUNT_READ',
  'PROMOTION_READ',
  'PROMOTION_WRITE',
  'SAFETY_CHECK_READ',
  'SCHEDULE_READ',
  'SCHEDULE_WRITE',
  'PRODUCT_EXPIRATION_READ',
  'PRODUCT_EXPIRATION_WRITE',
];

export default function EmployeePermissionPage() {
  const navigate = useNavigate();
  const currentUserRole = useAuthStore((state) => state.user?.role ?? null);
  const canRegisterAdmin = currentUserRole === 'SYSTEM_ADMIN';
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | undefined>(undefined);
  const [searchParams, setSearchParams] = useState<{ keyword?: string; role?: UserRole; page: number }>({ page: 0 });
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);
  const [permDetail, setPermDetail] = useState<EmployeePermissionDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedRole, setSelectedRole] = useState<UserRole | ''>('');
  const [editedUserPerms, setEditedUserPerms] = useState<string[]>([]);
  const [savingPerms, setSavingPerms] = useState(false);
  const [savingAuthority, setSavingAuthority] = useState(false);

  const { data: employeeData, isLoading } = useEmployees({
    keyword: searchParams.keyword,
    role: searchParams.role,
    page: searchParams.page,
    size: 20,
  });

  const handleSearch = () => {
    setSearchParams({
      keyword: keyword || undefined,
      role: roleFilter,
      page: 0,
    });
  };

  const loadPermissionDetail = useCallback(async (employeeId: number) => {
    setDetailLoading(true);
    try {
      const detail = await fetchEmployeePermissions(employeeId);
      setPermDetail(detail);
      setSelectedRole(detail.role ?? '');
      setEditedUserPerms(detail.user_permissions.map((up) => up.permission));
    } catch (err) {
      notification.error({
        message: '권한 조회 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
      });
    } finally {
      setDetailLoading(false);
    }
  }, []);

  const handleOpenDrawer = (employee: Employee) => {
    setSelectedEmployee(employee);
    setDrawerOpen(true);
    loadPermissionDetail(employee.id);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setSelectedEmployee(null);
    setPermDetail(null);
  };

  const handleAuthorityChange = () => {
    if (!selectedEmployee || !permDetail || !selectedRole) return;
    const previousLabel = roleLabel(permDetail.role);
    const newLabel = roleLabel(selectedRole);
    Modal.confirm({
      title: '역할 변경',
      content: `${selectedEmployee.name}의 역할을 '${previousLabel}'에서 '${newLabel}'으로 변경하시겠습니까?`,
      okText: '확인',
      cancelText: '취소',
      onOk: async () => {
        setSavingAuthority(true);
        try {
          await updateEmployeeAuthority(selectedEmployee.id, { role: selectedRole });
          notification.success({ message: '역할이 변경되었습니다' });
          await loadPermissionDetail(selectedEmployee.id);
        } catch (err) {
          notification.error({
            message: '역할 변경 실패',
            description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
          });
        } finally {
          setSavingAuthority(false);
        }
      },
    });
  };

  const handlePermToggle = (permCode: string, checked: boolean) => {
    setEditedUserPerms((prev) =>
      checked ? [...prev, permCode] : prev.filter((p) => p !== permCode),
    );
  };

  const hasPermChanges = (() => {
    if (!permDetail) return false;
    const original = permDetail.user_permissions.map((up) => up.permission).sort();
    const edited = [...editedUserPerms].sort();
    return original.length !== edited.length || !original.every((v, i) => v === edited[i]);
  })();

  const handleSavePerms = async () => {
    if (!selectedEmployee) return;
    setSavingPerms(true);
    try {
      await updateUserPermissions(selectedEmployee.id, { permissions: editedUserPerms });
      notification.success({ message: '권한이 저장되었습니다' });
      await loadPermissionDetail(selectedEmployee.id);
    } catch (err) {
      notification.error({
        message: '권한 저장 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
      });
    } finally {
      setSavingPerms(false);
    }
  };

  const handleResetPerms = () => {
    if (!selectedEmployee) return;
    Modal.confirm({
      title: '개별 권한 초기화',
      content: '모든 개별 할당 권한을 제거하시겠습니까?',
      okText: '확인',
      cancelText: '취소',
      onOk: async () => {
        setSavingPerms(true);
        try {
          await updateUserPermissions(selectedEmployee.id, { permissions: [] });
          notification.success({ message: '개별 권한이 초기화되었습니다' });
          await loadPermissionDetail(selectedEmployee.id);
        } catch (err) {
          notification.error({
            message: '권한 초기화 실패',
            description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
          });
        } finally {
          setSavingPerms(false);
        }
      },
    });
  };

  const employeeColumns: ColumnsType<Employee> = [
    { title: '사번', dataIndex: 'employeeCode', width: 100 },
    { title: '이름', dataIndex: 'name', width: 100 },
    {
      title: '소속',
      dataIndex: 'orgName',
      width: 120,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '역할',
      dataIndex: 'roleLabel',
      width: 100,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '작업',
      width: 100,
      render: (_: unknown, record: Employee) => (
        <Button size="small" onClick={() => handleOpenDrawer(record)}>
          권한 관리
        </Button>
      ),
    },
  ];

  const permTableColumns: ColumnsType<{ permission: string }> = [
    {
      title: '권한명',
      dataIndex: 'permission',
      width: 200,
    },
    {
      title: '역할 기본',
      width: 100,
      align: 'center',
      render: (_: unknown, record: { permission: string }) => {
        const isRolePerm = permDetail?.role_permissions.includes(record.permission);
        return isRolePerm ? <Tag color="blue">O</Tag> : <Tag>X</Tag>;
      },
    },
    {
      title: '개별 할당',
      width: 100,
      align: 'center',
      render: (_: unknown, record: { permission: string }) => {
        const isRolePerm = permDetail?.role_permissions.includes(record.permission);
        const isUserPerm = editedUserPerms.includes(record.permission);
        if (isRolePerm) {
          return <Checkbox checked={false} disabled />;
        }
        return (
          <Checkbox
            checked={isUserPerm}
            onChange={(e) => handlePermToggle(record.permission, e.target.checked)}
          />
        );
      },
    },
  ];

  const permTableData = ALL_PERMISSIONS.map((p) => ({ permission: p }));

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space align="center">
          <Button icon={<ArrowLeftOutlined />} type="text" onClick={() => navigate('/settings/permissions')}>
            권한 관리
          </Button>
          <Title level={4} style={{ margin: 0 }}>사원별 권한 관리</Title>
        </Space>
        {canRegisterAdmin && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/settings/admin-accounts/new')}
          >
            관리자 등록
          </Button>
        )}
      </div>

      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="사번/이름 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={handleSearch}
          style={{ width: 200 }}
        />
        <Select<UserRole>
          placeholder="역할"
          allowClear
          value={roleFilter}
          onChange={(val) => setRoleFilter(val)}
          style={{ width: 140 }}
          options={ROLE_OPTIONS_FOR_ADMIN_LOGIN}
        />
        <Button type="primary" onClick={handleSearch}>검색</Button>
      </Space>

      <Table
        rowKey="id"
        columns={employeeColumns}
        dataSource={employeeData?.content}
        loading={isLoading}
        size="small"
        pagination={{
          current: (searchParams.page ?? 0) + 1,
          pageSize: 20,
          total: employeeData?.totalElements,
          showSizeChanger: false,
          onChange: (page) => setSearchParams((prev) => ({ ...prev, page: page - 1 })),
        }}
      />

      <Drawer
        title={selectedEmployee ? `${selectedEmployee.name} (${selectedEmployee.employeeCode}) 권한 관리` : '권한 관리'}
        placement="right"
        width={520}
        open={drawerOpen}
        onClose={handleCloseDrawer}
        footer={
          permDetail && (
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
              <Button
                danger
                onClick={handleResetPerms}
                disabled={permDetail.user_permissions.length === 0}
                loading={savingPerms}
              >
                개별 권한 초기화
              </Button>
              <Button
                type="primary"
                onClick={handleSavePerms}
                disabled={!hasPermChanges}
                loading={savingPerms}
              >
                저장
              </Button>
            </div>
          )
        }
      >
        {detailLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
            <Spin size="large" />
          </div>
        ) : permDetail ? (
          <>
            <Title level={5}>역할</Title>
            <Space style={{ marginBottom: 24 }}>
              <Select<UserRole>
                value={selectedRole === '' ? undefined : selectedRole}
                onChange={(val) => setSelectedRole(val)}
                style={{ width: 160 }}
                options={ROLE_OPTIONS_FOR_ADMIN_LOGIN}
              />
              <Button
                onClick={handleAuthorityChange}
                disabled={!selectedRole || selectedRole === permDetail.role}
                loading={savingAuthority}
              >
                역할 변경
              </Button>
            </Space>

            <Title level={5}>권한 설정</Title>
            <Table
              rowKey="permission"
              columns={permTableColumns}
              dataSource={permTableData}
              pagination={false}
              size="small"
            />
          </>
        ) : null}
      </Drawer>
    </div>
  );
}
