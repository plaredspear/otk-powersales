import '../../domain/entities/safety_check_category.dart';

/// 안전점검 화면 상태 (V1: 라디오 + 체크박스 2섹션)
class SafetyCheckState {
  final List<SafetyCheckCategory>? categories;

  /// 섹션 1: 장비 라디오 응답 (seqNum → "예"/"해당없음")
  final Map<int, String> equipmentAnswers;

  /// 섹션 2: 예방사항 체크박스 (seqNum → checked)
  final Map<int, bool> precautionChecks;

  /// 현재 펼쳐진 질문1 항목의 seqNum (null이면 모두 접힘)
  final int? expandedItemIndex;

  /// 화면 진입 시각
  final DateTime? startTime;

  final bool isLoading;
  final bool isSubmitting;
  final bool isSubmitted;
  final String? errorMessage;

  const SafetyCheckState({
    this.categories,
    this.equipmentAnswers = const {},
    this.precautionChecks = const {},
    this.expandedItemIndex,
    this.startTime,
    this.isLoading = false,
    this.isSubmitting = false,
    this.isSubmitted = false,
    this.errorMessage,
  });

  factory SafetyCheckState.initial() {
    return SafetyCheckState(startTime: DateTime.now());
  }

  SafetyCheckState toLoading() {
    return SafetyCheckState(
      categories: categories,
      equipmentAnswers: equipmentAnswers,
      precautionChecks: precautionChecks,
      startTime: startTime,
      isLoading: true,
    );
  }

  SafetyCheckState toLoaded(List<SafetyCheckCategory> data) {
    return SafetyCheckState(
      categories: data,
      equipmentAnswers: const {},
      precautionChecks: const {},
      startTime: startTime,
    );
  }

  SafetyCheckState toError(String message) {
    return SafetyCheckState(
      categories: categories,
      equipmentAnswers: equipmentAnswers,
      precautionChecks: precautionChecks,
      startTime: startTime,
      errorMessage: message,
    );
  }

  SafetyCheckState toSubmitting() {
    return SafetyCheckState(
      categories: categories,
      equipmentAnswers: equipmentAnswers,
      precautionChecks: precautionChecks,
      startTime: startTime,
      isSubmitting: true,
    );
  }

  SafetyCheckState toSubmitted() {
    return SafetyCheckState(
      categories: categories,
      equipmentAnswers: equipmentAnswers,
      precautionChecks: precautionChecks,
      startTime: startTime,
      isSubmitted: true,
    );
  }

  /// 섹션 1: 장비 라디오 응답 설정
  SafetyCheckState setEquipmentAnswer(int seqNum, String answer) {
    final newAnswers = Map<int, String>.from(equipmentAnswers);
    newAnswers[seqNum] = answer;
    return SafetyCheckState(
      categories: categories,
      equipmentAnswers: newAnswers,
      precautionChecks: precautionChecks,
      expandedItemIndex: expandedItemIndex,
      startTime: startTime,
      isLoading: isLoading,
      isSubmitting: isSubmitting,
      isSubmitted: isSubmitted,
      errorMessage: errorMessage,
    );
  }

  /// 섹션 2: 예방사항 체크박스 토글
  SafetyCheckState togglePrecaution(int seqNum) {
    final newChecks = Map<int, bool>.from(precautionChecks);
    newChecks[seqNum] = !(newChecks[seqNum] ?? false);
    return SafetyCheckState(
      categories: categories,
      equipmentAnswers: equipmentAnswers,
      precautionChecks: newChecks,
      expandedItemIndex: expandedItemIndex,
      startTime: startTime,
      isLoading: isLoading,
      isSubmitting: isSubmitting,
      isSubmitted: isSubmitted,
      errorMessage: errorMessage,
    );
  }

  bool get isLoaded =>
      categories != null && !isLoading && !isSubmitting && errorMessage == null;

  bool get isError => errorMessage != null;

  /// 섹션 1(RADIO, required)의 모든 항목에 응답했는지 확인
  bool get allRequiredChecked {
    if (categories == null) return false;
    for (final category in categories!) {
      if (category.required && category.inputType == 'RADIO') {
        for (final item in category.items) {
          if (!equipmentAnswers.containsKey(item.seqNum)) return false;
        }
      }
    }
    return true;
  }

  SafetyCheckState copyWith({
    List<SafetyCheckCategory>? categories,
    Map<int, String>? equipmentAnswers,
    Map<int, bool>? precautionChecks,
    int? expandedItemIndex,
    bool clearExpandedItemIndex = false,
    DateTime? startTime,
    bool? isLoading,
    bool? isSubmitting,
    bool? isSubmitted,
    String? errorMessage,
  }) {
    return SafetyCheckState(
      categories: categories ?? this.categories,
      equipmentAnswers: equipmentAnswers ?? this.equipmentAnswers,
      precautionChecks: precautionChecks ?? this.precautionChecks,
      expandedItemIndex: clearExpandedItemIndex
          ? null
          : (expandedItemIndex ?? this.expandedItemIndex),
      startTime: startTime ?? this.startTime,
      isLoading: isLoading ?? this.isLoading,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      isSubmitted: isSubmitted ?? this.isSubmitted,
      errorMessage: errorMessage,
    );
  }
}
