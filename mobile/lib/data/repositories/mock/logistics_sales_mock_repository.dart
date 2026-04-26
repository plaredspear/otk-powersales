import '../../../domain/entities/logistics_sales.dart';
import '../../../domain/repositories/logistics_sales_repository.dart';
import '../../mock/logistics_sales_mock_data.dart';

/// 물류매출 Mock Repository 구현체
///
/// LogisticsSalesRepository 인터페이스를 구현하여 Mock 데이터를 반환합니다.
/// 실제 API 연동 전까지 UI 개발 및 테스트용으로 사용됩니다.
class LogisticsSalesMockRepository implements LogisticsSalesRepository {
  @override
  Future<List<LogisticsSales>> getLogisticsSales({
    required String yearMonth,
    LogisticsCategory? category,
    String? customerName,
  }) async {
    // Mock 데이터에서 년월로 필터링
    var results = LogisticsSalesMockData.getByYearMonth(yearMonth);

    // 카테고리 필터링 (선택적)
    if (category != null) {
      results = results
          .where((sales) => sales.category == category)
          .toList();
    }

    // customerName 필터링 (선택적)
    // Note: Mock 데이터에는 거래처명 필드가 없지만, 향후 확장을 위해 로직 추가
    if (customerName != null && customerName.isNotEmpty) {
      // Mock 데이터에서는 거래처명 필드가 없으므로 빈 리스트 반환
      results = [];
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 카테고리 코드순 정렬
    results.sort((a, b) => a.category.code.compareTo(b.category.code));

    return results;
  }

  @override
  Future<List<LogisticsSales>> getLogisticsSalesByCategory({
    required String yearMonth,
    required LogisticsCategory category,
  }) async {
    // Mock 데이터에서 년월 + 카테고리로 필터링
    final results = LogisticsSalesMockData.getByYearMonthAndCategory(
      yearMonth,
      category,
    );

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    return results;
  }

  @override
  Future<List<LogisticsSales>> getLogisticsSalesTrend({
    required String startYearMonth,
    required String endYearMonth,
    required LogisticsCategory category,
  }) async {
    // Mock 데이터에서 카테고리별 트렌드 조회
    final results = LogisticsSalesMockData.getCategoryTrend(
      category,
      startYearMonth,
      endYearMonth,
    );

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 년월순 정렬 (오름차순 - 시간 순서)
    results.sort((a, b) => a.yearMonth.compareTo(b.yearMonth));

    return results;
  }

  @override
  Future<List<LogisticsSales>> getCurrentMonthSales({
    LogisticsCategory? category,
  }) async {
    // Mock 데이터에서 당월 물류예상실적만 필터링
    var results = LogisticsSalesMockData.getCurrentMonthEstimates();

    // 카테고리 필터링 (선택적)
    if (category != null) {
      results = results
          .where((sales) => sales.category == category)
          .toList();
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 카테고리 코드순 정렬
    results.sort((a, b) => a.category.code.compareTo(b.category.code));

    return results;
  }

  @override
  Future<List<LogisticsSales>> getClosedSales({
    required String yearMonth,
    LogisticsCategory? category,
  }) async {
    // Mock 데이터에서 마감 실적만 필터링
    var results = LogisticsSalesMockData.getClosedSales();

    // 년월 필터링
    results = results
        .where((sales) => sales.yearMonth == yearMonth)
        .toList();

    // 카테고리 필터링 (선택적)
    if (category != null) {
      results = results
          .where((sales) => sales.category == category)
          .toList();
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 카테고리 코드순 정렬
    results.sort((a, b) => a.category.code.compareTo(b.category.code));

    return results;
  }
}
