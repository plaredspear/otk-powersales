/// 소비기한 등록/수정 폼 상태
///
/// 등록과 수정 화면에서 공유하는 폼 상태 클래스입니다.
/// 등록 시에는 거래처/제품 선택이 필요하고, 수정 시에는 읽기 전용으로 표시됩니다.
class ProductExpirationFormState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  // ── 거래처 정보 ──
  /// 선택된 거래처 코드
  final String? selectedAccountCode;

  /// 선택된 거래처명
  final String? selectedAccountName;

  // ── 제품 정보 ──
  /// 선택된 제품 코드
  final String? selectedProductCode;

  /// 선택된 제품명
  final String? selectedProductName;

  // ── 날짜 정보 ──
  /// 소비기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  // ── 설명 ──
  /// 설명 텍스트
  final String description;

  // ── 수정 모드 전용 ──
  /// 수정 대상 소비기한 시퀀스 (null이면 등록 모드)
  final int? editSeq;

  /// 수정 모드 진입 시점의 원본 소비기한 (변경 여부 판별용)
  final DateTime? originalExpiryDate;

  /// 수정 모드 진입 시점의 원본 알림일 (변경 여부 판별용)
  final DateTime? originalAlertDate;

  /// 수정 모드 진입 시점의 원본 설명 (변경 여부 판별용)
  final String? originalDescription;

  // ── 결과 상태 ──
  /// 저장 완료 여부 (등록 또는 수정 완료)
  final bool isSaved;

  /// 삭제 완료 여부 (수정 화면에서 삭제)
  final bool isDeleted;

  const ProductExpirationFormState({
    required this.isLoading,
    this.errorMessage,
    this.selectedAccountCode,
    this.selectedAccountName,
    this.selectedProductCode,
    this.selectedProductName,
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
    this.editSeq,
    this.originalExpiryDate,
    this.originalAlertDate,
    this.originalDescription,
    this.isSaved = false,
    this.isDeleted = false,
  });

  /// 등록 모드 초기 상태
  factory ProductExpirationFormState.initial() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final defaultExpiry = today.add(const Duration(days: 2));
    final defaultAlert = defaultExpiry.subtract(const Duration(days: 1));

    return ProductExpirationFormState(
      isLoading: false,
      expiryDate: defaultExpiry,
      alertDate: defaultAlert,
    );
  }

  // ── Computed Getters ──

  /// 등록 모드 여부
  bool get isRegisterMode => editSeq == null;

  /// 수정 모드 여부
  bool get isEditMode => editSeq != null;

  /// 거래처가 선택되었는지
  bool get hasAccount => selectedAccountCode != null && selectedAccountCode!.isNotEmpty;

  /// 제품이 선택되었는지
  bool get hasProduct =>
      selectedProductCode != null && selectedProductCode!.isNotEmpty;

  /// 폼 유효성 (등록 모드: 거래처 + 제품 필수, 수정 모드: 항상 유효)
  bool get isValid {
    if (isRegisterMode) {
      return hasAccount && hasProduct;
    }
    return true; // 수정 모드는 소비기한/알림이 이미 설정됨
  }

  /// 수정 모드에서 원본 대비 변경 여부
  ///
  /// 소비기한/알림일/설명 중 하나라도 진입 시점과 달라지면 true.
  /// 날짜는 시/분 성분을 무시하고 연·월·일 단위로만 비교한다
  /// (DatePicker 는 시간 성분이 0인 날짜만 반환하므로 원본에 시간이
  /// 섞여 있으면 같은 날 재선택 시에도 오탐이 날 수 있다).
  bool get isDirty {
    if (isRegisterMode) return true;
    return !_isSameDay(expiryDate, originalExpiryDate) ||
        !_isSameDay(alertDate, originalAlertDate) ||
        description != (originalDescription ?? '');
  }

  bool _isSameDay(DateTime? a, DateTime? b) {
    if (a == null || b == null) return a == b;
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  /// 저장 버튼 활성화 여부
  ///
  /// 수정 모드는 변경 사항이 있을 때만 활성화한다.
  bool get canSave => isValid && isDirty && !isLoading;

  // ── State Transition Helpers ──

  /// 로딩 상태로 전환
  ProductExpirationFormState toLoading() {
    return copyWith(isLoading: true, clearError: true);
  }

  /// 에러 상태로 전환
  ProductExpirationFormState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  // ── copyWith ──

  ProductExpirationFormState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    String? selectedAccountCode,
    String? selectedAccountName,
    bool clearAccount = false,
    String? selectedProductCode,
    String? selectedProductName,
    bool clearProduct = false,
    DateTime? expiryDate,
    DateTime? alertDate,
    String? description,
    int? editSeq,
    DateTime? originalExpiryDate,
    DateTime? originalAlertDate,
    String? originalDescription,
    bool? isSaved,
    bool? isDeleted,
  }) {
    return ProductExpirationFormState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      selectedAccountCode:
          clearAccount ? null : (selectedAccountCode ?? this.selectedAccountCode),
      selectedAccountName:
          clearAccount ? null : (selectedAccountName ?? this.selectedAccountName),
      selectedProductCode:
          clearProduct ? null : (selectedProductCode ?? this.selectedProductCode),
      selectedProductName:
          clearProduct ? null : (selectedProductName ?? this.selectedProductName),
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      description: description ?? this.description,
      editSeq: editSeq ?? this.editSeq,
      originalExpiryDate: originalExpiryDate ?? this.originalExpiryDate,
      originalAlertDate: originalAlertDate ?? this.originalAlertDate,
      originalDescription: originalDescription ?? this.originalDescription,
      isSaved: isSaved ?? this.isSaved,
      isDeleted: isDeleted ?? this.isDeleted,
    );
  }
}
