import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  App,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Form,
  Space,
  Spin,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { AxiosError } from 'axios';
import { useProfile } from '@/hooks/admin/useAdminPermission';
import {
  useAvailablePermissionResources,
  useUpdateProfileFlags,
} from '@/hooks/admin/usePermissionSetMutation';
import PermissionMatrixEditor, { type PermissionBit } from './components/PermissionMatrixEditor';

const { Title, Text } = Typography;

const OBJECT_BITS: PermissionBit[] = ['allowRead', 'allowCreate', 'allowEdit', 'allowDelete'];
const CUSTOM_BITS: PermissionBit[] = ['allowRead', 'allowCreate', 'allowEdit', 'allowDelete'];

interface SystemFormValues {
  viewAllData: boolean;
  modifyAllData: boolean;
  viewAllUsers: boolean;
  manageUsers: boolean;
  apiEnabled: boolean;
}

/**
 * Inspection 행 (ObjectPermissionRow / CustomPermissionRow) → Mutation Map 어댑터.
 * 키는 Object 영역은 sfApiName, Custom 영역은 resource. PermissionSetEditPage 와 동일.
 */
function rowsToMap(
  rows: { key: string; canRead: boolean; canCreate: boolean; canEdit: boolean; canDelete: boolean }[],
): Record<string, Record<string, boolean>> {
  const map: Record<string, Record<string, boolean>> = {};
  for (const row of rows) {
    const bits: Record<string, boolean> = {};
    if (row.canRead) bits.allowRead = true;
    if (row.canCreate) bits.allowCreate = true;
    if (row.canEdit) bits.allowEdit = true;
    if (row.canDelete) bits.allowDelete = true;
    if (Object.keys(bits).length > 0) map[row.key] = bits;
  }
  return map;
}

/**
 * Profile 권한 편집 페이지 (시스템 / Object / Custom 3 탭).
 *
 * SF 정합 — 직책별 Profile 에 객체권한 (예: monthly_sales_history Read) 을 박으면 발령으로 해당 직책이
 * 된 사원에게 화면 권한이 자동 전파된다. 저장 시 backend 가 is_locally_modified set + 권한 캐시 무효화.
 * PermissionSet 과 달리 Profile 은 system 비트 5종 전부를 다룬다 (메타 label/description 은 SF 출처라 편집 불가).
 */
