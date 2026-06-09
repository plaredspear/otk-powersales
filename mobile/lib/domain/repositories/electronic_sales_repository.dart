import '../entities/electronic_sales.dart';

/// 전산매출(ABC) Repository 인터페이스.
///
/// 거래처 1곳 + 기간 + 매출 조회 제품(바코드) 기준 전산매출(POS DB `live_tot_sales_dh`) 을
/// 조회한다. 레거시 `promotion/month/abcmain.jsp` 동등 — 단일 거래처 필수.
abstract class ElectronicSalesRepository {
  /// 전산매출 조회.
  ///
  /// [customerId]: 거래처(매장) ID (내 거래처 목록의 accountId)
  /// [startDate], [endDate]: 조회 기간 (`YYYY-MM-DD`)
  /// [barcodes]: 매출 조회 제품의 바코드 목록. 비어 있으면 합계금액만 조회.
  ///
  /// Returns: 합계금액 + 제품별 전산매출 목록 묶음
  Future<ElectronicSalesResult> getElectronicSales({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes,
  });
}
