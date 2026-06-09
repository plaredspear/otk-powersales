import '../entities/pos_sales_result.dart';

/// POS 매출 Repository 인터페이스.
///
/// 거래처 1곳 + 기간(시작/종료일) + 선택 바코드 목록 기준 제품별 POS 매출(POS DB `live_pos_sales_dh`)을
/// 조회한다. 레거시 `promotion/month/posmain.jsp` 동등 — 단일 거래처 필수.
abstract class PosSalesRepository {
  /// POS 매출 조회 (기간 + 선택 바코드).
  ///
  /// [customerId]: 거래처(매장) ID (내 거래처 목록의 accountId)
  /// [startDate], [endDate]: 조회 기간 (YYYY-MM-DD)
  /// [barcodes]: 매출 조회 제품의 바코드 목록. 비어 있으면 거래처 전체 제품 집계.
  ///
  /// Returns: 제품별 명세 + 합계금액
  Future<PosSalesResult> getPosSalesByRange({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes,
  });
}
