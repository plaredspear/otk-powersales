import { useContext, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Collapse,
  DatePicker,
  Divider,
  Image,
  InputNumber,
  Modal,
  Popconfirm,
  Popover,
  Select,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  message,
  notification,
} from 'antd';
import { CheckCircleFilled, CloseCircleFilled, CloseOutlined, ExclamationCircleOutlined, InfoCircleOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import {
  useCloneWithChildren,
  useDeletePromotion,
  useUpdatePromotion,
} from '@/hooks/promotion/usePromotionMutation';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { usePromotionEmployees } from '@/hooks/promotion/usePromotionEmployees';
import {
  useCreatePromotionEmployee,
  useDeletePromotionEmployee,
  useBatchUpdatePromotionEmployees,
  useConfirmPromotionSchedule,
} from '@/hooks/promotion/usePromotionEmployeeMutation';
import type { PromotionEmployee, PromotionEmployeeFormData } from '@/api/promotionEmployee';
import {
  BatchValidationError,
  type BatchUpdatePromotionEmployeeItem,
} from '@/api/promotionEmployee';
import type { PromotionFormData } from '@/api/promotion';
import { fetchEmployeesForPromotionLookup, type Employee } from '@/api/employee';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import { usePermission } from '@/hooks/usePermission';
import PromotionDetailSection, {
  type DetailFormValues,
} from './sections/PromotionDetailSection';
import PromotionProductSection, {
  type ProductFormValues,
} from './sections/PromotionProductSection';
import PromotionTargetActualSection from './sections/PromotionTargetActualSection';
import PromotionPosProductSection from './sections/PromotionPosProductSection';
import { getPPTTeamTypeColor } from '@/constants/pptTeamType';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import ResizableTable from '@/components/common/ResizableTable';

const { Title } = Typography;

const WORK_TYPE3_OPTIONS = [
  { label: '--None--', value: '' },
  { label: '고정', value: '고정' },
  { label: '격고', value: '격고' },
  { label: '순회', value: '순회' },
];

// Editable row data during edit mode
interface EditableRow {
  id: number;
  employeeId: number | null;
  employeeCode: string | null;
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
  primaryProductAmount: number | null;
  primarySalesQuantity: number | null;
  otherSalesAmount: number | null;
  otherSalesQuantity: number | null;
  s3ImageUniqueKey: string | null;
  siteImageUrl: string | null;
  promoCloseByTm: boolean;
  // Read-only display fields
  employeeName: string | null;
  scheduleId: number | null;
  currentProfessionalPromotionTeam: string | null;
}

function toEditableRow(pe: PromotionEmployee): EditableRow {
  return {
    id: pe.id,
    employeeId: pe.employeeId,
    employeeCode: pe.employeeCode,
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
    currentProfessionalPromotionTeam: pe.currentProfessionalPromotionTeam,
    promoCloseByTm: pe.promoCloseByTm,
    primaryProductAmount: pe.primaryProductAmount,
    primarySalesQuantity: pe.primarySalesQuantity,
    otherSalesAmount: pe.otherSalesAmount,
    otherSalesQuantity: pe.otherSalesQuantity,
    s3ImageUniqueKey: pe.s3ImageUniqueKey,
    siteImageUrl: pe.siteImageUrl,
  };
}

const REQUIRED_FIELDS: (keyof EditableRow)[] = [
  'scheduleDate',
];

export default function PromotionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const promotionId = Number(id);
  // 목록에서 넘어온 경우 직전 목록의 query string(page/필터)을 붙여 복귀 — "목록으로" 시 조건 초기화 방지.
  const listSearch = (location.state as { listSearch?: string } | null)?.listSearch ?? '';
  const listPath = `/promotions${listSearch}`;

  const { data: promotion, isLoading, error } = usePromotion(promotionId);
  const deleteMutation = useDeletePromotion();
  const updateMutation = useUpdatePromotion();
  const cloneWithChildrenMutation = useCloneWithChildren();
  const { data: formMeta } = usePromotionFormMeta();
  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('promotion', 'EDIT');
  // 행사사원은 여사원이므로 여사원 상세(female_employee) 로 이동한다. 조장 등 여사원 권한만
  // 가진 직책도 링크를 볼 수 있도록 female_employee READ 를 우선 판정하고, 전체 사원 관리
  // (employee) 권한만 가진 관리자도 링크가 보이도록 OR 로 확장한다.
  const canReadFemaleEmployee = hasEntityPermission('female_employee', 'READ');
  const canReadEmployee = canReadFemaleEmployee || hasEntityPermission('employee', 'READ');

  // NO.(행사사원 코드) 클릭 시 해당 사원 상세로 이동 — SF 레거시의 행사사원 레코드 링크 대체.
  // female_employee 권한 보유 시 여사원 상세 URL 로, 그 외(employee 권한만) 는 사원 상세 URL 로
  // 진입 — 각 URL prefix 가 상세 페이지의 권한 자원/조회 endpoint 를 결정하기 때문.
  const goToEmployee = useThrottleClick((employeeId: number) =>
    navigate(canReadFemaleEmployee ? `/female-employee/${employeeId}` : `/employee/${employeeId}`),
  );

  // --- 행사 인라인 편집 상태 ---
  const [promotionEditing, setPromotionEditing] = useState(false);
  const [detailForm, setDetailForm] = useState<DetailFormValues>({
    accountId: 0,
    accountName: null,
    startDate: '',
    endDate: '',
    promotionType: null,
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
  const confirmMutation = useConfirmPromotionSchedule();

  // --- 행사사원 편집 모드 상태 ---
  const [empEditMode, setEmpEditMode] = useState(false);
  const [editRows, setEditRows] = useState<EditableRow[]>([]);
  const [errorRowIds, setErrorRowIds] = useState<Set<number>>(new Set());
  const [errorMessages, setErrorMessages] = useState<Map<number, string>>(new Map());

  // --- 사원 Lookup 검색 상태 ---
  const [employeeOptions, setEmployeeOptions] = useState<Map<number, Employee[]>>(new Map());
  const [employeeSearchLoading, setEmployeeSearchLoading] = useState<Map<number, boolean>>(new Map());
  const searchTimerRef = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());

  const handleEmployeeSearch = (rowId: number, keyword: string) => {
    // Clear previous timer for this row
    const timers = searchTimerRef.current;
    const prevTimer = timers.get(rowId);
    if (prevTimer) clearTimeout(prevTimer);

    if (keyword.length < 2) {
      setEmployeeOptions((prev) => { const next = new Map(prev); next.delete(rowId); return next; });
      // 직전 입력(>=2글자)으로 켜진 로딩이 남지 않도록 함께 해제 — 타이머 만료 전 한 글자로 줄이면
      // setTimeout 콜백의 finally 가 실행되지 않아 스피너가 영구히 도는 문제 방지.
      setEmployeeSearchLoading((prev) => { const next = new Map(prev); next.delete(rowId); return next; });
      return;
    }

    setEmployeeSearchLoading((prev) => new Map(prev).set(rowId, true));

    const timer = setTimeout(async () => {
      try {
        const result = await fetchEmployeesForPromotionLookup({ keyword, size: 5 });
        setEmployeeOptions((prev) => new Map(prev).set(rowId, result.content));
      } catch {
        setEmployeeOptions((prev) => { const next = new Map(prev); next.delete(rowId); return next; });
      } finally {
        setEmployeeSearchLoading((prev) => { const next = new Map(prev); next.delete(rowId); return next; });
      }
    }, 300);

    timers.set(rowId, timer);
  };

  useEffect(() => {
    setDynamicTitle(promotion?.promotionNumber ?? null);
    return () => setDynamicTitle(null);
  }, [promotion?.promotionNumber, setDynamicTitle]);

  const hasDates = promotion?.startDate && promotion?.endDate;

  // --- 행사 인라인 편집: 진입 ---
  const enterPromotionEdit = () => {
    if (!promotion) return;
    setDetailForm({
      accountId: promotion.accountId,
      accountName: promotion.accountName,
      startDate: promotion.startDate,
      endDate: promotion.endDate,
      promotionType: promotion.promotionType,
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
  };

  // --- 행사 인라인 편집: 취소 ---
  const cancelPromotionEdit = () => {
    const hasDetailChange = promotion && (
      detailForm.accountId !== promotion.accountId ||
      detailForm.startDate !== promotion.startDate ||
      detailForm.endDate !== promotion.endDate ||
      detailForm.promotionType !== promotion.promotionType ||
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
  };

  // --- 행사 인라인 편집: 저장 ---
  const savePromotionEdit = async () => {
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
      promotionType: detailForm.promotionType ?? promotion.promotionType ?? '',
      accountId: detailForm.accountId,
      startDate: detailForm.startDate,
      endDate: detailForm.endDate,
      primaryProductId: productForm.primaryProductId,
      otherProduct: productForm.otherProduct,
      message: detailForm.message,
      standLocation: detailForm.standLocation ?? '',
      remark: productForm.remark,
    };

    const executePromotionSave = async () => {
      try {
        await updateMutation.mutateAsync({ id: promotionId, data });
        message.success('저장되었습니다');
        setPromotionEditing(false);
      } catch (err) {
        message.error(err instanceof Error ? err.message : '수정에 실패했습니다');
      }
    };

    // 거래처 변경 시 연결된 확정 여사원일정이 일괄 해제됨 — 사전 안내 모달 (T9 동등)
    const accountChanged = detailForm.accountId !== promotion.accountId;
    const confirmedEmployees =
      accountChanged && employees ? employees.filter((e) => e.scheduleId != null) : [];

    if (accountChanged && confirmedEmployees.length > 0) {
      const maxDisplay = 5;
      const displayEmployees = confirmedEmployees.slice(0, maxDisplay);
      const remaining = confirmedEmployees.length - maxDisplay;
      const newAccountLabel = detailForm.accountName ?? `#${detailForm.accountId}`;
      const oldAccountLabel = promotion.accountName ?? `#${promotion.accountId}`;

      Modal.confirm({
        title: '거래처 변경 안내',
        icon: <InfoCircleOutlined style={{ color: '#1677ff' }} />,
        content: (
          <div>
            <p>
              거래처를 <b>{oldAccountLabel}</b> 에서 <b>{newAccountLabel}</b> 로 변경하면
              아래 행사사원의 확정된 여사원일정이 모두 해제됩니다.
            </p>
            <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
              {displayEmployees.map((e) => (
                <li key={e.id}>
                  {e.employeeName ?? e.employeeCode ?? '-'} ({e.scheduleDate ?? '-'})
                </li>
              ))}
              {remaining > 0 && <li>외 {remaining}명</li>}
            </ul>
            <p style={{ color: '#666', fontSize: 13 }}>
              해제된 일정은 &quot;일정 확정&quot; 버튼을 다시 눌러 재확정할 수 있습니다.
            </p>
          </div>
        ),
        okText: '확인 후 저장',
        cancelText: '취소',
        onOk: executePromotionSave,
      });
    } else {
      await executePromotionSave();
    }
  };

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

  // --- 행사마스터 복제 (폼 방식 — UC-11) ---
  const handleClone = () => {
    navigate(`/promotions/new?cloneFrom=${promotionId}`);
  };

  // --- 행사마스터 자식 포함 복제 (1클릭 — UC-12) ---
  const handleCloneWithChildren = () => {
    Modal.confirm({
      title: '자식 포함 복제',
      content: '행사마스터 정보 및 행사사원을 모두 복제하시겠습니까?',
      okText: '복제',
      cancelText: '취소',
      onOk: async () => {
        try {
          const result = await cloneWithChildrenMutation.mutateAsync(promotionId);
          message.success('행사마스터와 행사사원이 복제되었습니다');
          navigate(`/promotions/${result.id}`);
        } catch {
          message.error('행사마스터 자식 포함 복제에 실패했습니다');
        }
      },
    });
  };

  // --- 즉시 추가 (마지막 행 복사) ---
  const handleAddEmployee = async () => {
    try {
      let data: PromotionEmployeeFormData | undefined;

      // 편집 모드이면 editRows에서, 아니면 employees에서 마지막 행 복사
      const sourceRows = empEditMode && editRows.length > 0 ? editRows : employees;
      if (sourceRows && sourceRows.length > 0) {
        const lastRow = sourceRows[sourceRows.length - 1];
        const body: PromotionEmployeeFormData = {};

        if (lastRow.employeeId != null) body.employeeId = lastRow.employeeId;
        if (lastRow.scheduleDate != null) {
          body.scheduleDate = dayjs(lastRow.scheduleDate).add(1, 'day').format('YYYY-MM-DD');
        }
        if (lastRow.workStatus != null) body.workStatus = lastRow.workStatus;
        if (lastRow.workType1 != null) body.workType1 = lastRow.workType1;
        if (lastRow.workType3 != null) body.workType3 = lastRow.workType3;
        if (lastRow.professionalPromotionTeam != null) {
          body.professionalPromotionTeam = lastRow.professionalPromotionTeam;
        }

        if (Object.keys(body).length > 0) data = body;
      }

      const newEmployee = await createPeMutation.mutateAsync({ promotionId, data });

      // 편집 모드이면 서버 응답을 editRows에 즉시 추가
      if (empEditMode) {
        setEditRows((prev) => [...prev, toEditableRow(newEmployee)]);
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '행사사원 추가에 실패했습니다');
    }
  };

  // --- 행사사원 편집 모드 진입 ---
  const enterEmpEditMode = () => {
    if (!employees) return;
    setEditRows(employees.map(toEditableRow));
    setErrorRowIds(new Set());
    setErrorMessages(new Map());
    setEmpEditMode(true);
  };

  // --- 행사사원 편집 모드 취소 ---
  const cancelEmpEditMode = () => {
    setEmpEditMode(false);
    setEditRows([]);
    setErrorRowIds(new Set());
    setErrorMessages(new Map());
  };

  // --- 스케줄 확정 ---
  const handleConfirmSchedule = () => {
    const count = employees?.length ?? 0;
    Modal.confirm({
      title: '스케줄 확정',
      content: `스케줄을 확정하시겠습니까? 행사사원 ${count}건의 여사원일정이 생성/갱신됩니다.`,
      okText: '확인',
      cancelText: '취소',
      onOk: async () => {
        try {
          const result = await confirmMutation.mutateAsync(promotionId);
          message.success(`스케줄 확정 완료 (${result.upsertedSchedules}건)`);
        } catch (err) {
          const errorMessage =
            err instanceof Error ? err.message : '스케줄 확정에 실패했습니다';
          message.error(errorMessage);
        }
      },
    });
  };

  // --- 행 필드 변경 ---
  const updateField = (rowId: number, field: keyof EditableRow, value: unknown) => {
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
  };

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
  const getChangedItems = (): BatchUpdatePromotionEmployeeItem[] | null => {
    if (!employees) return null;
    const originalMap = new Map(employees.map((e) => [e.id, e]));
    const changed: BatchUpdatePromotionEmployeeItem[] = [];

    for (const row of editRows) {
      const orig = originalMap.get(row.id);

      // 신규 행 (employees 리페치 미완료 또는 방금 생성된 행): 무조건 dirty 처리
      const isDirty = !orig ||
        row.employeeId !== orig.employeeId ||
        row.scheduleDate !== orig.scheduleDate ||
        row.workStatus !== orig.workStatus ||
        row.workType1 !== orig.workType1 ||
        row.workType3 !== orig.workType3 ||
        row.workType4 !== orig.workType4 ||
        row.professionalPromotionTeam !== orig.professionalPromotionTeam ||
        row.basePrice !== orig.basePrice ||
        row.dailyTargetCount !== orig.dailyTargetCount ||
        row.primaryProductAmount !== orig.primaryProductAmount ||
        row.primarySalesQuantity !== orig.primarySalesQuantity ||
        row.otherSalesAmount !== orig.otherSalesAmount ||
        row.otherSalesQuantity !== orig.otherSalesQuantity ||
        row.s3ImageUniqueKey !== orig.s3ImageUniqueKey ||
        row.promoCloseByTm !== orig.promoCloseByTm;

      if (isDirty) {
        changed.push({
          id: row.id,
          employeeId: row.employeeId ?? null,
          scheduleDate: row.scheduleDate ?? '',
          workStatus: row.workStatus ?? '',
          workType1: row.workType1 ?? '',
          workType3: row.workType3 || null,
          workType4: row.workType4,
          professionalPromotionTeam: row.professionalPromotionTeam,
          basePrice: row.basePrice,
          dailyTargetCount: row.dailyTargetCount,
          primaryProductAmount: row.primaryProductAmount,
          primarySalesQuantity: row.primarySalesQuantity,
          otherSalesAmount: row.otherSalesAmount,
          otherSalesQuantity: row.otherSalesQuantity,
          s3ImageUniqueKey: row.s3ImageUniqueKey,
          promoCloseByTm: row.promoCloseByTm,
        });
      }
    }

    return changed;
  };

  // --- 클라이언트 검증 ---
  const validateRequired = (): boolean => {
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
  };

  // --- 일정 해제 대상 판별 ---
  interface ScheduleReleaseTarget {
    employeeName: string;
    scheduleDate: string;
    changedFields: string[];
  }

  const getScheduleReleaseTargets = (
    changedItems: BatchUpdatePromotionEmployeeItem[],
  ): ScheduleReleaseTarget[] => {
    if (!employees) return [];
    const originalMap = new Map(employees.map((e) => [e.id, e]));
    const targets: ScheduleReleaseTarget[] = [];

    for (const item of changedItems) {
      const orig = originalMap.get(item.id);
      if (!orig || !orig.scheduleId) continue; // 미확정 → 스킵

      const changedFields: string[] = [];
      if (item.employeeId !== orig.employeeId) changedFields.push('행사사원');
      if (item.scheduleDate !== (orig.scheduleDate ?? '')) changedFields.push('투입일');
      if (item.workType3 !== orig.workType3) changedFields.push('근무유형3');

      if (changedFields.length === 0) continue; // 핵심 필드 변경 없음

      // 전문행사조만 변경된 경우 스킵
      const onlyProfTeamChanged =
        item.professionalPromotionTeam !== orig.professionalPromotionTeam &&
        changedFields.length === 0;
      if (onlyProfTeamChanged) continue;

      targets.push({
        employeeName: orig.employeeName ?? orig.employeeCode ?? '-',
        scheduleDate: orig.scheduleDate ?? '-',
        changedFields,
      });
    }

    return targets;
  };

  // --- 저장 실행 (API 호출) ---
  const executeSave = async (changedItems: BatchUpdatePromotionEmployeeItem[]) => {
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
          const batchItem = changedItems[itemErr.itemIndex];
          if (batchItem) {
            errorIds.add(batchItem.id);
            msgs.set(batchItem.id, itemErr.message);
          }
        }

        setErrorRowIds(errorIds);
        setErrorMessages(msgs);

        // notification으로 항목별 상세 에러 표시
        const maxDisplay = 5;
        const displayErrors = err.errors.slice(0, maxDisplay);
        const remaining = err.errors.length - maxDisplay;
        notification.error({
          message: `검증 오류 (${err.errors.length}건)`,
          description: (
            <ul style={{ margin: 0, paddingLeft: 16 }}>
              {displayErrors.map((itemErr, idx) => {
                const rowNum = itemErr.itemIndex + 1;
                return <li key={idx}>행 {rowNum}: {itemErr.message}</li>;
              })}
              {remaining > 0 && <li>외 {remaining}건</li>}
            </ul>
          ),
          placement: 'topRight',
          duration: 0,
        });
      } else {
        message.error(err instanceof Error ? err.message : '일괄 수정에 실패했습니다');
      }
    }
  };

  // --- 행사사원 저장 ---
  const handleEmpSave = async () => {
    if (!validateRequired()) return;

    const changedItems = getChangedItems();
    if (!changedItems || changedItems.length === 0) {
      cancelEmpEditMode();
      return;
    }

    // 일정 해제 대상 판별
    const releaseTargets = getScheduleReleaseTargets(changedItems);

    if (releaseTargets.length > 0) {
      const maxDisplay = 5;
      const displayTargets = releaseTargets.slice(0, maxDisplay);
      const remaining = releaseTargets.length - maxDisplay;

      Modal.confirm({
        title: '일정 확정 변경 안내',
        icon: <InfoCircleOutlined style={{ color: '#1677ff' }} />,
        content: (
          <div>
            <p>아래 행사사원의 확정된 여사원일정이 해제됩니다.</p>
            <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
              {displayTargets.map((t, idx) => (
                <li key={idx}>
                  {t.employeeName} ({t.scheduleDate}) — {t.changedFields.join(', ')} 변경
                </li>
              ))}
              {remaining > 0 && <li>외 {remaining}명</li>}
            </ul>
            <p style={{ color: '#666', fontSize: 13 }}>
              해제된 일정은 &quot;일정 확정&quot; 버튼을 다시 눌러 재확정할 수 있습니다.
            </p>
          </div>
        ),
        okText: '확인 후 저장',
        cancelText: '취소',
        onOk: () => executeSave(changedItems),
      });
    } else {
      await executeSave(changedItems);
    }
  };

  // --- 숫자 포맷 헬퍼 ---
  const fmtNum = (v: number | null | undefined): string => {
    if (v == null) return '';
    return v.toLocaleString();
  };

  // SF 여사원마감(Formula_PromoCloseByTm__c): IF(PromoCloseByTm__c = true, '✔️', '❌')
  // 여사원 모바일 일매출 마감 완료(true) 시 ✔️, 미마감(false) 시 ❌
  const closeColumn = {
    title: '여사원마감',
    dataIndex: 'promoCloseByTm',
    width: 70,
    align: 'center' as const,
    render: (v: boolean) =>
      v ? (
        <CheckCircleFilled style={{ color: '#52c41a' }} />
      ) : (
        <CloseCircleFilled style={{ color: '#ff4d4f' }} />
      ),
  };

  // --- 읽기 모드 컬럼 ---
  const readColumns: ColumnsType<PromotionEmployee> = [
      {
        title: 'NO.',
        dataIndex: 'name',
        width: 110,
        align: 'center' as const,
        render: (name: string | null, record: PromotionEmployee) =>
          name && canReadEmployee && record.employeeId != null ? (
            <a onClick={() => goToEmployee(record.employeeId!)}>{name}</a>
          ) : (
            name ?? '-'
          ),
      },
      {
        title: <span>행사사원<span style={{ color: '#fa8c16', marginLeft: 2 }}>**</span></span>,
        dataIndex: 'employeeName',
        width: 120,
        align: 'center' as const,
        render: (name: string | null, record: PromotionEmployee) =>
          name ?? record.employeeCode ?? '-',
      },
      {
        title: '전문행사조(현재)',
        dataIndex: 'currentProfessionalPromotionTeam',
        width: 130,
        align: 'center' as const,
        render: (v: string | null) =>
          v ? <Tag color={getPPTTeamTypeColor(v)}>{v}</Tag> : '-',
      },
      {
        title: '전문행사조(투입당시)',
        dataIndex: 'professionalPromotionTeam',
        width: 130,
        align: 'center' as const,
        render: (v: string | null) =>
          v ? <Tag color={getPPTTeamTypeColor(v)}>{v}</Tag> : '-',
      },
      {
        title: <span>투입일<span style={{ color: '#ff4d4f', marginLeft: 2 }}>*</span></span>,
        dataIndex: 'scheduleDate',
        width: 110,
        align: 'center' as const,
        render: (v: string | null) => (v ? v.replace(/-/g, '/') : '-'),
      },
      {
        title: <span>근무유형3<span style={{ color: '#fa8c16', marginLeft: 2 }}>**</span></span>,
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
        onHeaderCell: () => ({ className: 'promo-emp-hl-pink' }),
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
        onHeaderCell: () => ({ className: 'promo-emp-hl-pink' }),
        render: (v: number | null) => fmtNum(v),
      },
      {
        title: '기타판매수량',
        dataIndex: 'otherSalesQuantity',
        width: 100,
        align: 'right' as const,
        onHeaderCell: () => ({ className: 'promo-emp-hl-yellow' }),
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
        dataIndex: 'siteImageUrl',
        width: 80,
        align: 'center' as const,
        render: (url: string | null, record: PromotionEmployee) => {
          // URL 있음: 실제 썸네일. URL 없고 key 만 있음: 로컬 환경(미리보기 미지원) — 존재는 아이콘으로 안내.
          // 둘 다 없음: 사진 없음(빈칸). local 판별은 backend 가 siteImageUrl 을 null 로 내려 결정한다.
          if (url) {
            return (
              <Image
                src={url}
                alt="현장사진"
                width={48}
                height={48}
                style={{ objectFit: 'cover', borderRadius: 4 }}
              />
            );
          }
          if (record.s3ImageUniqueKey) {
            return (
              <Tooltip title="로컬 환경에서는 현장사진 미리보기가 지원되지 않습니다 (운영/개발 환경에서 표시)">
                <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
              </Tooltip>
            );
          }
          return null;
        },
      },
      {
        // SF 확정(ScheduleConfirmed__c): 조원일정 연결(scheduleId != null) 시 ✔️, 미연결 시 ❌
        title: '확정',
        dataIndex: 'scheduleId',
        width: 70,
        align: 'center' as const,
        render: (v: number | null) =>
          v != null ? (
            <CheckCircleFilled style={{ color: '#52c41a' }} />
          ) : (
            <CloseCircleFilled style={{ color: '#ff4d4f' }} />
          ),
      },
      closeColumn,
  ];

  // --- 편집 모드 컬럼 ---
  const editColumns: ColumnsType<EditableRow> = [
      { title: 'NO.', dataIndex: 'id', width: 70, align: 'center' as const },
      {
        title: <span>행사사원<span style={{ color: '#fa8c16', marginLeft: 2 }}>**</span></span>,
        width: 220,
        render: (_: unknown, record: EditableRow) => {
          const options = employeeOptions.get(record.id) ?? [];
          const loading = employeeSearchLoading.get(record.id) ?? false;
          const errMsg = errorMessages.get(record.id);
          return (
            <Space size={4} style={{ width: '100%' }}>
              {errMsg && (
                <Tooltip title={errMsg}>
                  <ExclamationCircleOutlined style={{ color: '#ff4d4f', flexShrink: 0 }} />
                </Tooltip>
              )}
            <Select
              size="small"
              showSearch
              filterOption={false}
              placeholder="사원 검색"
              popupMatchSelectWidth={false}
              dropdownStyle={{ minWidth: 250 }}
              value={record.employeeId ? { value: record.employeeId, label: record.employeeName ? `${record.employeeName} (${record.employeeCode})` : (record.employeeCode ?? String(record.employeeId)) } : undefined}
              labelInValue
              loading={loading}
              onSearch={(val) => handleEmployeeSearch(record.id, val)}
              onChange={(opt: { value: number; label: string }) => {
                const selected = options.find((e) => e.id === opt.value);
                updateField(record.id, 'employeeId', opt.value);
                updateField(record.id, 'employeeCode', selected?.employeeCode ?? null);
                updateField(record.id, 'employeeName', selected?.name ?? opt.label);
              }}
              notFoundContent={loading ? <Spin size="small" /> : '검색 결과 없음'}
              style={{ width: '100%' }}
              allowClear
              onClear={() => {
                updateField(record.id, 'employeeId', null);
                updateField(record.id, 'employeeCode', null);
                updateField(record.id, 'employeeName', null);
              }}
            >
              {options.map((emp) => (
                <Select.Option key={emp.id} value={emp.id}>
                  {emp.name} ({emp.employeeCode})
                </Select.Option>
              ))}
            </Select>
            </Space>
          );
        },
      },
      {
        title: '전문행사조(현재)',
        dataIndex: 'currentProfessionalPromotionTeam',
        width: 130,
        align: 'center' as const,
        render: (v: string | null) =>
          v ? <Tag color={getPPTTeamTypeColor(v)}>{v}</Tag> : '-',
      },
      {
        title: '전문행사조(투입당시)',
        dataIndex: 'professionalPromotionTeam',
        width: 130,
        align: 'center' as const,
        render: (v: string | null) =>
          v ? <Tag color={getPPTTeamTypeColor(v)}>{v}</Tag> : '-',
      },
      {
        title: <span>투입일<span style={{ color: '#ff4d4f', marginLeft: 2 }}>*</span></span>,
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
        title: <span>근무유형3<span style={{ color: '#fa8c16', marginLeft: 2 }}>**</span></span>,
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
          if (record.basePrice != null && record.dailyTargetCount != null) {
            return fmtNum(record.basePrice * record.dailyTargetCount);
          }
          return '';
        },
      },
      {
        title: '대표품목 매출',
        dataIndex: 'primaryProductAmount',
        width: 110,
        onHeaderCell: () => ({ className: 'promo-emp-hl-pink' }),
        render: (_: unknown, record: EditableRow) => (
          <InputNumber
            size="small"
            min={0}
            value={record.primaryProductAmount}
            onChange={(v) => updateField(record.id, 'primaryProductAmount', v)}
            style={{ width: '100%' }}
          />
        ),
      },
      {
        title: '진도율',
        width: 70,
        align: 'right' as const,
        render: (_: unknown, record: EditableRow) => {
          const targetAmount = (record.basePrice != null && record.dailyTargetCount != null)
            ? record.basePrice * record.dailyTargetCount
            : null;
          if (!targetAmount || targetAmount === 0) return '0%';
          if (record.primaryProductAmount == null) return '0%';
          return `${Math.floor((record.primaryProductAmount / targetAmount) * 100)}%`;
        },
      },
      {
        title: '대표품목판매수량',
        dataIndex: 'primarySalesQuantity',
        width: 120,
        onHeaderCell: () => ({ className: 'promo-emp-hl-pink' }),
        render: (_: unknown, record: EditableRow) => (
          <InputNumber
            size="small"
            min={0}
            precision={0}
            value={record.primarySalesQuantity}
            onChange={(v) => updateField(record.id, 'primarySalesQuantity', v)}
            style={{ width: '100%' }}
          />
        ),
      },
      {
        title: '기타판매수량',
        dataIndex: 'otherSalesQuantity',
        width: 100,
        onHeaderCell: () => ({ className: 'promo-emp-hl-yellow' }),
        render: (_: unknown, record: EditableRow) => (
          <InputNumber
            size="small"
            min={0}
            precision={0}
            value={record.otherSalesQuantity}
            onChange={(v) => updateField(record.id, 'otherSalesQuantity', v)}
            style={{ width: '100%' }}
          />
        ),
      },
      {
        title: '기타판매금액',
        dataIndex: 'otherSalesAmount',
        width: 100,
        render: (_: unknown, record: EditableRow) => (
          <InputNumber
            size="small"
            min={0}
            value={record.otherSalesAmount}
            onChange={(v) => updateField(record.id, 'otherSalesAmount', v)}
            style={{ width: '100%' }}
          />
        ),
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
        dataIndex: 'siteImageUrl',
        width: 100,
        align: 'center' as const,
        render: (url: string | null, record: EditableRow) =>
          record.s3ImageUniqueKey ? (
            <Space size={4}>
              {url ? (
                <Image
                  src={url}
                  alt="현장사진"
                  width={40}
                  height={40}
                  style={{ objectFit: 'cover', borderRadius: 4 }}
                />
              ) : (
                // 로컬 환경: 미리보기 미지원 — 사진 존재만 안내 아이콘으로 표시
                <Tooltip title="로컬 환경에서는 현장사진 미리보기가 지원되지 않습니다 (운영/개발 환경에서 표시)">
                  <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                </Tooltip>
              )}
              <Button
                type="text"
                size="small"
                danger
                icon={<CloseOutlined />}
                onClick={() => {
                  // key 를 비우면 batch 저장 시 사진이 제거된다. 미리보기도 즉시 사라지도록 URL 도 함께 비운다.
                  updateField(record.id, 's3ImageUniqueKey', null);
                  updateField(record.id, 'siteImageUrl', null);
                }}
                style={{ minWidth: 0, padding: '0 4px' }}
              />
            </Space>
          ) : null,
      },
      {
        // SF 확정(ScheduleConfirmed__c): 조원일정 연결(scheduleId != null) 시 ✔️, 미연결 시 ❌ — 읽기 전용 파생값
        title: '확정',
        dataIndex: 'scheduleId',
        width: 70,
        align: 'center' as const,
        render: (v: number | null) =>
          v != null ? (
            <CheckCircleFilled style={{ color: '#52c41a' }} />
          ) : (
            <CloseCircleFilled style={{ color: '#ff4d4f' }} />
          ),
      },
      {
        // 여사원마감(true) 시 ✔️ + 마감 해제 버튼, 미마감(false) 시 ❌ (읽기 모드 closeColumn 과 동일 의미)
        title: '여사원마감',
        dataIndex: 'promoCloseByTm',
        width: 90,
        align: 'center' as const,
        render: (v: boolean, record: EditableRow) =>
          v ? (
            <Space size={4}>
              <CheckCircleFilled style={{ color: '#52c41a' }} />
              <Button
                type="text"
                size="small"
                danger
                icon={<CloseOutlined />}
                onClick={() => updateField(record.id, 'promoCloseByTm', false)}
                style={{ minWidth: 0, padding: '0 4px' }}
              />
            </Space>
          ) : (
            <CloseCircleFilled style={{ color: '#ff4d4f' }} />
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
  ];

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
        <Button type="link" onClick={() => navigate(listPath)}>
          ← 목록으로
        </Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          행사마스터를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const employeeCount = employees?.length ?? 0;
  const canEmpEdit = hasDates;
  const canConfirmSchedule = hasDates && employeeCount > 0;
  const empEditDisabledTooltip = !hasDates
    ? '행사 시작일과 종료일을 먼저 입력하세요'
    : employeeCount === 0
      ? '행사사원을 먼저 추가하세요'
      : '';
  const addDisabledTooltip = !hasDates ? '행사 시작일과 종료일을 먼저 입력하세요' : '';

  return (
    <div style={{ padding: '0 16px 16px 16px' }}>
      {/* 상단 헤더: 목록으로 + 수정/삭제 or 취소/저장 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Button type="link" onClick={() => navigate(listPath)} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        {promotionEditing ? (
          <Space>
            <span style={{ color: '#888', fontSize: 13 }}>
              <span style={{ color: '#ff4d4f', fontWeight: 'bold' }}>*</span> = 필수 정보
            </span>
            <Button onClick={cancelPromotionEdit}>취소</Button>
            <Button type="primary" onClick={savePromotionEdit} loading={updateMutation.isPending}>
              저장
            </Button>
          </Space>
        ) : canWrite ? (
          <Space>
            <Button onClick={enterPromotionEdit}>수정</Button>
            <Button onClick={handleClone}>복제</Button>
            <Button
              onClick={handleCloneWithChildren}
              loading={cloneWithChildrenMutation.isPending}
            >
              자식 포함 복제
            </Button>
            <Button danger onClick={handleDelete}>
              삭제
            </Button>
          </Space>
        ) : null}
      </div>

      {/* 세부 사항 / 행사품목 / 목표·실적 섹션: Collapse */}
      <Collapse
        className="promotion-detail-collapse"
        defaultActiveKey={['detail', 'product', 'targetActual']}
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
            key: 'targetActual',
            label: '목표/실적',
            children: (
              <PromotionTargetActualSection promotion={promotion} editing={promotionEditing} />
            ),
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
          <Space>
            <Title level={5} style={{ margin: 0 }}>
              행사사원
            </Title>
            <Popover
              trigger="click"
              content={
                <div style={{ fontSize: 13 }}>
                  <div><span style={{ color: '#ff4d4f', fontWeight: 'bold' }}>*</span> 저장시 필수</div>
                  <div><span style={{ color: '#fa8c16', fontWeight: 'bold' }}>**</span> 일정확정시 필수</div>
                  <div>
                    <span style={{ display: 'inline-block', width: 12, height: 12, backgroundColor: '#e6f4ff', border: '1px solid #91caff', verticalAlign: 'middle', marginRight: 4 }} />
                    일정이 확정된 행
                  </div>
                  <Divider style={{ margin: '8px 0' }} />
                  <div style={{ fontWeight: 'bold', marginBottom: 4 }}>근무유형3 출근 등록 가능 조건</div>
                  <div style={{ color: '#888', marginBottom: 4 }}>같은 사원·같은 날짜 기준</div>
                  <ul style={{ paddingLeft: 18, margin: 0 }}>
                    <li><b>고정</b>: 그 날 다른 일정(고정/격고/순회)이 없어야 등록 가능</li>
                    <li><b>격고</b>: 같은 날 최대 2건까지 가능. 단, 고정이 있거나 순회가 함께 있으면 불가</li>
                    <li><b>순회</b>: 고정이 있거나 격고가 2건이면 불가</li>
                  </ul>
                </div>
              }
            >
              <InfoCircleOutlined style={{ color: '#999', cursor: 'pointer' }} />
            </Popover>
          </Space>
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
              <Tooltip title={!canConfirmSchedule ? empEditDisabledTooltip : ''}>
                <Button
                  onClick={handleConfirmSchedule}
                  disabled={!canConfirmSchedule}
                  loading={confirmMutation.isPending}
                >
                  일정 확정
                </Button>
              </Tooltip>
              <Tooltip title={!canEmpEdit ? addDisabledTooltip : ''}>
                <Button onClick={enterEmpEditMode} disabled={!canEmpEdit}>
                  편집
                </Button>
              </Tooltip>
            </Space>
          )}
        </div>

        {empEditMode ? (
          <ResizableTable<EditableRow>
            className="promo-emp-table"
            bordered
            columns={editColumns}
            dataSource={editRows}
            rowKey="id"
            size="small"
            pagination={false}
            locale={{ emptyText: '등록된 행사사원이 없습니다' }}
            footer={() => (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>총 {editRows.length}건</span>
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
            rowClassName={(record) => {
              const classes: string[] = [];
              if (errorRowIds.has(record.id)) classes.push('ant-table-row-error');
              if (record.scheduleId != null) classes.push('ant-table-row-confirmed');
              return classes.join(' ');
            }}
            onRow={(record) => ({
              title: errorMessages.get(record.id) || undefined,
            })}
            scroll={{ x: 2100 }}
          />
        ) : (
          <ResizableTable<PromotionEmployee>
            className="promo-emp-table"
            bordered
            columns={readColumns}
            dataSource={employees ?? []}
            rowKey="id"
            size="small"
            pagination={false}
            loading={employeesLoading}
            locale={{ emptyText: '등록된 행사사원이 없습니다' }}
            rowClassName={(record) =>
              record.scheduleId != null ? 'ant-table-row-confirmed' : ''
            }
            footer={() => `총 ${employees?.length ?? 0}건`}
            scroll={{ x: 1800 }}
          />
        )}
      </div>

      {/* 상세 POS품목 섹션 — SF Promotion 상세의 "상세 POS품목" Related List 동등 */}
      <PromotionPosProductSection promotionId={promotionId} />


      {/* Scoped styles for promotion detail page */}
      <style>{`
        .promo-emp-table .ant-table-thead th {
          font-size: 12px !important;
        }
        /* SF 행사사원 related list 동등 — 컬럼 헤더 강조색 (대표품목 매출/판매수량 분홍, 기타판매수량 노랑) */
        .promo-emp-table .ant-table-thead th.promo-emp-hl-pink {
          background-color: #fde0e4 !important;
        }
        .promo-emp-table .ant-table-thead th.promo-emp-hl-yellow {
          background-color: #fdf3a3 !important;
        }
        .ant-table-row-error td {
          background-color: #fff2f0 !important;
        }
        .ant-table-row-confirmed td {
          background-color: #e6f4ff !important;
        }
        .promotion-detail-collapse .ant-collapse-content-box {
          padding: 0 !important;
        }
        .promotion-detail-collapse .ant-descriptions-view {
          table-layout: fixed;
        }
        .promotion-detail-collapse .ant-descriptions-view col {
          width: 25%;
        }
      `}</style>
    </div>
  );
}
