import '../entities/logistics_sales.dart';
import '../repositories/logistics_sales_repository.dart';

/// 물류매출 조회 UseCase.
///
/// 거래처+연월 기준 온도대별 물류매출 조회 및 합계/증감 계산 로직을 담당한다.
class GetLogisticsSales {
  final LogisticsSalesRepository _repository;

  GetLogisticsSales(this._repository);

  /// 물류매출 조회.
  Future<List<LogisticsSales>> call({
    required int customerId,
    required String yearMonth,
  }) {
    return _repository.getLogisticsSales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
  }

  /// 당해 실적 합계.
  int calculateTotalAmount(List<LogisticsSales> salesList) =>
      salesList.fold(0, (sum, s) => sum + s.currentAmount);

  /// 전년 실적 합계.
  int calculateTotalPreviousYearAmount(List<LogisticsSales> salesList) =>
      salesList.fold(0, (sum, s) => sum + s.previousYearAmount);

  /// 증감 금액 합계.
  int calculateTotalDifference(List<LogisticsSales> salesList) =>
      salesList.fold(0, (sum, s) => sum + s.difference);

  /// 전체 증감율(%) — 가중 평균. 전년 실적 0 이면 null.
  double? calculateTotalGrowthRate(List<LogisticsSales> salesList) {
    if (salesList.isEmpty) return null;
    final totalCurrent = calculateTotalAmount(salesList);
    final totalPrevious = calculateTotalPreviousYearAmount(salesList);
    if (totalPrevious == 0) return null;
    return ((totalCurrent - totalPrevious) / totalPrevious) * 100;
  }

  /// 카테고리별 당해 실적 합계 Map.
  Map<LogisticsCategory, int> calculateCategoryTotals(
    List<LogisticsSales> salesList,
  ) {
    final totals = <LogisticsCategory, int>{};
    for (final s in salesList) {
      totals[s.category] = (totals[s.category] ?? 0) + s.currentAmount;
    }
    return totals;
  }

  /// 중복 제거된 카테고리 목록 (코드순 정렬).
  List<LogisticsCategory> extractCategories(List<LogisticsSales> salesList) {
    final categories = salesList.map((s) => s.category).toSet().toList();
    categories.sort((a, b) => a.code.compareTo(b.code));
    return categories;
  }
}
