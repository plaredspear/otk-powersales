import '../../domain/entities/staff_evaluation.dart';

/// 여사원 평가조회 화면 상태
///
/// 조회 연월, 평가 데이터, 로딩/에러 상태를 포함한다.
class StaffEvaluationState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 조회 연월 (YYYYMM)
  final String yearMonth;

  /// 평가 데이터
  final StaffEvaluation? evaluation;

  const StaffEvaluationState({
    this.isLoading = false,
    this.errorMessage,
    required this.yearMonth,
    this.evaluation,
  });

  /// 초기 상태 — 레거시 `evaluationList` 기본 조회월(전월) 정합.
  factory StaffEvaluationState.initial() {
    final now = DateTime.now();
    final prev = DateTime(now.year, now.month - 1, 1);
    final yearMonth = '${prev.year}${prev.month.toString().padLeft(2, '0')}';
    return StaffEvaluationState(yearMonth: yearMonth);
  }

  StaffEvaluationState toLoading() =>
      copyWith(isLoading: true, errorMessage: null);

  StaffEvaluationState toError(String message) =>
      copyWith(isLoading: false, errorMessage: message);

  bool get hasData => evaluation != null;

  /// 이전 월로 이동 가능한지 (3년 전까지)
  bool get canGoToPreviousMonth {
    final current = DateTime.parse('${yearMonth}01');
    final threeYearsAgo = DateTime.now().subtract(const Duration(days: 365 * 3));
    return current.isAfter(threeYearsAgo);
  }

  /// 다음 월로 이동 가능한지 (미래는 조회 불가, 당월까지)
  bool get canGoToNextMonth {
    final current = DateTime.parse('${yearMonth}01');
    final now = DateTime.now();
    return current.isBefore(DateTime(now.year, now.month, 1));
  }

  String getPreviousMonth() {
    final current = DateTime.parse('${yearMonth}01');
    final previous = DateTime(current.year, current.month - 1, 1);
    return '${previous.year}${previous.month.toString().padLeft(2, '0')}';
  }

  String getNextMonth() {
    final current = DateTime.parse('${yearMonth}01');
    final next = DateTime(current.year, current.month + 1, 1);
    return '${next.year}${next.month.toString().padLeft(2, '0')}';
  }

  /// 연월 표시 형식 (예: "2026년 05월")
  String get displayYearMonth {
    final year = yearMonth.substring(0, 4);
    final month = yearMonth.substring(4, 6);
    return '$year년 $month월';
  }

  StaffEvaluationState copyWith({
    bool? isLoading,
    String? errorMessage,
    String? yearMonth,
    StaffEvaluation? evaluation,
  }) {
    return StaffEvaluationState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      yearMonth: yearMonth ?? this.yearMonth,
      evaluation: evaluation ?? this.evaluation,
    );
  }
}
