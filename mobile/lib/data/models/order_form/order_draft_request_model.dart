/// 임시저장 요청 모델 (Spec #596 §5.1).
///
/// API: `POST /api/v1/mobile/orders/draft`
class OrderDraftRequestModel {
  /// 본인 담당 거래처 PK
  final int accountId;

  /// 납기일 (ISO 8601 `YYYY-MM-DD`, nullable — 전달 시 ≥ today)
  final String? deliveryDate;

  /// 총 주문금액 (≥ 0, 임시저장은 0 허용)
  final int totalAmount;

  /// 주문 라인 (길이 ≥ 1)
  final List<OrderDraftRequestLineModel> lines;

  const OrderDraftRequestModel({
    required this.accountId,
    this.deliveryDate,
    required this.totalAmount,
    required this.lines,
  });

  Map<String, dynamic> toJson() {
    return {
      'accountId': accountId,
      if (deliveryDate != null) 'deliveryDate': deliveryDate,
      'totalAmount': totalAmount,
      'lines': lines.map((e) => e.toJson()).toList(),
    };
  }
}

class OrderDraftRequestLineModel {
  final int lineNumber;
  final String productCode;
  final String unit;
  final double quantity;
  final int? quantityPieces;
  final double? quantityBoxes;
  final double? unitPrice;
  final double? amount;

  const OrderDraftRequestLineModel({
    required this.lineNumber,
    required this.productCode,
    required this.unit,
    required this.quantity,
    this.quantityPieces,
    this.quantityBoxes,
    this.unitPrice,
    this.amount,
  });

  Map<String, dynamic> toJson() {
    return {
      'lineNumber': lineNumber,
      'productCode': productCode,
      'unit': unit,
      'quantity': quantity,
      if (quantityPieces != null) 'quantityPieces': quantityPieces,
      if (quantityBoxes != null) 'quantityBoxes': quantityBoxes,
      if (unitPrice != null) 'unitPrice': unitPrice,
      if (amount != null) 'amount': amount,
    };
  }
}