export default function ProfileEditPage() {
  const { profileId: rawId } = useParams<{ profileId: string }>();
  const profileId = rawId ? Number(rawId) : undefined;
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [form] = Form.useForm<SystemFormValues>();

  const { data, isLoading, isError, error } = useProfile(profileId, { userPage: 0, userSize: 1 });
  const resources = useAvailablePermissionResources();
  const updateFlags = useUpdateProfileFlags(profileId ?? 0);

  const [objectValue, setObjectValue] = useState<Record<string, Record<string, boolean>>>({});
  const [objectBaseline, setObjectBaseline] = useState<Record<string, Record<string, boolean>>>({});
  const [customValue, setCustomValue] = useState<Record<string, Record<string, boolean>>>({});
  const [customBaseline, setCustomBaseline] = useState<Record<string, Record<string, boolean>>>({});
  const [systemBaseline, setSystemBaseline] = useState<SystemFormValues>({
    viewAllData: false,
    modifyAllData: false,
    viewAllUsers: false,
    manageUsers: false,
    apiEnabled: false,
  });

  useEffect(() => {
    if (!data) return;
    const sys: SystemFormValues = {
      viewAllData: data.flags.viewAllData,
      modifyAllData: data.flags.modifyAllData,
      viewAllUsers: data.flags.viewAllUsers,
      manageUsers: data.flags.manageUsers,
      apiEnabled: data.flags.apiEnabled,
    };
    form.setFieldsValue(sys);
    setSystemBaseline(sys);
    const objMap = rowsToMap(data.objectPermissions.map((r) => ({ key: r.sfApiName, ...r })));
    const customMap = rowsToMap(data.customPermissions.map((r) => ({ key: r.resource, ...r })));
    setObjectValue(objMap);
    setObjectBaseline(objMap);
    setCustomValue(customMap);
    setCustomBaseline(customMap);
  }, [data, form]);

  const objectResources = useMemo(
    () => (resources.data?.sfObjects ?? []).map((o) => ({ name: o.sfApiName, label: o.entity })),
    [resources.data],
  );
  const customResourceItems = useMemo(
    () => (resources.data?.customResources ?? []).map((r) => ({ name: r })),
    [resources.data],
  );

  if (isLoading || resources.isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="Profile 상세 조회 실패"
          description={(error as Error)?.message ?? 'Profile 을 찾을 수 없습니다'}
          action={<Button onClick={() => navigate('/admin/permissions/profiles')}>목록으로</Button>}
        />
      </div>
    );
  }

  const handleSave = async () => {
    const sys = await form.validateFields();
    const systemChanged =
      sys.viewAllData !== systemBaseline.viewAllData ||
      sys.modifyAllData !== systemBaseline.modifyAllData ||
      sys.viewAllUsers !== systemBaseline.viewAllUsers ||
      sys.manageUsers !== systemBaseline.manageUsers ||
      sys.apiEnabled !== systemBaseline.apiEnabled;
    const changed =
      systemChanged ||
      JSON.stringify(objectValue) !== JSON.stringify(objectBaseline) ||
      JSON.stringify(customValue) !== JSON.stringify(customBaseline);

    if (!changed) {
      message.info('변경 사항이 없습니다');
      return;
    }

    try {
      await updateFlags.mutateAsync({
        viewAllData: sys.viewAllData,
        modifyAllData: sys.modifyAllData,
        viewAllUsers: sys.viewAllUsers,
        manageUsers: sys.manageUsers,
        apiEnabled: sys.apiEnabled,
        objectPermissions: objectValue,
        customPermissions: customValue,
      });
      message.success('Profile 권한이 저장되었습니다');
      navigate(`/admin/permissions/profiles/${profileId}`);
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string; message?: string } }>;
      message.error(err.response?.data?.error?.message || '저장에 실패했습니다');
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate(`/admin/permissions/profiles/${profileId}`)}>← 상세로</Button>
      </Space>

      <Title level={4}>
        {data.name} 권한 편집
        {data.isLocallyModified && (
          <Tag color="orange" style={{ marginLeft: 12 }}>
            ⚠️ 신규 시스템에서 수정됨
          </Tag>
        )}
      </Title>

      <Alert
        type="info"
        message="직책 자동 권한"
        description="Profile 에 객체권한을 부여하면, 발령으로 해당 직책이 된 사원에게 화면 권한이 자동으로 따라옵니다 (예: 6.조장 에 monthly_sales_history Read → 조장 전원 월매출 화면 자동 접근)."
        style={{ marginBottom: 12 }}
      />

      <Card>
        <Form<SystemFormValues> form={form} layout="vertical">
          <Tabs
            items={[
              {
                key: 'system',
                label: '시스템 권한',
                children: (
                  <>
                    <Descriptions size="small" bordered column={2} style={{ marginBottom: 16 }}>
                      <Descriptions.Item label="Name">
                        <Text code>{data.name}</Text>
                        <Text type="secondary"> (수정 불가)</Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="Profile ID">{data.profileId}</Descriptions.Item>
                    </Descriptions>
                    <Form.Item name="viewAllData" valuePropName="checked">
                      <Checkbox>VIEW_ALL_DATA (모든 데이터 조회)</Checkbox>
                    </Form.Item>
                    <Form.Item name="modifyAllData" valuePropName="checked">
                      <Checkbox>MODIFY_ALL_DATA (모든 데이터 수정)</Checkbox>
                    </Form.Item>
                    <Form.Item name="viewAllUsers" valuePropName="checked">
                      <Checkbox>VIEW_ALL_USERS (모든 사용자 조회)</Checkbox>
                    </Form.Item>
                    <Form.Item name="manageUsers" valuePropName="checked">
                      <Checkbox>MANAGE_USERS (사용자 관리)</Checkbox>
                    </Form.Item>
                    <Form.Item name="apiEnabled" valuePropName="checked">
                      <Checkbox>API_ENABLED (API 사용)</Checkbox>
                    </Form.Item>
                  </>
                ),
              },
              {
                key: 'object',
                label: `Object Permissions (${objectResources.length})`,
                children: (
                  <PermissionMatrixEditor
                    resources={objectResources}
                    bits={OBJECT_BITS}
                    value={objectValue}
                    onChange={setObjectValue}
                    baselineValue={objectBaseline}
                  />
                ),
              },
              {
                key: 'custom',
                label: `Custom Permissions (${customResourceItems.length})`,
                children: (
                  <PermissionMatrixEditor
                    resources={customResourceItems}
                    bits={CUSTOM_BITS}
                    value={customValue}
                    onChange={setCustomValue}
                    baselineValue={customBaseline}
                  />
                ),
              },
            ]}
          />
          <Space style={{ marginTop: 16 }}>
            <Button type="primary" loading={updateFlags.isPending} onClick={handleSave}>
              저장
            </Button>
            <Button
              onClick={() => navigate(`/admin/permissions/profiles/${profileId}`)}
              disabled={updateFlags.isPending}
            >
              취소
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
}
