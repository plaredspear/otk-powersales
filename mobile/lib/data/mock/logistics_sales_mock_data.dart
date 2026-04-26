import '../../domain/entities/logistics_sales.dart';

/// 물류매출 Mock 데이터
///
/// 물류배부 실적 데이터
/// - 당월: 물류예상실적 반영
/// - 이전월: ABC물류배부 마감 실적
/// - 카테고리: 상온, 라면, 냉동/냉장
/// UI 개발 및 테스트용 샘플 데이터
class LogisticsSalesMockData {
  static final List<LogisticsSales> data = [
    // 2026년 1월 - 당월 물류예상실적 (상온)
    LogisticsSales(
      yearMonth: '202601',
      category: LogisticsCategory.normal,
      currentAmount: 180000000,
      previousYearAmount: 165000000,
      difference: 15000000,
      growthRate: 9.1,
      isCurrentMonth: true,
    ),

    // 2026년 1월 - 당월 물류예상실적 (라면)
    LogisticsSales(
      yearMonth: '202601',
      category: LogisticsCategory.ramen,
      currentAmount: 320000000,
      previousYearAmount: 295000000,
      difference: 25000000,
      growthRate: 8.5,
      isCurrentMonth: true,
    ),

    // 2026년 1월 - 당월 물류예상실적 (냉동/냉장)
    LogisticsSales(
      yearMonth: '202601',
      category: LogisticsCategory.frozen,
      currentAmount: 145000000,
      previousYearAmount: 138000000,
      difference: 7000000,
      growthRate: 5.1,
      isCurrentMonth: true,
    ),

    // 2025년 12월 - 마감 실적 (상온)
    LogisticsSales(
      yearMonth: '202512',
      category: LogisticsCategory.normal,
      currentAmount: 175000000,
      previousYearAmount: 162000000,
      difference: 13000000,
      growthRate: 8.0,
      isCurrentMonth: false,
    ),

    // 2025년 12월 - 마감 실적 (라면)
    LogisticsSales(
      yearMonth: '202512',
      category: LogisticsCategory.ramen,
      currentAmount: 310000000,
      previousYearAmount: 288000000,
      difference: 22000000,
      growthRate: 7.6,
      isCurrentMonth: false,
    ),

    // 2025년 12월 - 마감 실적 (냉동/냉장)
    LogisticsSales(
      yearMonth: '202512',
      category: LogisticsCategory.frozen,
      currentAmount: 140000000,
      previousYearAmount: 135000000,
      difference: 5000000,
      growthRate: 3.7,
      isCurrentMonth: false,
    ),

    // 2025년 11월 - 마감 실적 (상온)
    LogisticsSales(
      yearMonth: '202511',
      category: LogisticsCategory.normal,
      currentAmount: 168000000,
      previousYearAmount: 158000000,
      difference: 10000000,
      growthRate: 6.3,
      isCurrentMonth: false,
    ),

    // 2025년 11월 - 마감 실적 (라면)
    LogisticsSales(
      yearMonth: '202511',
      category: LogisticsCategory.ramen,
      currentAmount: 298000000,
      previousYearAmount: 280000000,
      difference: 18000000,
      growthRate: 6.4,
      isCurrentMonth: false,
    ),

    // 2025년 11월 - 마감 실적 (냉동/냉장)
    LogisticsSales(
      yearMonth: '202511',
      category: LogisticsCategory.frozen,
      currentAmount: 135000000,
      previousYearAmount: 132000000,
      difference: 3000000,
      growthRate: 2.3,
      isCurrentMonth: false,
    ),

    // 2025년 10월 - 마감 실적 (상온)
    LogisticsSales(
      yearMonth: '202510',
      category: LogisticsCategory.normal,
      currentAmount: 172000000,
      previousYearAmount: 165000000,
      difference: 7000000,
      growthRate: 4.2,
      isCurrentMonth: false,
    ),

    // 2025년 10월 - 마감 실적 (라면)
    LogisticsSales(
      yearMonth: '202510',
      category: LogisticsCategory.ramen,
      currentAmount: 305000000,
      previousYearAmount: 292000000,
      difference: 13000000,
      growthRate: 4.5,
      isCurrentMonth: false,
    ),

    // 2025년 10월 - 마감 실적 (냉동/냉장)
    LogisticsSales(
      yearMonth: '202510',
      category: LogisticsCategory.frozen,
      currentAmount: 138000000,
      previousYearAmount: 140000000,
      difference: -2000000,
      growthRate: -1.4,
      isCurrentMonth: false,
    ),

    // 2026년 2월 - 당월 물류예상실적 (상온) - 미래 데이터
    LogisticsSales(
      yearMonth: '202602',
      category: LogisticsCategory.normal,
      currentAmount: 185000000,
      previousYearAmount: 168000000,
      difference: 17000000,
      growthRate: 10.1,
      isCurrentMonth: true,
    ),

    // 2026년 2월 - 당월 물류예상실적 (라면) - 미래 데이터
    LogisticsSales(
      yearMonth: '202602',
      category: LogisticsCategory.ramen,
      currentAmount: 328000000,
      previousYearAmount: 300000000,
      difference: 28000000,
      growthRate: 9.3,
      isCurrentMonth: true,
    ),

    // 2026년 2월 - 당월 물류예상실적 (냉동/냉장) - 미래 데이터
    LogisticsSales(
      yearMonth: '202602',
      category: LogisticsCategory.frozen,
      currentAmount: 150000000,
      previousYearAmount: 142000000,
      difference: 8000000,
      growthRate: 5.6,
      isCurrentMonth: true,
    ),
  ];

  /// 년월로 필터링
  static List<LogisticsSales> getByYearMonth(String yearMonth) {
    return data.where((sales) => sales.yearMonth == yearMonth).toList();
  }

  /// 카테고리로 필터링
  static List<LogisticsSales> getByCategory(LogisticsCategory category) {
    return data.where((sales) => sales.category == category).toList();
  }

  /// 년월 + 카테고리로 필터링
  static List<LogisticsSales> getByYearMonthAndCategory(
    String yearMonth,
    LogisticsCategory category,
  ) {
    return data
        .where((sales) =>
            sales.yearMonth == yearMonth && sales.category == category)
        .toList();
  }

  /// 당월 물류예상실적만 필터링
  static List<LogisticsSales> getCurrentMonthEstimates() {
    return data.where((sales) => sales.isCurrentMonth).toList();
  }

  /// 마감 실적만 필터링
  static List<LogisticsSales> getClosedSales() {
    return data.where((sales) => !sales.isCurrentMonth).toList();
  }

  /// 년월 범위로 필터링 (트렌드 분석용)
  static List<LogisticsSales> getByYearMonthRange(
    String startYearMonth,
    String endYearMonth,
  ) {
    return data
        .where((sales) =>
            sales.yearMonth.compareTo(startYearMonth) >= 0 &&
            sales.yearMonth.compareTo(endYearMonth) <= 0)
        .toList();
  }

  /// 카테고리별 트렌드 데이터 (특정 기간, 특정 카테고리)
  static List<LogisticsSales> getCategoryTrend(
    LogisticsCategory category,
    String startYearMonth,
    String endYearMonth,
  ) {
    return data
        .where((sales) =>
            sales.category == category &&
            sales.yearMonth.compareTo(startYearMonth) >= 0 &&
            sales.yearMonth.compareTo(endYearMonth) <= 0)
        .toList();
  }
}
