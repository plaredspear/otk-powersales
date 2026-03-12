import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  DatePicker,
  Descriptions,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import { useDeletePromotion } from '@/hooks/promotion/usePromotionMutation';
import { usePromotionEmployees } from '@/hooks/promotion/usePromotionEmployees';
import {
  useCreatePromotionEmployee,
  useDeletePromotionEmployee,
  useBatchUpdatePromotionEmployees,
} from '@/hooks/promotion/usePromotionEmployeeMutation';
import type { PromotionEmployee } from '@/api/promotionEmployee';
import {
  BatchValidationError,
  type BatchUpdatePromotionEmployeeItem,
} from '@/api/promotionEmployee';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const { Title } = Typography;

const CATEGORY_TAG: Record<string, string> = {
  라면: 'red',
  냉장: 'blue',
  냉동: 'cyan',
  만두: 'orange',
};

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

const WORK_STATUS_OPTIONS = [
  { label: '근무', value: '근무' },
  { label: '연차', value: '연차' },
  { label: '대휴', value: '대휴' },
];

const WORK_TYPE3_OPTIONS = [
  { label: '고정', value: '고정' },
  { label: '격고', value: '격고' },
  { label: '순회', value: '순회' },
];

// Editable row data during edit mode
interface EditableRow {
  id: number;
  employeeSfid: string | null;
  scheduleDate: string | null;
  workStatus: string | null;
  workType1: string | null;
  workType3: string | null;
  workType4: string | null;
  professionalPromotionTeam: string | null;
  basePrice: number | null;
  dailyTargetCount: number | null;
  targetAmount: number | null;
  actualAmount: number | null;
  // Read-only display fields
  employeeName: string | null;
  scheduleId: number | null;
  promoCloseByTm: boolean;
}

function toEditableRow(pe: PromotionEmployee): EditableRow {
  return {
    id: pe.id,
    employeeSfid: pe.employeeSfid,
    scheduleDate: pe.scheduleDate,
    workStatus: pe.workStatus,
    workType1: pe.workType1,
    workType3: pe.workType3,
    workType4: pe.workType4,
    professionalPromotionTeam: pe.professionalPromotionTeam,
    basePrice: pe.basePrice,
    dailyTargetCount: pe.dailyTargetCount,
    targetAmount: null,
    actualAmount: null,
    employeeName: pe.employeeName,
    scheduleId: pe.scheduleId,
    promoCloseByTm: pe.promoCloseByTm,
  };
}

const REQUIRED_FIELDS: (keyof EditableRow)[] = [
  'employeeSfid',
  'scheduleDate',
  'workStatus',
  'workType1',
  'workType3',
];

