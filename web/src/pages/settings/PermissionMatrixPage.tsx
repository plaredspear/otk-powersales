import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Checkbox, notification, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import {
  fetchPermissionMatrix,
  updateRolePermissions,
  type PermissionDetail,
  type PermissionMatrixData,
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

  const canManage = data?.currentUser.canManagePermissions ?? false;

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

  const matrixColumns: ColumnsType<PermissionDetail> = [
    { title: '설명', dataIndex: 'description', width: 200, fixed: 'left' },
    {
      title: '대상 메뉴',
      dataIndex: 'menus',
      width: 220,
      render: (menus: string[]) => menus.join(', '),
    },
    ...data.roles.map((roleRow) => ({
      title:
        roleRow.role === data.currentUser.role ? (
          <>
            {roleRow.roleLabel} <Tag color="blue">내 역할</Tag>
          </>
        ) : (
          roleRow.roleLabel
        ),
      key: roleRow.role,
      width: 120,
      align: 'center' as const,
      className:
        roleRow.role === data.currentUser.role ? 'permission-matrix-highlight-col' : undefined,
      onHeaderCell: () => ({
        className:
          roleRow.role === data.currentUser.role ? 'permission-matrix-highlight-col' : '',
      }),
      render: (_: unknown, permRow: PermissionDetail) => {
        const perms = getCurrentPermissions(roleRow.role);
        const hasPerm = perms.includes(permRow.code);
        if (canManage) {
          return (
            <Checkbox
              checked={hasPerm}
              onChange={(e) => handleToggle(roleRow.role, permRow.code, e.target.checked)}
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
        message={`내 권한: ${data.currentUser.roleLabel} (${data.currentUser.permissions.length}개 권한 보유)`}
        style={{ marginBottom: 24 }}
      />

      <Title level={5}>권한 · 역할 매트릭스</Title>
      <Table
        rowKey="code"
        columns={matrixColumns}
        dataSource={data.permissions}
        pagination={false}
        size="small"
        scroll={{ x: 900 }}
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
        .permission-matrix-highlight-col {
          background-color: #e6f7ff !important;
        }
      `}</style>
    </div>
  );
}
