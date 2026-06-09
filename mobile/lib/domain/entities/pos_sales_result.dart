import 'pos_sales.dart';

/// POS 매출 조회 결과 (거래처 1곳 + 기간 + 선택 바코드).
///
/// 레거시 `posmain.jsp` 의 매출 조회 응답 동등 — 제품별 명세([items]) 와 합계금액([totalAmount]).
/// 백엔드 `GET /api/v1/mobile/sales/pos/by-range` 응답을 표현한다.
class PosSalesResult {
  /// 제품별 POS 매출 명세
  final List<PosSales> items;

  /// 합계금액(원) — 레거시 `#totalAmount`
  final int totalAmount;

  /// 합계수량(EA)
  final int totalQuantity;

  const PosSalesResult({
    required this.items,
    required this.totalAmount,
    required this.totalQuantity,
  });

  /// 빈 결과 (미조회/0건)
  static const PosSalesResult empty =
      PosSalesResult(items: [], totalAmount: 0, totalQuantity: 0);
}
