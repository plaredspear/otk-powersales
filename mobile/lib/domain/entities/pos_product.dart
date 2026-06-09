/// POS 매출 조회용 제품 엔티티.
///
/// 레거시 `posmain.jsp` 의 매출 조회 제품(`#add-prd-list`) 항목 동등 — 제품명 검색/바코드 스캔으로
/// 추가되어, 선택된 항목의 [barcode] 가 POS 매출 조회 필터(`BARCODE IN (...)`)로 전달된다.
class PosProduct {
  /// 제품 코드 (`ITEM_CD` / 제품코드)
  final String productCode;

  /// 제품명
  final String productName;

  /// 대표 바코드 (POS `BARCODE` 매칭 키). 없으면 빈 문자열.
  final String barcode;

  const PosProduct({
    required this.productCode,
    required this.productName,
    required this.barcode,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is PosProduct &&
        other.productCode == productCode &&
        other.barcode == barcode;
  }

  @override
  int get hashCode => Object.hash(productCode, barcode);

  @override
  String toString() =>
      'PosProduct(productCode: $productCode, productName: $productName, barcode: $barcode)';
}
