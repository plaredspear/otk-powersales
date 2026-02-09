import '../../domain/entities/safety_check_category.dart';

/// 안전점검 화면 상태
///
/// 안전점검 화면의 데이터 로딩, 체크 상태, 제출 상태를 관리한다.
/// - initial: 데이터 요청 전 초기 상태
/// - loading: 체크리스트 항목 API 호출 중
/// - loaded: 체크리스트 항목 로딩 완료
/// - submitting: 제출 API 호출 중
/// - submitted: 제출 완료
/// - error: API 호출 실패
class SafetyCheckState {
  /// 카테고리 목록 (loaded 상태에서만 값이 있음)
  final List<SafetyCheckCategory>? categories;

  /// 각 항목의 체크 상태 (항목 ID → 체크 여부)
  final Map<int, bool> checkedItems;

  /// 로딩 상태
  final bool isLoading;

  /// 제출 중 상태
  final bool isSubmitting;

  /// 제출 완료 여부
  final bool isSubmitted;

  /// 에러 메시지 (error 상태에서만 값이 있음)
  final String? errorMessage;

  const SafetyCheckState({
    this.categories,
    this.checkedItems = const {},
    this.isLoading = false,
    this.isSubmitting = false,
    this.isSubmitted = false,
    this.errorMessage,
  });

  /// 초기 상태
  factory SafetyCheckState.initial() {
    return const SafetyCheckState();
  }

  /// 로딩 상태로 전환
  SafetyCheckState toLoading() {
    return SafetyCheckState(
      categories: categories,
      checkedItems: checkedItems,
      isLoading: true,
      isSubmitting: false,
      isSubmitted: false,
      errorMessage: null,
    );
  }

  /// 데이터 로딩 완료 상태로 전환
  SafetyCheckState toLoaded(List<SafetyCheckCategory> data) {
    // 모든 항목을 unchecked로 초기화
    final initialChecks = <int, bool>{};
    for (final category in data) {
      for (final item in category.items) {
        initialChecks[item.id] = false;
      }
    }

    return SafetyCheckState(
      categories: data,
      checkedItems: initialChecks,
      isLoading: false,
      isSubmitting: false,
      isSubmitted: false,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  SafetyCheckState toError(String message) {
    return SafetyCheckState(
      categories: categories,
      checkedItems: checkedItems,
      isLoading: false,
      isSubmitting: false,
      isSubmitted: false,
      errorMessage: message,
    );
  }

  /// 제출 중 상태로 전환
  SafetyCheckState toSubmitting() {
    return SafetyCheckState(
      categories: categories,
      checkedItems: checkedItems,
      isLoading: false,
      isSubmitting: true,
      isSubmitted: false,
      errorMessage: null,
    );
  }

  /// 제출 완료 상태로 전환
  SafetyCheckState toSubmitted() {
    return SafetyCheckState(
      categories: categories,
      checkedItems: checkedItems,
      isLoading: false,
      isSubmitting: false,
      isSubmitted: true,
      errorMessage: null,
    );
  }

  /// 항목 체크 상태 토글
  SafetyCheckState toggleItem(int itemId) {
    final newCheckedItems = Map<int, bool>.from(checkedItems);
    newCheckedItems[itemId] = !(newCheckedItems[itemId] ?? false);

    return SafetyCheckState(
      categories: categories,
      checkedItems: newCheckedItems,
      isLoading: isLoading,
      isSubmitting: isSubmitting,
      isSubmitted: isSubmitted,
      errorMessage: errorMessage,
    );
  }

  /// 데이터 로딩 완료 여부
  bool get isLoaded =>
      categories != null && !isLoading && !isSubmitting && errorMessage == null;

  /// 에러 상태 여부
  bool get isError => errorMessage != null;

  /// 모든 필수 항목이 체크되었는지 확인
  bool get allRequiredChecked {
    if (categories == null) return false;

    for (final category in categories!) {
      for (final item in category.items) {
        if (item.required && !(checkedItems[item.id] ?? false)) {
          return false;
        }
      }
    }
    return true;
  }

  /// 체크된 항목 ID 목록
  List<int> get checkedItemIds {
    return checkedItems.entries
        .where((entry) => entry.value)
        .map((entry) => entry.key)
        .toList();
  }

  SafetyCheckState copyWith({
    List<SafetyCheckCategory>? categories,
    Map<int, bool>? checkedItems,
    bool? isLoading,
    bool? isSubmitting,
    bool? isSubmitted,
    String? errorMessage,
  }) {
    return SafetyCheckState(
      categories: categories ?? this.categories,
      checkedItems: checkedItems ?? this.checkedItems,
      isLoading: isLoading ?? this.isLoading,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      isSubmitted: isSubmitted ?? this.isSubmitted,
      errorMessage: errorMessage,
    );
  }
}
