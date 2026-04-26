import '../../domain/entities/order_draft.dart';
import '../../domain/entities/validation_error.dart';

/// 주문서 작성 화면 상태
///
/// 작성 중인 주문서, 거래처 목록, 유효성 검증 결과, 로딩/에러 상태를 포함합니다.
class OrderFormState {
  /// 작성 중인 주문서
  final OrderDraft orderDraft;

  /// 거래처 목록 (id → name) for dropdown
  final Map<int, String> clients;

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

  const OrderFormState({
    required this.orderDraft,
    this.clients = const {},
    this.hasDraft = false,
    this.isLoading = false,
    this.isSubmitting = false,
    this.errorMessage,
    this.successMessage,
    this.validationErrors = const {},
    this.submitResult,
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
    Map<int, String>? clients,
    bool? hasDraft,
    bool? isLoading,
    bool? isSubmitting,
    String? errorMessage,
    String? successMessage,
    Map<String, ValidationError>? validationErrors,
    OrderSubmitResult? submitResult,
    bool clearError = false,
    bool clearSuccess = false,
    bool clearValidationErrors = false,
    bool clearSubmitResult = false,
  }) {
    return OrderFormState(
      orderDraft: orderDraft ?? this.orderDraft,
      clients: clients ?? this.clients,
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
    );
  }
}
