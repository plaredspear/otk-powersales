/// 유통기한 등록/수정 폼 상태
///
/// 등록과 수정 화면에서 공유하는 폼 상태 클래스입니다.
/// 등록 시에는 거래처/제품 선택이 필요하고, 수정 시에는 읽기 전용으로 표시됩니다.
class ShelfLifeFormState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  // ── 거래처 정보 ──
  /// 선택된 거래처 ID
  final int? selectedStoreId;

  /// 선택된 거래처명
  final String? selectedStoreName;

  // ── 제품 정보 ──
  /// 선택된 제품 코드
  final String? selectedProductCode;

  /// 선택된 제품명
  final String? selectedProductName;

  // ── 날짜 정보 ──
  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  // ── 설명 ──
  /// 설명 텍스트
  final String description;

  // ── 수정 모드 전용 ──
  /// 수정 대상 유통기한 ID (null이면 등록 모드)
  final int? editId;

  // ── 결과 상태 ──
  /// 저장 완료 여부 (등록 또는 수정 완료)
  final bool isSaved;

  /// 삭제 완료 여부 (수정 화면에서 삭제)
  final bool isDeleted;

  /// 거래처 목록 (드롭다운용)
  final Map<int, String> stores;

  const ShelfLifeFormState({
    required this.isLoading,
    this.errorMessage,
    this.selectedStoreId,
    this.selectedStoreName,
    this.selectedProductCode,
    this.selectedProductName,
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
    this.editId,
    this.isSaved = false,
    this.isDeleted = false,
    this.stores = const {},
  });

  /// 등록 모드 초기 상태
  factory ShelfLifeFormState.initial() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final defaultExpiry = today.add(const Duration(days: 2));
    final defaultAlert = defaultExpiry.subtract(const Duration(days: 1));

    return ShelfLifeFormState(
      isLoading: false,
      expiryDate: defaultExpiry,
      alertDate: defaultAlert,
    );
  }

  // ── Computed Getters ──

  /// 등록 모드 여부
  bool get isRegisterMode => editId == null;

  /// 수정 모드 여부
  bool get isEditMode => editId != null;

  /// 거래처가 선택되었는지
  bool get hasStore => selectedStoreId != null;

  /// 제품이 선택되었는지
  bool get hasProduct =>
      selectedProductCode != null && selectedProductCode!.isNotEmpty;

  /// 폼 유효성 (등록 모드: 거래처 + 제품 필수, 수정 모드: 항상 유효)
  bool get isValid {
    if (isRegisterMode) {
      return hasStore && hasProduct;
    }
    return true; // 수정 모드는 유통기한/알림이 이미 설정됨
  }

  /// 저장 버튼 활성화 여부
  bool get canSave => isValid && !isLoading;

  // ── State Transition Helpers ──

  /// 로딩 상태로 전환
  ShelfLifeFormState toLoading() {
    return copyWith(isLoading: true, clearError: true);
  }

  /// 에러 상태로 전환
  ShelfLifeFormState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  // ── copyWith ──

  ShelfLifeFormState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    int? selectedStoreId,
    String? selectedStoreName,
    bool clearStore = false,
    String? selectedProductCode,
    String? selectedProductName,
    bool clearProduct = false,
    DateTime? expiryDate,
    DateTime? alertDate,
    String? description,
    int? editId,
    bool? isSaved,
    bool? isDeleted,
    Map<int, String>? stores,
  }) {
    return ShelfLifeFormState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      selectedStoreId:
          clearStore ? null : (selectedStoreId ?? this.selectedStoreId),
      selectedStoreName:
          clearStore ? null : (selectedStoreName ?? this.selectedStoreName),
      selectedProductCode:
          clearProduct ? null : (selectedProductCode ?? this.selectedProductCode),
      selectedProductName:
          clearProduct ? null : (selectedProductName ?? this.selectedProductName),
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      description: description ?? this.description,
      editId: editId ?? this.editId,
      isSaved: isSaved ?? this.isSaved,
      isDeleted: isDeleted ?? this.isDeleted,
      stores: stores ?? this.stores,
    );
  }
}