export default function PromotionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const promotionId = Number(id);

  const { data: promotion, isLoading, error } = usePromotion(promotionId);
  const deleteMutation = useDeletePromotion();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  // --- 행사조원 ---
  const { data: employees, isLoading: employeesLoading } = usePromotionEmployees(promotionId);
  const createPeMutation = useCreatePromotionEmployee();
  const deletePeMutation = useDeletePromotionEmployee();
  const batchUpdateMutation = useBatchUpdatePromotionEmployees();

  // --- 편집 모드 상태 ---
  const [editMode, setEditMode] = useState(false);
  const [editRows, setEditRows] = useState<EditableRow[]>([]);
  const [errorRowIds, setErrorRowIds] = useState<Set<number>>(new Set());
  const [errorMessages, setErrorMessages] = useState<Map<number, string>>(new Map());

  useEffect(() => {
    setDynamicTitle(promotion?.promotionNumber ?? null);
    return () => setDynamicTitle(null);
  }, [promotion?.promotionNumber, setDynamicTitle]);

  const hasDates = promotion?.startDate && promotion?.endDate;

  const handleDelete = () => {
    Modal.confirm({
      title: '행사마스터 삭제',
      content: '이 행사마스터를 삭제하시겠습니까?',
      okText: '확인',
      cancelText: '취소',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteMutation.mutateAsync(promotionId);
          message.success('행사마스터가 삭제되었습니다');
          navigate('/promotions');
        } catch {
          message.error('행사마스터 삭제에 실패했습니다');
        }
      },
    });
  };

  // --- 즉시 추가 ---
  const handleAddEmployee = async () => {
    try {
      await createPeMutation.mutateAsync({ promotionId });
    } catch (err) {
      message.error(err instanceof Error ? err.message : '조원 추가에 실패했습니다');
    }
  };

  // --- 편집 모드 진입 ---
  const enterEditMode = useCallback(() => {
    if (!employees) return;
    setEditRows(employees.map(toEditableRow));
    setErrorRowIds(new Set());
    setErrorMessages(new Map());
    setEditMode(true);
  }, [employees]);

  // --- 편집 모드 취소 ---
  const cancelEditMode = () => {
    setEditMode(false);
    setEditRows([]);
    setErrorRowIds(new Set());
    setErrorMessages(new Map());
  };

  // --- 행 필드 변경 ---
  const updateField = useCallback(
    (rowId: number, field: keyof EditableRow, value: unknown) => {
      setEditRows((prev) =>
        prev.map((row) => (row.id === rowId ? { ...row, [field]: value } : row)),
      );
      // Clear error highlight for this row on change
      setErrorRowIds((prev) => {
        const next = new Set(prev);
        next.delete(rowId);
        return next;
      });
      setErrorMessages((prev) => {
        const next = new Map(prev);
        next.delete(rowId);
        return next;
      });
    },
    [],
  );

  // --- 편집 모드에서 삭제 ---
  const handleDeleteInEditMode = async (peId: number) => {
    try {
      await deletePeMutation.mutateAsync(peId);
      setEditRows((prev) => prev.filter((r) => r.id !== peId));
      message.success('조원이 삭제되었습니다');
    } catch {
      message.error('삭제에 실패했습니다');
    }
  };

  // --- Dirty check ---
  const getChangedItems = useCallback((): BatchUpdatePromotionEmployeeItem[] | null => {
    if (!employees) return null;
    const originalMap = new Map(employees.map((e) => [e.id, e]));
    const changed: BatchUpdatePromotionEmployeeItem[] = [];

    for (const row of editRows) {
      const orig = originalMap.get(row.id);
      if (!orig) continue;

      const isDirty =
        row.employeeSfid !== orig.employeeSfid ||
        row.scheduleDate !== orig.scheduleDate ||
        row.workStatus !== orig.workStatus ||
        row.workType1 !== orig.workType1 ||
        row.workType3 !== orig.workType3 ||
        row.workType4 !== orig.workType4 ||
        row.professionalPromotionTeam !== orig.professionalPromotionTeam ||
        row.basePrice !== orig.basePrice ||
        row.dailyTargetCount !== orig.dailyTargetCount;

      if (isDirty) {
        changed.push({
          id: row.id,
          employee_sfid: row.employeeSfid ?? '',
          schedule_date: row.scheduleDate ?? '',
          work_status: row.workStatus ?? '',
          work_type1: row.workType1 ?? '',
          work_type3: row.workType3 ?? '',
          work_type4: row.workType4,
          professional_promotion_team: row.professionalPromotionTeam,
          base_price: row.basePrice,
          daily_target_count: row.dailyTargetCount,
          target_amount: row.targetAmount,
          actual_amount: row.actualAmount,
        });
      }
    }

    return changed;
  }, [editRows, employees]);

  // --- 클라이언트 검증 ---
  const validateRequired = useCallback((): boolean => {
    const invalidIds = new Set<number>();
    const msgs = new Map<number, string>();

    for (const row of editRows) {
      for (const field of REQUIRED_FIELDS) {
        const val = row[field];
        if (val === null || val === undefined || val === '') {
          invalidIds.add(row.id);
          msgs.set(row.id, '필수 항목을 입력하세요');
          break;
        }
      }
    }

    if (invalidIds.size > 0) {
      setErrorRowIds(invalidIds);
      setErrorMessages(msgs);
      message.error('필수 항목을 입력하세요');
      return false;
    }
    return true;
  }, [editRows]);

  // --- 저장 ---
  const handleSave = async () => {
    // Client validation
    if (!validateRequired()) return;

    const changedItems = getChangedItems();
    if (!changedItems || changedItems.length === 0) {
      message.info('변경사항이 없습니다');
      return;
    }

    try {
      await batchUpdateMutation.mutateAsync({
        promotionId,
        data: { items: changedItems },
      });
      message.success('저장되었습니다');
      setEditMode(false);
      setEditRows([]);
      setErrorRowIds(new Set());
      setErrorMessages(new Map());
    } catch (err) {
      if (err instanceof BatchValidationError) {
        // Map errors to row IDs
        const errorIds = new Set<number>();
        const msgs = new Map<number, string>();

        for (const itemErr of err.errors) {
          // changedItems[itemErr.item_index] corresponds to the batch request item
          const batchItem = changedItems[itemErr.item_index];
          if (batchItem) {
            errorIds.add(batchItem.id);
            msgs.set(batchItem.id, itemErr.message);
          }
        }

        setErrorRowIds(errorIds);
        setErrorMessages(msgs);
        message.error(err.message);
      } else {
        message.error(err instanceof Error ? err.message : '일괄 수정에 실패했습니다');
      }
    }
  };

  // --- 확정 상태 표시 컬럼 (공통) ---
  const confirmColumn = useMemo(
    () => ({
      title: '확정',
      dataIndex: 'scheduleId',
      width: 70,
      render: (v: number | null) =>
        v != null ? <Tag color="blue">확정</Tag> : <Tag>미확정</Tag>,
    }),
    [],
  );

  const closeColumn = useMemo(
    () => ({
      title: '마감',
      dataIndex: 'promoCloseByTm',
      width: 60,
      render: (v: boolean) => (v ? <Tag color="red">마감</Tag> : '-'),
    }),
    [],
  );

  // --- 읽기 모드 컬럼 ---
  const readColumns: ColumnsType<PromotionEmployee> = useMemo(
    () => [
      {
        title: '사원명',
        dataIndex: 'employeeName',
        width: 100,
        render: (name: string | null, record: PromotionEmployee) => name ?? record.employeeSfid ?? '-',
      },
      {
        title: '투입일',
        dataIndex: 'scheduleDate',
        width: 120,
        render: (v: string | null) => v ?? '-',
      },
      {
        title: '근무상태',
        dataIndex: 'workStatus',
        width: 80,
        render: (v: string | null) => v ?? '-',
      },
      {
        title: '근무유형1',
        dataIndex: 'workType1',
        width: 100,
        render: (v: string | null) => v ?? '-',
      },
      {
        title: '근무유형3',
        dataIndex: 'workType3',
        width: 80,
        render: (v: string | null) => v ?? '-',
      },
      {
        title: '전문행사조',
        dataIndex: 'professionalPromotionTeam',
        width: 120,
        render: (v: string | null) => v ?? '-',
      },
      confirmColumn,
      closeColumn,
    ],
    [confirmColumn, closeColumn],
  );

  // --- 편집 모드 컬럼 ---
  const editColumns: ColumnsType<EditableRow> = useMemo(
    () => [
      {
        title: '여사원 SF ID',
        dataIndex: 'employeeSfid',
        width: 140,
        render: (_, record) => (
          <Input
            size="small"
            maxLength={18}
            value={record.employeeSfid ?? ''}
            onChange={(e) => updateField(record.id, 'employeeSfid', e.target.value || null)}
          />
        ),
      },
      {
        title: '투입일',
        dataIndex: 'scheduleDate',
        width: 140,
        render: (_, record) => (
          <DatePicker
            size="small"
            format="YYYY-MM-DD"
            value={record.scheduleDate ? dayjs(record.scheduleDate) : null}
            onChange={(d) =>
              updateField(record.id, 'scheduleDate', d ? d.format('YYYY-MM-DD') : null)
            }
            style={{ width: '100%' }}
          />
        ),
      },
      {
        title: '근무상태',
        dataIndex: 'workStatus',
        width: 90,
        render: (_, record) => (
          <Select
            size="small"
            options={WORK_STATUS_OPTIONS}
            value={record.workStatus}
            onChange={(v) => updateField(record.id, 'workStatus', v)}
            style={{ width: '100%' }}
            allowClear={false}
          />
        ),
      },
      {
        title: '근무유형1',
        dataIndex: 'workType1',
        width: 100,
        render: (_, record) => (
          <Input
            size="small"
            maxLength={100}
            value={record.workType1 ?? ''}
            onChange={(e) => updateField(record.id, 'workType1', e.target.value || null)}
          />
        ),
      },
      {
        title: '근무유형3',
        dataIndex: 'workType3',
        width: 90,
        render: (_, record) => (
          <Select
            size="small"
            options={WORK_TYPE3_OPTIONS}
            value={record.workType3}
            onChange={(v) => updateField(record.id, 'workType3', v)}
            style={{ width: '100%' }}
            allowClear={false}
          />
        ),
      },
      {
        title: '근무유형4',
        dataIndex: 'workType4',
        width: 100,
        render: (_, record) => (
          <Input
            size="small"
            maxLength={100}
            value={record.workType4 ?? ''}
            onChange={(e) => updateField(record.id, 'workType4', e.target.value || null)}
          />
        ),
      },
      {
        title: '전문행사조',
        dataIndex: 'professionalPromotionTeam',
        width: 120,
        render: (_, record) => (
          <Input
            size="small"
            maxLength={100}
            value={record.professionalPromotionTeam ?? ''}
            onChange={(e) =>
              updateField(record.id, 'professionalPromotionTeam', e.target.value || null)
            }
          />
        ),
      },
      {
        title: '판매단가',
        dataIndex: 'basePrice',
        width: 100,
        render: (_, record) => (
          <InputNumber
            size="small"
            min={0}
            value={record.basePrice}
            onChange={(v) => updateField(record.id, 'basePrice', v)}
            style={{ width: '100%' }}
          />
        ),
      },
      {
        title: '목표수량',
        dataIndex: 'dailyTargetCount',
        width: 90,
        render: (_, record) => (
          <InputNumber
            size="small"
            min={0}
            precision={0}
            value={record.dailyTargetCount}
            onChange={(v) => updateField(record.id, 'dailyTargetCount', v)}
            style={{ width: '100%' }}
          />
        ),
      },
      confirmColumn,
      closeColumn,
      {
        title: '',
        width: 60,
        render: (_, record) => (
          <Popconfirm
            title="삭제하시겠습니까?"
            onConfirm={() => handleDeleteInEditMode(record.id)}
            okText="확인"
            cancelText="취소"
          >
            <Button type="link" size="small" danger>
              삭제
            </Button>
          </Popconfirm>
        ),
      },
    ],
    [confirmColumn, closeColumn, updateField],
  );

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error || !promotion) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/promotions')}>
          ← 목록으로
        </Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          행사마스터를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const categoryColor = promotion.category ? CATEGORY_TAG[promotion.category] : undefined;
  const typeColor = promotion.promotionTypeName
    ? PROMOTION_TYPE_TAG[promotion.promotionTypeName]
    : undefined;

  const employeeCount = employees?.length ?? 0;
  const canEdit = hasDates && employeeCount > 0;
  const editDisabledTooltip = !hasDates
    ? '행사 시작일과 종료일을 먼저 입력하세요'
    : employeeCount === 0
      ? '조원을 먼저 추가하세요'
      : '';
  const addDisabledTooltip = !hasDates ? '행사 시작일과 종료일을 먼저 입력하세요' : '';

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Button type="link" onClick={() => navigate('/promotions')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          <Button onClick={() => navigate(`/promotions/${promotionId}/edit`)}>수정</Button>
          <Button danger onClick={handleDelete}>
            삭제
          </Button>
        </Space>
      </div>

      <Descriptions column={1} bordered>
        <Descriptions.Item label="행사번호">{promotion.promotionNumber}</Descriptions.Item>
        <Descriptions.Item label="행사명">
          {promotion.promotionName ? (
            <>
              {promotion.promotionName}
              <Typography.Text type="secondary"> (자동)</Typography.Text>
            </>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="행사유형">
          {promotion.promotionTypeName ? (
            <Tag color={typeColor}>{promotion.promotionTypeName}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="거래처">{promotion.accountName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="기간">
          {promotion.startDate} ~ {promotion.endDate}
        </Descriptions.Item>
        <Descriptions.Item label="대표상품">
          {promotion.primaryProductName ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="기타상품">{promotion.otherProduct ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="비고">{promotion.remark ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="카테고리">
          {promotion.category ? (
            <Tag color={categoryColor}>{promotion.category}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="제품유형">{promotion.productType ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="지점명">{promotion.branchName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="매대위치">{promotion.standLocation ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="목표금액">
          {promotion.targetAmount != null
            ? `${promotion.targetAmount.toLocaleString()}원`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="실적금액">
          {promotion.actualAmount != null
            ? `${promotion.actualAmount.toLocaleString()}원`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="마감 여부">
          {promotion.isClosed ? <Tag color="red">마감</Tag> : '미마감'}
        </Descriptions.Item>
        <Descriptions.Item label="메시지">{promotion.message ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="CC코드">{promotion.costCenterCode ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="생성일">
          {promotion.createdAt?.substring(0, 16)}
        </Descriptions.Item>
        <Descriptions.Item label="수정일">
          {promotion.updatedAt?.substring(0, 16)}
        </Descriptions.Item>
      </Descriptions>

      {/* 행사조원 섹션 */}
      <div style={{ marginTop: 32 }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <Title level={5} style={{ margin: 0 }}>
            행사조원
          </Title>
          {editMode ? (
            <Space>
              <Button onClick={cancelEditMode}>취소</Button>
              <Button
                type="primary"
                onClick={handleSave}
                loading={batchUpdateMutation.isPending}
              >
                저장
              </Button>
            </Space>
          ) : (
            <Space>
              <Button disabled title="Spec #191에서 구현 예정">
                일정 확정
              </Button>
              <Tooltip title={addDisabledTooltip}>
                <Button
                  type="primary"
                  onClick={handleAddEmployee}
                  loading={createPeMutation.isPending}
                  disabled={!hasDates}
                >
                  + 조원 추가
                </Button>
              </Tooltip>
              <Tooltip title={editDisabledTooltip}>
                <Button onClick={enterEditMode} disabled={!canEdit}>
                  편집
                </Button>
              </Tooltip>
            </Space>
          )}
        </div>

        {editMode ? (
          <Table<EditableRow>
            columns={editColumns}
            dataSource={editRows}
            rowKey="id"
            size="small"
            pagination={false}
            locale={{ emptyText: '등록된 조원이 없습니다' }}
            footer={() => `총 ${editRows.length}건`}
            rowClassName={(record) => (errorRowIds.has(record.id) ? 'ant-table-row-error' : '')}
            onRow={(record) => ({
              title: errorMessages.get(record.id) || undefined,
            })}
            scroll={{ x: 1200 }}
          />
        ) : (
          <Table<PromotionEmployee>
            columns={readColumns}
            dataSource={employees ?? []}
            rowKey="id"
            size="small"
            pagination={false}
            loading={employeesLoading}
            locale={{ emptyText: '등록된 조원이 없습니다' }}
            footer={() => `총 ${employees?.length ?? 0}건`}
          />
        )}
      </div>

      {/* Error row highlight style */}
      <style>{`
        .ant-table-row-error td {
          background-color: #fff2f0 !important;
        }
      `}</style>
    </div>
  );
}
