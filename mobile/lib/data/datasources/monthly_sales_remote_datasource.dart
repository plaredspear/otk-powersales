import '../models/monthly_sales_model.dart';

/// 월매출 원격 데이터소스 인터페이스
///
/// API 서버와의 월매출 통계 관련 통신을 추상화합니다.
abstract class MonthlySalesRemoteDataSource {
  /// 월매출 조회 API 호출
  ///
  /// GET /api/v1/mobile/sales/monthly
  ///
  /// [customerId]: 거래처 ID (null이면 전체 거래처 합산)
  /// [yearMonth]: 조회 연월 (YYYYMM)
  Future<MonthlySalesModel> getMonthlySales({
    String? customerId,
    required String yearMonth,
  });
}
