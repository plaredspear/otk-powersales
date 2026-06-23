import '../../data/models/order_form/order_draft_response_model.dart';
import '../../domain/entities/order_draft.dart';
import '../../domain/entities/validation_error.dart';

/// 주문서 작성 화면 상태
///
/// 작성 중인 주문서, 거래처 목록, 유효성 검증 결과, 로딩/에러 상태를 포함합니다.
class OrderFormState {
  /// 작성 중인 주문서
  final OrderDraft orderDraft;

  /// 임시저장 데이터 존재 여부
  final bool hasDraft;

  /// 일반 로딩 상태 (데이터 조회)
  final bool isLoading;

  /// 전송 중 여부 (승인요청/임시저장)
  final bool isSubmitting;

  /// 에러 메시지
  final String? errorMessage;

  /// 성공 메시지 (snackbar 표시용)
  final String? successMessage;

  /// 제품별 유효성 에러 (productCode → ValidationError)
  final Map<String, ValidationError> validationErrors;

  /// 전송 결과 (성공 시)
  final OrderSubmitResult? submitResult;

  // ─── Spec #598 P1-M: 신규 백엔드 연결 필드 ─────────────────────────
  // (P2-M / P3-M 가 활용 — P1-M 단독 머지 시점에는 화면 동작 변경 없음)

  /// 멱등키 (UUID v4) — 신규 폼 진입 시 발급, 등록 200 OK 후 폐기.
  /// `OrderFormRepository.submitOrderRequest` 호출 시 헤더 `Idempotency-Key` 또는 본문 `clientRequestId` 로 송신.
  final String? clientRequestId;

  /// 임시저장 PK (`tmp_order.tmp_order_id`) — 임시저장 삭제 시 사용.
  final int? draftId;

  /// 거래처 SAP 코드 (Account.externalKey) — `getLoanInquiry` 호출 입력.
  final String? selectedExternalKey;

  /// 거래처 PK — `submitOrderRequest` 페이로드 `accountId`.
  final int? selectedAccountId;

  /// `getOrderDraft` 응답 임시 보관 — 사용자가 다이얼로그 "예"/"아니오" 선택 후 사용.
  final OrderDraftResponseModel? pendingDraft;

  /// 납기일 +10일 (Spec #598 P3-M (I)) 다이얼로그 표시 트리거.
  /// validateAndSubmitOrder 가 (A)~(H) 통과 + (I) 충족 시 true 로 set.
  /// UI 가 listener 로 다이얼로그 표시 후 confirmDeliveryDateAndSubmit 또는 cancelDeliveryDateConfirm 호출.
  final bool requiresDeliveryDateConfirm;

  /// 승인요청 확인 다이얼로그 표시 트리거.
  /// validateAndSubmitOrder 가 (A)~(H) 통과 시 true 로 set.
  /// UI 가 listener 로 "승인요청 하시겠습니까?" 다이얼로그 표시 후
  /// confirmSubmit 또는 cancelSubmitConfirm 호출.
  final bool requiresSubmitConfirm;

  const OrderFormState({
    required this.orderDraft,
    this.hasDraft = false,
    this.isLoading = false,
    this.isSubmitting = false,
    this.errorMessage,
    this.successMessage,
    this.validationErrors = const {},
    this.submitResult,
    this.clientRequestId,
    this.draftId,
    this.selectedExternalKey,
    this.selectedAccountId,
    this.pendingDraft,
    this.requiresDeliveryDateConfirm = false,
    this.requiresSubmitConfirm = false,
  });

  /// 초기 상태
  factory OrderFormState.initial() {
    return OrderFormState(
      orderDraft: OrderDraft.empty(),
    );
  }

  /// 로딩 상태로 전환
  OrderFormState toLoading() {
    return copyWith(
      isLoading: true,
      clearError: true,
    );
  }

  /// 에러 상태로 전환
  OrderFormState toError(String message) {
    return copyWith(
      isLoading: false,
      isSubmitting: false,
      errorMessage: message,
    );
  }

