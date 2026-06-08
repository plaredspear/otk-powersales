import '../entities/pos_sales.dart';

/// POS 매출 Repository 인터페이스.
///
/// 거래처 1곳 + 연월 기준 제품별 POS 매출(POS DB `live_pos_sales_dh`) 을 조회한다.
/// 레거시 `promotion/month/posmain.jsp` 동등 — 단일 거래처 필수.
abstract class PosSalesRepository {
  /// POS 매출 조회.
  ///
  /// [customerId]: 거래처(매장) ID (내 거래처 목록의 accountId)
  /// [yearMonth]: 조회 년월 (예: '202601')
  ///
  /// Returns: 제품별 POS 매출 목록
  Future<List<PosSales>> getPosSales({
    required int customerId,
    required String yearMonth,
  });
}
