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
  Input,
  Space,
  Spin,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { AxiosError } from 'axios';
import { usePermissionSet } from '@/hooks/admin/useAdminPermission';
import {
  useAvailablePermissionResources,
  useUpdatePermissionSetFlags,
  useUpdatePermissionSetMeta,
} from '@/hooks/admin/usePermissionSetMutation';
import PermissionMatrixEditor, { type PermissionBit } from './components/PermissionMatrixEditor';

const { Title, Text } = Typography;

const OBJECT_BITS: PermissionBit[] = ['allowRead', 'allowCreate', 'allowEdit', 'allowDelete', 'viewAllRecords', 'modifyAllRecords'];
const CUSTOM_BITS: PermissionBit[] = ['allowRead', 'allowCreate', 'allowEdit', 'allowDelete'];

interface MetaFormValues {
  label?: string;
  description?: string;
  viewAllData: boolean;
  modifyAllData: boolean;
}

/**
 * Inspection `ObjectPermissionRow[]` → Mutation `Record<string, Record<string, boolean>>` 어댑터.
 * objectPermissions 의 키는 SF API name 그대로 (Inspection 행의 sfApiName).
 */
function rowsToObjectMap(
  rows: { sfApiName: string; canRead: boolean; canCreate: boolean; canEdit: boolean; canDelete: boolean }[],
): Record<string, Record<string, boolean>> {
  const map: Record<string, Record<string, boolean>> = {};
  for (const row of rows) {
    const bits: Record<string, boolean> = {};
    if (row.canRead) bits.allowRead = true;
    if (row.canCreate) bits.allowCreate = true;
    if (row.canEdit) bits.allowEdit = true;
    if (row.canDelete) bits.allowDelete = true;
    if (Object.keys(bits).length > 0) {
      map[row.sfApiName] = bits;
    }
  }
  return map;
}

/**
 * Inspection `CustomPermissionRow[]` → Mutation Map 어댑터.
 */
function rowsToCustomMap(
  rows: { resource: string; canRead: boolean; canCreate: boolean; canEdit: boolean; canDelete: boolean }[],
): Record<string, Record<string, boolean>> {
  const map: Record<string, Record<string, boolean>> = {};
  for (const row of rows) {
    const bits: Record<string, boolean> = {};
    if (row.canRead) bits.allowRead = true;
    if (row.canCreate) bits.allowCreate = true;
    if (row.canEdit) bits.allowEdit = true;
    if (row.canDelete) bits.allowDelete = true;
    if (Object.keys(bits).length > 0) {
      map[row.resource] = bits;
    }
  }
  return map;
}

/**
 * Spec #837 — PermissionSet 편집 페이지 (메타 / Object / Custom 3 탭).
 *
 * 저장 시 메타 (label/description/system 비트) 와 권한 비트 (object/custom) 가 변경되었으면 각각
 * 별도 PUT 호출. 메타 PUT 성공 + flags PUT 실패 시 메타는 commit 된 상태로 안내.
 */
