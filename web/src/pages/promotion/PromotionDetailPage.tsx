import { useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Collapse,
  DatePicker,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import { useDeletePromotion, useUpdatePromotion } from '@/hooks/promotion/usePromotionMutation';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { usePromotionEmployees } from '@/hooks/promotion/usePromotionEmployees';
import {
  useCreatePromotionEmployee,
  useDeletePromotionEmployee,
  useBatchUpdatePromotionEmployees,
} from '@/hooks/promotion/usePromotionEmployeeMutation';
import type { PromotionEmployee, PromotionEmployeeFormData } from '@/api/promotionEmployee';
import {
  BatchValidationError,
  type BatchUpdatePromotionEmployeeItem,
} from '@/api/promotionEmployee';
import { fetchEmployees } from '@/api/employee';
import type { Employee } from '@/api/employee';
import type { PromotionFormData } from '@/api/promotion';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import PromotionDetailSection, {
  type DetailFormValues,
} from './sections/PromotionDetailSection';
import PromotionProductSection, {
  type ProductFormValues,
} from './sections/PromotionProductSection';
import PromotionAmountSection from './sections/PromotionAmountSection';

const { Title } = Typography;

const WORK_STATUS_OPTIONS = [
  { label: '근무', value: '근무' },
  { label: '연차', value: '연차' },
  { label: '대휴', value: '대휴' },
];

const WORK_TYPE3_OPTIONS = [
  { label: '--None--', value: '' },
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
  primaryProductAmount: number | null;
  primarySalesQuantity: number | null;
  otherSalesAmount: number | null;
  otherSalesQuantity: number | null;
  s3ImageUniqueKey: string | null;
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
    targetAmount: pe.targetAmount,
    actualAmount: pe.actualAmount,
    employeeName: pe.employeeName,
    scheduleId: pe.scheduleId,
    promoCloseByTm: pe.promoCloseByTm,
    primaryProductAmount: pe.primaryProductAmount,
    primarySalesQuantity: pe.primarySalesQuantity,
    otherSalesAmount: pe.otherSalesAmount,
    otherSalesQuantity: pe.otherSalesQuantity,
    s3ImageUniqueKey: pe.s3ImageUniqueKey,
  };
}

const REQUIRED_FIELDS: (keyof EditableRow)[] = [
  'scheduleDate',
  'workStatus',
  'workType1',
];

