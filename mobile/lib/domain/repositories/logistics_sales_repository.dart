import '../entities/logistics_sales.dart';

/// 물류매출 Repository 인터페이스.
///
/// 거래처 1곳 + 연월 기준 온도대별 물류마감실적(ORORA `ShipClosingAmount`)을 조회한다.
abstract class LogisticsSalesRepository {
  /// 물류매출 조회.
  ///
  /// [customerId]: 거래처(매장) ID (내 거래처 목록의 accountId)
  /// [yearMonth]: 조회 년월 (예: '202601')
  ///
  /// Returns: 온도대별 물류매출 목록 (상온/라면/냉동·냉장)
  Future<List<LogisticsSales>> getLogisticsSales({
    required int customerId,
    required String yearMonth,
  });
}
