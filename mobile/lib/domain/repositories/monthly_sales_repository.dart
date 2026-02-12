import '../entities/monthly_sales.dart';

/// 월매출 Repository 인터페이스
///
/// 거래처별 월매출 통계 조회 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class MonthlySalesRepository {
  /// 월매출 통계 조회
  ///
  /// 특정 거래처의 월매출 통계 정보를 조회합니다.
  /// 제품유형별 매출, 전년 대비 데이터, 월 평균 실적을 포함합니다.
  ///
  /// [customerId]: 거래처 ID (null이면 전체 거래처 합산)
  /// [yearMonth]: 조회 연월 (YYYYMM 형식, 예: "202608")
  ///
  /// Returns: 월매출 통계 정보
  Future<MonthlySales> getMonthlySales({
    String? customerId,
    required String yearMonth,
  });
}