export default function PromotionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const promotionId = Number(id);

  const { data: promotion, isLoading, error } = usePromotion(promotionId);
  const deleteMutation = useDeletePromotion();
  const updateMutation = useUpdatePromotion();
  const { data: formMeta } = usePromotionFormMeta();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  // --- 행사 인라인 편집 상태 ---
  const [promotionEditing, setPromotionEditing] = useState(false);
  const [detailForm, setDetailForm] = useState<DetailFormValues>({
    accountId: 0,
    accountName: null,
    startDate: '',
    endDate: '',
    promotionTypeId: null,
    standLocation: null,
    message: null,
  });
  const [productForm, setProductForm] = useState<ProductFormValues>({
    primaryProductId: null,
    primaryProductName: null,
    otherProduct: null,
    remark: null,
  });

  // --- 행사조원 ---
  const { data: employees, isLoading: employeesLoading } = usePromotionEmployees(promotionId);
  const createPeMutation = useCreatePromotionEmployee();
  const deletePeMutation = useDeletePromotionEmployee();
  const batchUpdateMutation = useBatchUpdatePromotionEmployees();

  // --- 행사사원 편집 모드 상태 ---
  const [empEditMode, setEmpEditMode] = useState(false);
  const [editRows, setEditRows] = useState<EditableRow[]>([]);
  const [errorRowIds, setErrorRowIds] = useState<Set<number>>(new Set());
  const [errorMessages, setErrorMessages] = useState<Map<number, string>>(new Map());

  // --- 사원 Lookup 검색 상태 ---
  const [employeeSearchResults, setEmployeeSearchResults] = useState<Employee[]>([]);
  const [employeeSearching, setEmployeeSearching] = useState(false);
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setDynamicTitle(promotion?.promotionNumber ?? null);
    return () => setDynamicTitle(null);
  }, [promotion?.promotionNumber, setDynamicTitle]);

  const hasDates = promotion?.startDate && promotion?.endDate;

  // --- 행사 인라인 편집: 진입 ---
  const enterPromotionEdit = useCallback(() => {
    if (!promotion) return;
    setDetailForm({
      accountId: promotion.accountId,
      accountName: promotion.accountName,
      startDate: promotion.startDate,
      endDate: promotion.endDate,
      promotionTypeId: promotion.promotionTypeId,
      standLocation: promotion.standLocation,
      message: promotion.message,
    });
    setProductForm({
      primaryProductId: promotion.primaryProductId,
      primaryProductName: promotion.primaryProductName,
      otherProduct: promotion.otherProduct,
      remark: promotion.remark,
    });
    setPromotionEditing(true);
  }, [promotion]);

  // --- 행사 인라인 편집: 취소 ---
  const cancelPromotionEdit = useCallback(() => {
    const hasDetailChange = promotion && (
      detailForm.accountId !== promotion.accountId ||
      detailForm.startDate !== promotion.startDate ||
      detailForm.endDate !== promotion.endDate ||
      detailForm.promotionTypeId !== promotion.promotionTypeId ||
      detailForm.standLocation !== promotion.standLocation ||
      detailForm.message !== promotion.message
    );
    const hasProductChange = promotion && (
      productForm.primaryProductId !== promotion.primaryProductId ||
      productForm.otherProduct !== promotion.otherProduct ||
      productForm.remark !== promotion.remark
    );

    if (hasDetailChange || hasProductChange) {
      Modal.confirm({
        title: '편집 취소',
        content: '수정 내용이 저장되지 않습니다. 취소하시겠습니까?',
        okText: '확인',
        cancelText: '계속 편집',
        onOk: () => setPromotionEditing(false),
      });
    } else {
      setPromotionEditing(false);
    }
  }, [promotion, detailForm, productForm]);

  // --- 행사 인라인 편집: 저장 ---
  const savePromotionEdit = useCallback(async () => {
    if (!promotion) return;

    // Client validation
    if (!detailForm.accountId) {
      message.error('거래처를 선택하세요');
      return;
    }
    if (!detailForm.startDate) {
      message.error('시작일을 입력하세요');
      return;
    }
    if (!detailForm.endDate) {
      message.error('종료일을 입력하세요');
      return;
    }
    if (detailForm.endDate < detailForm.startDate) {
      message.error('종료일은 시작일 이후여야 합니다');
      return;
    }
    if (!productForm.primaryProductId) {
      message.error('대표제품을 선택하세요');
      return;
    }

    const data: PromotionFormData = {
      promotion_type_id: detailForm.promotionTypeId ?? promotion.promotionTypeId ?? 0,
      account_id: detailForm.accountId,
      start_date: detailForm.startDate,
      end_date: detailForm.endDate,
      primary_product_id: productForm.primaryProductId,
      other_product: productForm.otherProduct,
      message: detailForm.message,
      stand_location: detailForm.standLocation ?? '',
      remark: productForm.remark,
    };

    try {
      await updateMutation.mutateAsync({ id: promotionId, data });
      message.success('저장되었습니다');
      setPromotionEditing(false);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '수정에 실패했습니다');
    }
  }, [promotion, promotionId, detailForm, productForm, updateMutation]);

  // --- 행사 삭제 ---
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

  // --- 즉시 추가 (마지막 행 복사) ---
  const handleAddEmployee = async () => {
    try {
      let data: PromotionEmployeeFormData | undefined;

      if (employees && employees.length > 0) {
        const lastRow = employees[employees.length - 1];
        const body: PromotionEmployeeFormData = {};

        if (lastRow.employeeSfid != null) body.employee_sfid = lastRow.employeeSfid;
        if (lastRow.scheduleDate != null) {
          body.schedule_date = dayjs(lastRow.scheduleDate).add(1, 'day').format('YYYY-MM-DD');
        }
        if (lastRow.workStatus != null) body.work_status = lastRow.workStatus;
        if (lastRow.workType1 != null) body.work_type1 = lastRow.workType1;
        if (lastRow.workType3 != null) body.work_type3 = lastRow.workType3;
        if (lastRow.professionalPromotionTeam != null) {
          body.professional_promotion_team = lastRow.professionalPromotionTeam;
        }

        if (Object.keys(body).length > 0) data = body;
      }

      await createPeMutation.mutateAsync({ promotionId, data });
    } catch (err) {
      message.error(err instanceof Error ? err.message : '행사사원 추가에 실패했습니다');
    }
  };

  // --- 행사사원 편집 모드 진입 ---
  const enterEmpEditMode = useCallback(() => {
    if (!employees) return;
    setEditRows(employees.map(toEditableRow));
    setErrorRowIds(new Set());
    setErrorMessages(new Map());
    setEmpEditMode(true);
  }, [employees]);

  // --- 행사사원 편집 모드 취소 ---
  const cancelEmpEditMode = () => {
    setEmpEditMode(false);
    setEditRows([]);
    setErrorRowIds(new Set());
    setErrorMessages(new Map());
  };

  // --- 사원 Lookup 검색 ---
  const handleEmployeeSearch = useCallback((keyword: string) => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);

    if (keyword.length < 2) {
      setEmployeeSearchResults([]);
      setEmployeeSearching(false);
      return;
    }

    setEmployeeSearching(true);
    searchTimerRef.current = setTimeout(async () => {
      try {
        const result = await fetchEmployees({ keyword, size: 10 });
        setEmployeeSearchResults(result.content);
      } catch {
        setEmployeeSearchResults([]);
      } finally {
        setEmployeeSearching(false);
      }
    }, 300);
  }, []);

  // --- 행 필드 변경 ---
  const updateField = useCallback(
    (rowId: number, field: keyof EditableRow, value: unknown) => {
      setEditRows((prev) =>
        prev.map((row) => (row.id === rowId ? { ...row, [field]: value } : row)),
      );
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
          employee_sfid: row.employeeSfid || null,
          schedule_date: row.scheduleDate ?? '',
          work_status: row.workStatus ?? '',
          work_type1: row.workType1 ?? '',
          work_type3: row.workType3 || null,
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

  // --- 행사사원 저장 ---
  const handleEmpSave = async () => {
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
      setEmpEditMode(false);
      setEditRows([]);
      setErrorRowIds(new Set());
      setErrorMessages(new Map());
    } catch (err) {
      if (err instanceof BatchValidationError) {
        const errorIds = new Set<number>();
        const msgs = new Map<number, string>();

        for (const itemErr of err.errors) {
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

  // --- 숫자 포맷 헬퍼 ---
  const fmtNum = (v: number | null | undefined): string => {
    if (v == null) return '';
    return v.toLocaleString();
  };

  // --- 확정 상태 표시 컬럼 (공통) ---
  const confirmColumn = useMemo(
    () => ({
      title: '확정',
      dataIndex: 'scheduleId',
      width: 60,
      align: 'center' as const,
      render: (v: number | null) =>
        v != null ? <CheckCircleFilled style={{ color: '#52c41a' }} /> : null,
    }),
    [],
  );

  const closeColumn = useMemo(
    () => ({
      title: '여사마감',
      dataIndex: 'promoCloseByTm',
      width: 70,
      align: 'center' as const,
      render: (v: boolean) =>
        v ? <CloseCircleFilled style={{ color: '#ff4d4f' }} /> : null,
    }),
    [],
  );

  // --- 읽기 모드 컬럼 ---
  const readColumns: ColumnsType<PromotionEmployee> = useMemo(
    () => [
      { title: 'NO.', dataIndex: 'id', width: 70, align: 'center' as const },
      {
        title: '행사사원',
        dataIndex: 'employeeName',
        width: 120,
        render: (name: string | null, record: PromotionEmployee) =>
          name ?? record.employeeSfid ?? '-',
      },
      {
        title: '전문행사조(현재)',
        width: 130,
        align: 'center' as const,
        render: () => '-',
      },
      {
        title: '전문행사조(투입당시)',
        dataIndex: 'professionalPromotionTeam',
        width: 130,
        align: 'center' as const,
        render: (v: string | null) => v ?? '-',
      },
      {
        title: '투입일',
        dataIndex: 'scheduleDate',
        width: 110,
        align: 'center' as const,
        render: (v: string | null) => (v ? v.replace(/-/g, '/') : '-'),
      },
      {
        title: '근무유형3',
        dataIndex: 'workType3',
        width: 80,
        align: 'center' as const,
        render: (v: string | null) => v ?? '-',
      },
      {
        title: '기준단가',
        dataIndex: 'basePrice',
        width: 90,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '목표수량',
        dataIndex: 'dailyTargetCount',
        width: 80,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '목표금액',
        dataIndex: 'targetAmount',
        width: 110,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '대표품목 매출',
        dataIndex: 'primaryProductAmount',
        width: 110,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '진도율',
        width: 70,
        align: 'right' as const,
        render: (_: unknown, record: PromotionEmployee) => {
          if (!record.targetAmount || record.targetAmount === 0) return '0%';
          if (record.primaryProductAmount == null) return '0%';
          return `${Math.floor((record.primaryProductAmount / record.targetAmount) * 100)}%`;
        },
      },
      {
        title: '대표품목판매수량',
        dataIndex: 'primarySalesQuantity',
        width: 120,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '기타판매금액',
        dataIndex: 'otherSalesAmount',
        width: 100,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '기타판매수량',
        dataIndex: 'otherSalesQuantity',
        width: 100,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '총 실적',
        width: 100,
        align: 'right' as const,
        render: (_: unknown, record: PromotionEmployee) => {
          if (record.primaryProductAmount == null && record.otherSalesAmount == null) return '';
          return fmtNum((record.primaryProductAmount ?? 0) + (record.otherSalesAmount ?? 0));
        },
      },
      {
        title: '현장사진',
        dataIndex: 's3ImageUniqueKey',
        width: 70,
        align: 'center' as const,
        render: (v: string | null) =>
          v ? <CheckCircleFilled style={{ color: '#52c41a' }} /> : null,
      },
      confirmColumn,
      closeColumn,
    ],
    [confirmColumn, closeColumn],
  );

  // --- 편집 모드 컬럼 ---
  const editColumns: ColumnsType<EditableRow> = useMemo(
    () => [
      { title: 'NO.', dataIndex: 'id', width: 70, align: 'center' as const },
      {
        title: '행사사원',
        dataIndex: 'employeeName',
        width: 120,
        render: (name: string | null, record: EditableRow) =>
          name ?? record.employeeSfid ?? '-',
      },
      {
        title: '전문행사조(현재)',
        width: 130,
        align: 'center' as const,
        render: () => '-',
      },
      {
        title: '전문행사조(투입당시)',
        dataIndex: 'professionalPromotionTeam',
        width: 130,
        render: (_: unknown, record: EditableRow) => (
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
        title: '투입일',
        dataIndex: 'scheduleDate',
        width: 140,
        render: (_: unknown, record: EditableRow) => (
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
        title: '근무유형3',
        dataIndex: 'workType3',
        width: 90,
        render: (_: unknown, record: EditableRow) => (
          <Select
            size="small"
            options={WORK_TYPE3_OPTIONS}
            value={record.workType3 ?? ''}
            onChange={(v) => updateField(record.id, 'workType3', v || null)}
            style={{ width: '100%' }}
            allowClear={false}
          />
        ),
      },
      {
        title: '기준단가',
        dataIndex: 'basePrice',
        width: 100,
        render: (_: unknown, record: EditableRow) => (
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
        render: (_: unknown, record: EditableRow) => (
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
      {
        title: '목표금액',
        width: 110,
        align: 'right' as const,
        render: (_: unknown, record: EditableRow) => {
          const computed = (record.basePrice ?? 0) * (record.dailyTargetCount ?? 0);
          return computed ? fmtNum(computed) : '';
        },
      },
      {
        title: '대표품목 매출',
        dataIndex: 'primaryProductAmount',
        width: 110,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '진도율',
        width: 70,
        align: 'right' as const,
        render: (_: unknown, record: EditableRow) => {
          const target = (record.basePrice ?? 0) * (record.dailyTargetCount ?? 0);
          if (!target) return '0%';
          if (record.primaryProductAmount == null) return '0%';
          return `${Math.floor((record.primaryProductAmount / target) * 100)}%`;
        },
      },
      {
        title: '대표품목판매수량',
        dataIndex: 'primarySalesQuantity',
        width: 120,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '기타판매금액',
        dataIndex: 'otherSalesAmount',
        width: 100,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '기타판매수량',
        dataIndex: 'otherSalesQuantity',
        width: 100,
        align: 'right' as const,
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '총 실적',
        width: 100,
        align: 'right' as const,
        render: (_: unknown, record: EditableRow) => {
          if (record.primaryProductAmount == null && record.otherSalesAmount == null) return '';
          return fmtNum((record.primaryProductAmount ?? 0) + (record.otherSalesAmount ?? 0));
        },
      },
      {
        title: '현장사진',
        dataIndex: 's3ImageUniqueKey',
        width: 70,
        align: 'center' as const,
        render: (v: string | null) =>
          v ? <CheckCircleFilled style={{ color: '#52c41a' }} /> : null,
      },
      confirmColumn,
      closeColumn,
      // --- 편집 모드에서만 추가 표시되는 컬럼 ---
      {
        title: '여사원 SF ID',
        dataIndex: 'employeeSfid',
        width: 200,
        render: (_: unknown, record: EditableRow) => (
          <Select
            size="small"
            showSearch
            filterOption={false}
            placeholder="사원 검색..."
            value={record.employeeSfid ? { value: record.employeeSfid, label: record.employeeName ?? record.employeeSfid } : undefined}
            labelInValue
            allowClear
            onSearch={handleEmployeeSearch}
            onChange={(option) => {
              if (option) {
                const selected = employeeSearchResults.find((e) => e.sfid === option.value);
                updateField(record.id, 'employeeSfid', option.value);
                updateField(record.id, 'employeeName', selected?.name ?? option.label);
              }
            }}
            onClear={() => {
              updateField(record.id, 'employeeSfid', null);
              updateField(record.id, 'employeeName', null);
            }}
            notFoundContent={
              employeeSearching ? <Spin size="small" /> : '검색 결과 없음'
            }
            options={employeeSearchResults
              .filter((e) => e.sfid)
              .map((e) => ({
                value: e.sfid!,
                label: `${e.name} (${e.employeeId})`,
              }))}
            style={{ width: '100%' }}
          />
        ),
      },
      {
        title: '근무상태',
        dataIndex: 'workStatus',
        width: 90,
        render: (_: unknown, record: EditableRow) => (
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
        render: (_: unknown, record: EditableRow) => (
          <Input
            size="small"
            maxLength={100}
            value={record.workType1 ?? ''}
            onChange={(e) => updateField(record.id, 'workType1', e.target.value || null)}
          />
        ),
      },
      {
        title: '근무유형4',
        dataIndex: 'workType4',
        width: 100,
        render: (_: unknown, record: EditableRow) => (
          <Input
            size="small"
            maxLength={100}
            value={record.workType4 ?? ''}
            onChange={(e) => updateField(record.id, 'workType4', e.target.value || null)}
          />
        ),
      },
      {
        title: '삭제',
        width: 60,
        render: (_: unknown, record: EditableRow) => (
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
    [confirmColumn, closeColumn, updateField, handleEmployeeSearch, employeeSearchResults, employeeSearching],
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

  const employeeCount = employees?.length ?? 0;
  const canEmpEdit = hasDates && employeeCount > 0;
  const empEditDisabledTooltip = !hasDates
    ? '행사 시작일과 종료일을 먼저 입력하세요'
    : employeeCount === 0
      ? '행사사원을 먼저 추가하세요'
      : '';
  const addDisabledTooltip = !hasDates ? '행사 시작일과 종료일을 먼저 입력하세요' : '';

  return (
    <div style={{ padding: 16 }}>
      {/* 상단 헤더: 목록으로 + 수정/삭제 or 취소/저장 */}
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
        {promotionEditing ? (
          <Space>
            <Button onClick={cancelPromotionEdit}>취소</Button>
            <Button type="primary" onClick={savePromotionEdit} loading={updateMutation.isPending}>
              저장
            </Button>
          </Space>
        ) : (
          <Space>
            <Button onClick={enterPromotionEdit}>수정</Button>
            <Button danger onClick={handleDelete}>
              삭제
            </Button>
          </Space>
        )}
      </div>

      {/* 3개 섹션: Collapse */}
      <Collapse
        defaultActiveKey={['detail', 'product', 'amount']}
        items={[
          {
            key: 'detail',
            label: '세부 사항',
            children: (
              <PromotionDetailSection
                promotion={promotion}
                editing={promotionEditing}
                formValues={detailForm}
                onFormChange={(v) => setDetailForm((prev) => ({ ...prev, ...v }))}
                formMeta={formMeta}
              />
            ),
          },
          {
            key: 'product',
            label: '행사품목',
            children: (
              <PromotionProductSection
                promotion={promotion}
                editing={promotionEditing}
                formValues={productForm}
                onFormChange={(v) => setProductForm((prev) => ({ ...prev, ...v }))}
              />
            ),
          },
          {
            key: 'amount',
            label: '목표/실적',
            children: <PromotionAmountSection promotion={promotion} />,
          },
        ]}
      />

      {/* 행사사원 섹션 */}
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
            행사사원
          </Title>
          {empEditMode ? (
            <Space>
              <Button onClick={cancelEmpEditMode}>취소</Button>
              <Button
                type="primary"
                onClick={handleEmpSave}
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
              <Tooltip title={empEditDisabledTooltip}>
                <Button onClick={enterEmpEditMode} disabled={!canEmpEdit}>
                  편집
                </Button>
              </Tooltip>
            </Space>
          )}
        </div>

        {empEditMode ? (
          <Table<EditableRow>
            columns={editColumns}
            dataSource={editRows}
            rowKey="id"
            size="small"
            pagination={false}
            locale={{ emptyText: '등록된 행사사원이 없습니다' }}
            footer={() => `총 ${editRows.length}건`}
            rowClassName={(record) => (errorRowIds.has(record.id) ? 'ant-table-row-error' : '')}
            onRow={(record) => ({
              title: errorMessages.get(record.id) || undefined,
            })}
            scroll={{ x: 2400 }}
          />
        ) : (
          <Table<PromotionEmployee>
            columns={readColumns}
            dataSource={employees ?? []}
            rowKey="id"
            size="small"
            pagination={false}
            loading={employeesLoading}
            locale={{ emptyText: '등록된 행사사원이 없습니다' }}
            footer={() => (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>총 {employees?.length ?? 0}건</span>
                <Tooltip title={addDisabledTooltip}>
                  <Button
                    type="primary"
                    onClick={handleAddEmployee}
                    loading={createPeMutation.isPending}
                    disabled={!hasDates}
                  >
                    + 행사사원 추가
                  </Button>
                </Tooltip>
              </div>
            )}
            scroll={{ x: 1800 }}
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
