import '../entities/logistics_sales.dart';

/// 물류매출 Repository 인터페이스
///
/// 물류예상실적(당월) 및 ABC물류배부 마감실적(이전월) 조회를 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class LogisticsSalesRepository {
  /// 물류매출 데이터 조회
  ///
  /// [yearMonth]: 조회 년월 (예: '202601')
  /// [category]: 카테고리 필터 (선택적, null이면 전체 조회)
  /// [customerName]: 거래처명 필터 (선택적)
  ///
  /// Returns: 조건에 맞는 물류매출 목록
  Future<List<LogisticsSales>> getLogisticsSales({
    required String yearMonth,
    LogisticsCategory? category,
    String? customerName,
  });

  /// 특정 카테고리의 물류매출 조회
  ///
  /// [yearMonth]: 조회 년월
  /// [category]: 카테고리 (상온, 라면, 냉동/냉장)
  ///
  /// Returns: 카테고리별 물류매출 목록
  Future<List<LogisticsSales>> getLogisticsSalesByCategory({
    required String yearMonth,
    required LogisticsCategory category,
  });

  /// 카테고리별 월별 추이 조회 (차트용)
  ///
  /// [startYearMonth]: 시작 년월 (예: '202601')
  /// [endYearMonth]: 종료 년월 (예: '202612')
  /// [category]: 카테고리
  ///
  /// Returns: 월별 물류매출 목록 (차트 시각화용)
  Future<List<LogisticsSales>> getLogisticsSalesTrend({
    required String startYearMonth,
    required String endYearMonth,
    required LogisticsCategory category,
  });

  /// 당월 물류예상실적 조회
  ///
  /// [category]: 카테고리 필터 (선택적)
  ///
  /// Returns: 당월 물류예상실적 목록
  Future<List<LogisticsSales>> getCurrentMonthSales({
    LogisticsCategory? category,
  });

  /// 이전월 ABC물류배부 마감실적 조회
  ///
  /// [yearMonth]: 조회 년월
  /// [category]: 카테고리 필터 (선택적)
  ///
  /// Returns: 마감실적 목록
  Future<List<LogisticsSales>> getClosedSales({
    required String yearMonth,
    LogisticsCategory? category,
  });
}