export default function PermissionSetEditPage() {
  const { permissionSetId: rawId } = useParams<{ permissionSetId: string }>();
  const permissionSetId = rawId ? Number(rawId) : undefined;
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [form] = Form.useForm<MetaFormValues>();

  const { data, isLoading, isError, error } = usePermissionSet(permissionSetId, { userPage: 0, userSize: 1 });
  const resources = useAvailablePermissionResources();
  const updateMeta = useUpdatePermissionSetMeta(permissionSetId ?? 0);
  const updateFlags = useUpdatePermissionSetFlags(permissionSetId ?? 0);

  const [objectValue, setObjectValue] = useState<Record<string, Record<string, boolean>>>({});
  const [objectBaseline, setObjectBaseline] = useState<Record<string, Record<string, boolean>>>({});
  const [customValue, setCustomValue] = useState<Record<string, Record<string, boolean>>>({});
  const [customBaseline, setCustomBaseline] = useState<Record<string, Record<string, boolean>>>({});
  const [systemBaseline, setSystemBaseline] = useState<{ viewAllData: boolean; modifyAllData: boolean }>({
    viewAllData: false,
    modifyAllData: false,
  });

  useEffect(() => {
    if (!data) return;
    form.setFieldsValue({
      label: data.label ?? undefined,
      description: data.description ?? undefined,
      viewAllData: data.flags?.viewAllData ?? false,
      modifyAllData: data.flags?.modifyAllData ?? false,
    });
    const objMap = rowsToObjectMap(data.objectPermissions);
    const customMap = rowsToCustomMap(data.customPermissions);
    setObjectValue(objMap);
    setObjectBaseline(objMap);
    setCustomValue(customMap);
    setCustomBaseline(customMap);
    setSystemBaseline({
      viewAllData: data.flags?.viewAllData ?? false,
      modifyAllData: data.flags?.modifyAllData ?? false,
    });
  }, [data, form]);

  const objectResources = useMemo(
    () =>
      (resources.data?.sfObjects ?? []).map((o) => ({
        name: o.sfApiName,
        label: o.entity,
      })),
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
          message="PermissionSet 상세 조회 실패"
          description={(error as Error)?.message ?? 'PermissionSet 을 찾을 수 없습니다'}
          action={<Button onClick={() => navigate('/admin/permissions/permission-sets')}>목록으로</Button>}
        />
      </div>
    );
  }

  const handleSave = async () => {
    let metaValues: MetaFormValues;
    try {
      metaValues = await form.validateFields();
    } catch {
      message.warning('메타 탭의 입력 형식을 확인하세요');
      return;
    }

    const metaChanged =
      (metaValues.label ?? null) !== (data.label ?? null) ||
      (metaValues.description ?? null) !== (data.description ?? null);
    const systemChanged =
      metaValues.viewAllData !== systemBaseline.viewAllData ||
      metaValues.modifyAllData !== systemBaseline.modifyAllData;
    const flagsChanged =
      systemChanged ||
      JSON.stringify(objectValue) !== JSON.stringify(objectBaseline) ||
      JSON.stringify(customValue) !== JSON.stringify(customBaseline);

    if (!metaChanged && !flagsChanged) {
      message.info('변경 사항이 없습니다');
      return;
    }

    let metaCommitted = false;
    try {
      if (metaChanged) {
        await updateMeta.mutateAsync({
          label: metaValues.label ?? null,
          description: metaValues.description ?? null,
        });
        metaCommitted = true;
      }
      if (flagsChanged && permissionSetId) {
        await updateFlags.mutateAsync({
          viewAllData: metaValues.viewAllData,
          modifyAllData: metaValues.modifyAllData,
          objectPermissions: objectValue,
          customPermissions: customValue,
        });
      }
      message.success('PermissionSet 이 저장되었습니다');
      navigate(`/admin/permissions/permission-sets/${permissionSetId}`);
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string; message?: string } }>;
      const code = err.response?.data?.error?.code;
      const msg = err.response?.data?.error?.message;
      if (metaCommitted) {
        message.error(
          `메타는 저장되었으나 권한 비트 저장에 실패했습니다 — ${msg ?? code ?? '재시도 필요'}`,
        );
      } else {
        message.error(msg || '저장에 실패했습니다');
      }
    }
  };

  const saving = updateMeta.isPending || updateFlags.isPending;

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate(`/admin/permissions/permission-sets/${permissionSetId}`)}>
          ← 상세로
        </Button>
      </Space>

      <Title level={4}>
        {data.label ?? data.name} 편집
        {data.isLocallyModified && (
          <Tag color="orange" style={{ marginLeft: 12 }}>
            ⚠️ 신규 시스템에서 수정됨
          </Tag>
        )}
        {data.sfOrigin && (
          <Tag color="blue" style={{ marginLeft: 4 }}>
            SF 출처
          </Tag>
        )}
      </Title>

      <Card>
        <Form<MetaFormValues> form={form} layout="vertical">
          <Tabs
            items={[
              {
                key: 'meta',
                label: '메타',
                children: (
                  <>
                    <Descriptions size="small" bordered column={2} style={{ marginBottom: 16 }}>
                      <Descriptions.Item label="Name">
                        <Text code>{data.name}</Text>
                        <Text type="secondary"> (수정 불가)</Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="PS ID">{data.permissionSetId}</Descriptions.Item>
                    </Descriptions>
                    <Form.Item label="Label" name="label" rules={[{ max: 255 }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item label="Description" name="description" rules={[{ max: 1024 }]}>
                      <Input.TextArea rows={3} />
                    </Form.Item>
                    <Form.Item label="VIEW_ALL_DATA" name="viewAllData" valuePropName="checked">
                      <Checkbox>모든 데이터 조회 가능 (View All Data)</Checkbox>
                    </Form.Item>
                    <Form.Item label="MODIFY_ALL_DATA" name="modifyAllData" valuePropName="checked">
                      <Checkbox>모든 데이터 수정 가능 (Modify All Data)</Checkbox>
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
            <Button type="primary" loading={saving} onClick={handleSave}>
              저장
            </Button>
            <Button
              onClick={() => navigate(`/admin/permissions/permission-sets/${permissionSetId}`)}
              disabled={saving}
            >
              취소
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
}
