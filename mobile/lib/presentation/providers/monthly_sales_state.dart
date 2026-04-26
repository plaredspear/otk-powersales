import '../../domain/entities/monthly_sales.dart';

/// 월매출 화면 상태
///
/// 거래처, 연월 선택, 월매출 통계, 로딩/에러 상태를 포함합니다.
class MonthlySalesState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 선택된 거래처 ID (null이면 전체)
  final String? selectedCustomerId;

  /// 조회 연월 (YYYYMM 형식)
  final String yearMonth;

  /// 월매출 통계 데이터
  final MonthlySales? monthlySales;

  const MonthlySalesState({
    this.isLoading = false,
    this.errorMessage,
    this.selectedCustomerId,
    required this.yearMonth,
    this.monthlySales,
  });

  /// 초기 상태 (이전 달)
  factory MonthlySalesState.initial() {
    final now = DateTime.now();
    final previousMonth = DateTime(now.year, now.month - 1, 1);
    final yearMonth = '${previousMonth.year}${previousMonth.month.toString().padLeft(2, '0')}';

    return MonthlySalesState(
      yearMonth: yearMonth,
    );
  }

  /// 로딩 상태로 전환
  MonthlySalesState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  MonthlySalesState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 데이터가 있는지 여부
  bool get hasData => monthlySales != null;

  /// 이전 월로 이동 가능한지 (예: 3년 전까지만 허용)
  bool get canGoToPreviousMonth {
    final current = DateTime.parse('${yearMonth}01');
    final threeYearsAgo = DateTime.now().subtract(const Duration(days: 365 * 3));
    return current.isAfter(threeYearsAgo);
  }

  /// 다음 월로 이동 가능한지 (미래는 조회 불가)
  bool get canGoToNextMonth {
    final current = DateTime.parse('${yearMonth}01');
    final now = DateTime.now();
    final currentMonth = DateTime(now.year, now.month, 1);
    return current.isBefore(currentMonth);
  }

  /// 이전 월 계산
  String getPreviousMonth() {
    final current = DateTime.parse('${yearMonth}01');
    final previous = DateTime(current.year, current.month - 1, 1);
    return '${previous.year}${previous.month.toString().padLeft(2, '0')}';
  }

  /// 다음 월 계산
  String getNextMonth() {
    final current = DateTime.parse('${yearMonth}01');
    final next = DateTime(current.year, current.month + 1, 1);
    return '${next.year}${next.month.toString().padLeft(2, '0')}';
  }

  /// 연월 표시 형식 (예: "2026년 02월")
  String get displayYearMonth {
    final year = yearMonth.substring(0, 4);
    final month = yearMonth.substring(4, 6);
    return '$year년 $month월';
  }

  MonthlySalesState copyWith({
    bool? isLoading,
    String? errorMessage,
    String? selectedCustomerId,
    String? yearMonth,
    MonthlySales? monthlySales,
    bool clearCustomerFilter = false,
  }) {
    return MonthlySalesState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      selectedCustomerId: clearCustomerFilter
          ? null
          : (selectedCustomerId ?? this.selectedCustomerId),
      yearMonth: yearMonth ?? this.yearMonth,
      monthlySales: monthlySales ?? this.monthlySales,
    );
  }
}
