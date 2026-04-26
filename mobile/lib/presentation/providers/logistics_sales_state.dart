import '../../domain/entities/logistics_sales.dart';

/// 물류매출 조회 필터 조건
class LogisticsSalesFilter {
  /// 년월 (필수, 예: "202601")
  final String yearMonth;

  /// 카테고리 (선택, null이면 전체 카테고리)
  final LogisticsCategory? category;

  /// 거래처명 (선택)
  final String? customerName;

  const LogisticsSalesFilter({
    required this.yearMonth,
    this.category,
    this.customerName,
  });

  /// 기본 필터 (현재 월, 전체 카테고리)
  factory LogisticsSalesFilter.defaultFilter() {
    final now = DateTime.now();
    final yearMonth = '${now.year}${now.month.toString().padLeft(2, '0')}';
    return LogisticsSalesFilter(yearMonth: yearMonth);
  }

  LogisticsSalesFilter copyWith({
    String? yearMonth,
    LogisticsCategory? category,
    bool clearCategory = false,
    String? customerName,
    bool clearCustomerName = false,
  }) {
    return LogisticsSalesFilter(
      yearMonth: yearMonth ?? this.yearMonth,
      category: clearCategory ? null : (category ?? this.category),
      customerName:
          clearCustomerName ? null : (customerName ?? this.customerName),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is LogisticsSalesFilter &&
        other.yearMonth == yearMonth &&
        other.category == category &&
        other.customerName == customerName;
  }

  @override
  int get hashCode {
    return Object.hash(
      yearMonth,
      category,
      customerName,
    );
  }
}

/// 물류매출 조회 상태
class LogisticsSalesState {
  /// 조회된 물류매출 목록
  final List<LogisticsSales> sales;

  /// 현재 적용된 필터
  final LogisticsSalesFilter filter;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 현재 년월 (당월/이전월 판단용)
  final String currentYearMonth;

  /// 당월 여부
  final bool isCurrentMonth;

  /// 총 금액 (당해)
  final int totalAmount;

  /// 총 금액 (전년)
  final int totalPreviousYearAmount;

  /// 총 증감 금액
  final int totalDifference;

  /// 전체 증감율 (%)
  final double? totalGrowthRate;

  /// 카테고리별 합계 Map (카테고리 -> 총 금액)
  final Map<LogisticsCategory, int> categoryTotals;

  /// 카테고리 목록 (중복 제거)
  final List<LogisticsCategory> categoryList;

  const LogisticsSalesState({
    required this.sales,
    required this.filter,
    required this.currentYearMonth,
    this.isLoading = false,
    this.errorMessage,
    this.isCurrentMonth = false,
    this.totalAmount = 0,
    this.totalPreviousYearAmount = 0,
    this.totalDifference = 0,
    this.totalGrowthRate,
    this.categoryTotals = const {},
    this.categoryList = const [],
  });

  /// 초기 상태
  factory LogisticsSalesState.initial() {
    final now = DateTime.now();
    final currentYearMonth =
        '${now.year}${now.month.toString().padLeft(2, '0')}';
    return LogisticsSalesState(
      sales: const [],
      filter: LogisticsSalesFilter.defaultFilter(),
      currentYearMonth: currentYearMonth,
      isCurrentMonth: true,
    );
  }

  LogisticsSalesState copyWith({
    List<LogisticsSales>? sales,
    LogisticsSalesFilter? filter,
    String? currentYearMonth,
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    bool? isCurrentMonth,
    int? totalAmount,
    int? totalPreviousYearAmount,
    int? totalDifference,
    double? totalGrowthRate,
    bool clearTotalGrowthRate = false,
    Map<LogisticsCategory, int>? categoryTotals,
    List<LogisticsCategory>? categoryList,
  }) {
    return LogisticsSalesState(
      sales: sales ?? this.sales,
      filter: filter ?? this.filter,
      currentYearMonth: currentYearMonth ?? this.currentYearMonth,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      isCurrentMonth: isCurrentMonth ?? this.isCurrentMonth,
      totalAmount: totalAmount ?? this.totalAmount,
      totalPreviousYearAmount:
          totalPreviousYearAmount ?? this.totalPreviousYearAmount,
      totalDifference: totalDifference ?? this.totalDifference,
      totalGrowthRate: clearTotalGrowthRate
          ? null
          : (totalGrowthRate ?? this.totalGrowthRate),
      categoryTotals: categoryTotals ?? this.categoryTotals,
      categoryList: categoryList ?? this.categoryList,
    );
  }
}
