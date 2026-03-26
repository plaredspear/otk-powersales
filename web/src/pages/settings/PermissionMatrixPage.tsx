import { useEffect, useState } from 'react';
import { Alert, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { fetchPermissionMatrix, type PermissionMatrixData } from '@/api/permission';
import { notification } from 'antd';

const { Title, Text } = Typography;

export default function PermissionMatrixPage() {
  const [data, setData] = useState<PermissionMatrixData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPermissionMatrix()
      .then(setData)
      .catch((err) => {
        notification.error({
          message: '권한 매트릭스 조회 실패',
          description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
        });
      })
      .finally(() => setLoading(false));
  }, []);

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

  const matrixColumns: ColumnsType<{ role: string; permissions: string[] }> = [
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
      render: (_: unknown, record: { role: string; permissions: string[] }) =>
        record.permissions.includes(perm.code) ? (
          <Tag color="blue">O</Tag>
        ) : (
          <Text type="secondary">-</Text>
        ),
    })),
  ];

  return (
    <div style={{ padding: 16 }}>
      <Title level={4} style={{ marginBottom: 16 }}>
        권한 관리
      </Title>

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
      <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
        O = 권한 보유 / - = 권한 없음
      </Text>

      <style>{`
        .permission-matrix-highlight td {
          background-color: #e6f7ff !important;
        }
      `}</style>
    </div>
  );
}
