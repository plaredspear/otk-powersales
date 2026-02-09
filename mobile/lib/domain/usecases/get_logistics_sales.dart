import '../entities/logistics_sales.dart';
import '../repositories/logistics_sales_repository.dart';

/// 물류매출 조회 UseCase
///
/// 물류매출 조회 및 필터링 비즈니스 로직을 처리합니다.
/// - 당월: 물류예상실적 반영
/// - 이전월: ABC물류배부 마감 실적
class GetLogisticsSales {
  final LogisticsSalesRepository _repository;

  GetLogisticsSales(this._repository);

  /// 물류매출 조회 (기본)
  ///
  /// [yearMonth] 년월 (예: "202601")
  /// [category] 카테고리 필터 (선택)
  /// [customerName] 거래처명 필터 (선택)
  ///
  /// Returns: 물류매출 목록
  Future<List<LogisticsSales>> call({
    required String yearMonth,
    LogisticsCategory? category,
    String? customerName,
  }) async {
    return _repository.getLogisticsSales(
      yearMonth: yearMonth,
      category: category,
      customerName: customerName,
    );
  }

  /// 카테고리별 물류매출 조회
  ///
  /// [yearMonth] 년월
  /// [category] 카테고리 (상온, 라면, 냉동/냉장)
  ///
  /// Returns: 해당 카테고리의 물류매출 목록
  Future<List<LogisticsSales>> getByCategory({
    required String yearMonth,
    required LogisticsCategory category,
  }) async {
    return _repository.getLogisticsSalesByCategory(
      yearMonth: yearMonth,
      category: category,
    );
  }

  /// 카테고리별 월별 추이 조회 (차트용)
  ///
  /// [startYearMonth] 시작 년월 (예: "202601")
  /// [endYearMonth] 종료 년월 (예: "202612")
  /// [category] 카테고리
  ///
  /// Returns: 월별 물류매출 목록 (차트 시각화용)
  Future<List<LogisticsSales>> getTrend({
    required String startYearMonth,
    required String endYearMonth,
    required LogisticsCategory category,
  }) async {
    // 년월 범위 검증
    if (startYearMonth.compareTo(endYearMonth) > 0) {
      throw ArgumentError('시작 년월은 종료 년월보다 이전이어야 합니다.');
    }

    return _repository.getLogisticsSalesTrend(
      startYearMonth: startYearMonth,
      endYearMonth: endYearMonth,
      category: category,
    );
  }

  /// 당월 물류예상실적 조회
  ///
  /// [category] 카테고리 필터 (선택)
  ///
  /// Returns: 당월 물류예상실적 목록
  Future<List<LogisticsSales>> getCurrentMonthSales({
    LogisticsCategory? category,
  }) async {
    return _repository.getCurrentMonthSales(
      category: category,
    );
  }

  /// 이전월 ABC물류배부 마감실적 조회
  ///
  /// [yearMonth] 조회 년월
  /// [category] 카테고리 필터 (선택)
  ///
  /// Returns: 마감실적 목록
  Future<List<LogisticsSales>> getClosedSales({
    required String yearMonth,
    LogisticsCategory? category,
  }) async {
    return _repository.getClosedSales(
      yearMonth: yearMonth,
      category: category,
    );
  }

  /// 당월/이전월 구분 조회
  ///
  /// 현재 년월을 기준으로 당월이면 물류예상실적을,
  /// 이전월이면 ABC물류배부 마감실적을 조회합니다.
  ///
  /// [yearMonth] 조회 년월
  /// [currentYearMonth] 현재 년월 (기본값: 현재 시스템 년월)
  /// [category] 카테고리 필터 (선택)
  ///
  /// Returns: 물류매출 목록
  Future<List<LogisticsSales>> getSalesByMonthType({
    required String yearMonth,
    String? currentYearMonth,
    LogisticsCategory? category,
  }) async {
    // 현재 년월이 지정되지 않으면 시스템 현재 년월 사용
    final now = DateTime.now();
    final currentMonth = currentYearMonth ??
        '${now.year}${now.month.toString().padLeft(2, '0')}';

    // 당월 여부 판단
    final isCurrentMonth = yearMonth == currentMonth;

    if (isCurrentMonth) {
      // 당월: 물류예상실적 조회
      return getCurrentMonthSales(category: category);
    } else {
      // 이전월: ABC물류배부 마감실적 조회
      return getClosedSales(
        yearMonth: yearMonth,
        category: category,
      );
    }
  }

  /// 물류매출 목록의 총 금액 계산
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 총 금액
  int calculateTotalAmount(List<LogisticsSales> salesList) {
    if (salesList.isEmpty) return 0;
    return salesList.fold(0, (sum, sales) => sum + sales.currentAmount);
  }

  /// 전년 총 금액 계산
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 전년 총 금액
  int calculateTotalPreviousYearAmount(List<LogisticsSales> salesList) {
    if (salesList.isEmpty) return 0;
    return salesList.fold(0, (sum, sales) => sum + sales.previousYearAmount);
  }

  /// 총 증감 금액 계산
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 총 증감 금액
  int calculateTotalDifference(List<LogisticsSales> salesList) {
    if (salesList.isEmpty) return 0;
    return salesList.fold(0, (sum, sales) => sum + sales.difference);
  }

  /// 전체 증감율 계산 (가중 평균)
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 전체 증감율 (%), null이면 전년 실적이 0
  double? calculateTotalGrowthRate(List<LogisticsSales> salesList) {
    if (salesList.isEmpty) return null;

    final totalCurrent = calculateTotalAmount(salesList);
    final totalPrevious = calculateTotalPreviousYearAmount(salesList);

    if (totalPrevious == 0) return null;

    return ((totalCurrent - totalPrevious) / totalPrevious) * 100;
  }

  /// 카테고리별 합계 계산
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 카테고리별 합계 Map (카테고리 -> 총 금액)
  Map<LogisticsCategory, int> calculateCategoryTotals(
    List<LogisticsSales> salesList,
  ) {
    final totals = <LogisticsCategory, int>{};

    for (final sales in salesList) {
      totals[sales.category] =
          (totals[sales.category] ?? 0) + sales.currentAmount;
    }

    return totals;
  }

  /// 카테고리 목록 추출
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 중복 제거된 카테고리 목록 (정렬됨)
  List<LogisticsCategory> extractCategories(List<LogisticsSales> salesList) {
    final categories = salesList.map((sales) => sales.category).toSet();
    final categoryList = categories.toList();
    // 카테고리 코드순 정렬
    categoryList.sort((a, b) => a.code.compareTo(b.code));
    return categoryList;
  }

  /// 특정 카테고리의 실적만 필터링
  ///
  /// [salesList] 물류매출 목록
  /// [category] 카테고리
  ///
  /// Returns: 필터링된 물류매출 목록
  List<LogisticsSales> filterByCategory(
    List<LogisticsSales> salesList,
    LogisticsCategory category,
  ) {
    return salesList.where((sales) => sales.category == category).toList();
  }

  /// 당월 실적만 필터링
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 당월 실적 목록
  List<LogisticsSales> filterCurrentMonthSales(
    List<LogisticsSales> salesList,
  ) {
    return salesList.where((sales) => sales.isCurrentMonth).toList();
  }

  /// 마감 실적만 필터링
  ///
  /// [salesList] 물류매출 목록
  ///
  /// Returns: 마감 실적 목록
  List<LogisticsSales> filterClosedSales(List<LogisticsSales> salesList) {
    return salesList.where((sales) => !sales.isCurrentMonth).toList();
  }
}
