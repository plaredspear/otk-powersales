import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Checkbox, notification, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import {
  fetchPermissionMatrix,
  updateRolePermissions,
  type PermissionMatrixData,
  type RolePermissions,
} from '@/api/permission';

const { Title, Text } = Typography;

export default function PermissionMatrixPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<PermissionMatrixData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editedRoles, setEditedRoles] = useState<Record<string, string[]>>({});

  const loadData = useCallback(() => {
    setLoading(true);
    fetchPermissionMatrix()
      .then((result) => {
        setData(result);
        setEditedRoles({});
      })
      .catch((err) => {
        notification.error({
          message: '권한 매트릭스 조회 실패',
          description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
        });
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const canManage = data?.current_user.can_manage_permissions ?? false;

  const changedRoles = useMemo(() => {
    if (!data) return [];
    return Object.keys(editedRoles).filter((role) => {
      const original = data.roles.find((r) => r.role === role);
      if (!original) return false;
      const edited = editedRoles[role];
      return (
        edited.length !== original.permissions.length ||
        !edited.every((p) => original.permissions.includes(p))
      );
    });
  }, [data, editedRoles]);

  const hasChanges = changedRoles.length > 0;

  const getCurrentPermissions = (role: string): string[] => {
    if (editedRoles[role] !== undefined) return editedRoles[role];
    const original = data?.roles.find((r) => r.role === role);
    return original?.permissions ?? [];
  };

  const handleToggle = (role: string, permCode: string, checked: boolean) => {
    const current = getCurrentPermissions(role);
    const updated = checked
      ? [...current, permCode]
      : current.filter((p) => p !== permCode);
    setEditedRoles((prev) => ({ ...prev, [role]: updated }));
  };

  const handleSave = async () => {
    if (!data || changedRoles.length === 0) return;
    setSaving(true);
    try {
      for (const role of changedRoles) {
        await updateRolePermissions(role, { permissions: editedRoles[role] });
      }
      notification.success({ message: '권한 매트릭스가 저장되었습니다' });
      loadData();
    } catch (err) {
      notification.error({
        message: '권한 매트릭스 저장 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!data) return null;

  const permissionColumns: ColumnsType<{ code: string; description: string; menus: string[] }> = [
    { title: '권한 코드', dataIndex: 'code', width: 180 },
    { title: '설명', dataIndex: 'description', width: 180 },
    {
      title: '대상 메뉴',
      dataIndex: 'menus',
      render: (menus: string[]) => menus.join(', '),
    },
  ];

  const matrixColumns: ColumnsType<RolePermissions> = [
    {
      title: '역할',
      dataIndex: 'role',
      width: 120,
      fixed: 'left',
      render: (role: string) =>
        role === data.current_user.role ? <>{role} <Tag color="blue">내 역할</Tag></> : role,
    },
    ...data.permissions.map((perm) => ({
      title: perm.description,
      key: perm.code,
      width: 120,
      align: 'center' as const,
      render: (_: unknown, record: RolePermissions) => {
        const perms = getCurrentPermissions(record.role);
        const hasPerm = perms.includes(perm.code);
        if (canManage) {
          return (
            <Checkbox
              checked={hasPerm}
              onChange={(e) => handleToggle(record.role, perm.code, e.target.checked)}
            />
          );
        }
        return hasPerm ? <Tag color="blue">O</Tag> : <Text type="secondary">-</Text>;
      },
    })),
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>권한 관리</Title>
        {canManage && (
          <Button type="default" onClick={() => navigate('/settings/permissions/employees')}>
            사원별 권한 관리
          </Button>
        )}
      </div>

      <Alert
        type="info"
        showIcon
        message={`내 권한: ${data.current_user.role} (${data.current_user.permissions.length}개 권한 보유)`}
        style={{ marginBottom: 24 }}
      />

      <Title level={5}>권한별 대상 메뉴</Title>
      <Table
        rowKey="code"
        columns={permissionColumns}
        dataSource={data.permissions}
        pagination={false}
        size="small"
        style={{ marginBottom: 32 }}
      />

      <Title level={5}>역할별 권한 매트릭스</Title>
      <Table
        rowKey="role"
        columns={matrixColumns}
        dataSource={data.roles}
        pagination={false}
        size="small"
        scroll={{ x: 900 }}
        rowClassName={(record) =>
          record.role === data.current_user.role ? 'permission-matrix-highlight' : ''
        }
      />

      {canManage && (
        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Button
            type="primary"
            disabled={!hasChanges}
            loading={saving}
            onClick={handleSave}
          >
            변경사항 저장
          </Button>
        </div>
      )}

      <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
        {canManage ? '체크박스를 토글하여 역할별 권한을 수정한 후 [변경사항 저장]을 클릭하세요' : 'O = 권한 보유 / - = 권한 없음'}
      </Text>

      <style>{`
        .permission-matrix-highlight td {
          background-color: #e6f7ff !important;
        }
      `}</style>
    </div>
  );
}