  // --- Computed Getters ---

  /// 총 주문금액
  int get totalAmount => orderDraft.calculatedTotalAmount;

  /// 선택된 거래처 ID
  int? get selectedClientId => orderDraft.clientId;

  /// 선택된 거래처명 (거래처 선택 필드 표시용)
  String? get selectedClientName => orderDraft.clientName;

  /// 여신 잔액
  int? get creditBalance => orderDraft.creditBalance;

  /// 납기일
  DateTime? get deliveryDate => orderDraft.deliveryDate;

  /// 제품 목록
  List<OrderDraftItem> get items => orderDraft.items;

  /// 선택된 제품 목록
  List<OrderDraftItem> get selectedItems => orderDraft.selectedItems;

  /// 제품이 있는지 여부
  bool get hasItems => orderDraft.items.isNotEmpty;

  /// 여신 한도 초과 여부 (레거시 write.jsp:188 `loan < total` 차단 조건).
  /// 여신 잔액이 조회된 경우에만 판정한다(null 이면 미초과로 간주).
  bool get isLoanExceeded =>
      creditBalance != null && totalAmount > creditBalance!;

  /// 모든 제품이 선택되었는지 여부
  bool get allItemsSelected => orderDraft.allItemsSelected;

  /// 필수 입력이 완료되었는지 여부
  bool get isRequiredFieldsFilled => orderDraft.isRequiredFieldsFilled;

  /// 유효성 에러가 있는지 여부
  bool get hasValidationErrors => validationErrors.isNotEmpty;

  /// 수정 모드인지 여부
  bool get isEditMode => orderDraft.id != null;

  OrderFormState copyWith({
    OrderDraft? orderDraft,
    bool? hasDraft,
    bool? isLoading,
    bool? isSubmitting,
    String? errorMessage,
    String? successMessage,
    Map<String, ValidationError>? validationErrors,
    OrderSubmitResult? submitResult,
    String? clientRequestId,
    int? draftId,
    String? selectedExternalKey,
    int? selectedAccountId,
    OrderDraftResponseModel? pendingDraft,
    bool? requiresDeliveryDateConfirm,
    bool? requiresSubmitConfirm,
    bool clearError = false,
    bool clearSuccess = false,
    bool clearValidationErrors = false,
    bool clearSubmitResult = false,
    bool clearClientRequestId = false,
    bool clearDraftId = false,
    bool clearSelectedExternalKey = false,
    bool clearSelectedAccountId = false,
    bool clearPendingDraft = false,
    bool clearRequiresDeliveryDateConfirm = false,
    bool clearRequiresSubmitConfirm = false,
  }) {
    return OrderFormState(
      orderDraft: orderDraft ?? this.orderDraft,
      hasDraft: hasDraft ?? this.hasDraft,
      isLoading: isLoading ?? this.isLoading,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      successMessage:
          clearSuccess ? null : (successMessage ?? this.successMessage),
      validationErrors: clearValidationErrors
          ? const {}
          : (validationErrors ?? this.validationErrors),
      submitResult:
          clearSubmitResult ? null : (submitResult ?? this.submitResult),
      clientRequestId: clearClientRequestId
          ? null
          : (clientRequestId ?? this.clientRequestId),
      draftId: clearDraftId ? null : (draftId ?? this.draftId),
      selectedExternalKey: clearSelectedExternalKey
          ? null
          : (selectedExternalKey ?? this.selectedExternalKey),
      selectedAccountId: clearSelectedAccountId
          ? null
          : (selectedAccountId ?? this.selectedAccountId),
      pendingDraft:
          clearPendingDraft ? null : (pendingDraft ?? this.pendingDraft),
      requiresDeliveryDateConfirm: clearRequiresDeliveryDateConfirm
          ? false
          : (requiresDeliveryDateConfirm ?? this.requiresDeliveryDateConfirm),
      requiresSubmitConfirm: clearRequiresSubmitConfirm
          ? false
          : (requiresSubmitConfirm ?? this.requiresSubmitConfirm),
    );
  }
}
