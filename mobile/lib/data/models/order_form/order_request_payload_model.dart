/// 주문 등록 요청 페이로드 모델 (Spec #592 §5.1).
///
/// API: `POST /api/v1/mobile/order-requests`
/// `clientRequestId` 는 본문 또는 헤더 `Idempotency-Key` 로 송신 (Spec #598 P1-M Q10).
class OrderRequestPayloadModel {
  /// UUID v4 멱등키 (모바일 발급, 길이 36자)
  final String? clientRequestId;

  /// 거래처 PK (Long)
  final int accountId;

  /// 납기일 (ISO 8601 `YYYY-MM-DD`)
  final String deliveryDate;

  /// 총 주문금액 (원)
  final int totalAmount;

  /// 주문 라인
  final List<OrderRequestLineModel> lines;

  const OrderRequestPayloadModel({
    this.clientRequestId,
    required this.accountId,
    required this.deliveryDate,
    required this.totalAmount,
    required this.lines,
  });

  Map<String, dynamic> toJson() {
    return {
      if (clientRequestId != null) 'clientRequestId': clientRequestId,
      'accountId': accountId,
      'deliveryDate': deliveryDate,
      'totalAmount': totalAmount,
      'lines': lines.map((e) => e.toJson()).toList(),
    };
  }
}

class OrderRequestLineModel {
  /// 라인 번호 (≥ 0, 동일 요청 내 unique)
  final int lineNumber;

  /// 제품코드 (1~20자)
  final String productCode;

  /// 단위 기준 수량 (> 0)
  final double quantity;

  /// 단위 (`BOX` / `EA`)
  final String unit;

  /// 총 EA 수량 (≥ 1)
  final int quantityPieces;

  /// BOX 수량 (≥ 0, 소수점 2자리)
  final double quantityBoxes;

  const OrderRequestLineModel({
    required this.lineNumber,
    required this.productCode,
    required this.quantity,
    required this.unit,
    required this.quantityPieces,
    required this.quantityBoxes,
  });

  Map<String, dynamic> toJson() {
    return {
      'lineNumber': lineNumber,
      'productCode': productCode,
      'quantity': quantity,
      'unit': unit,
      'quantityPieces': quantityPieces,
      'quantityBoxes': quantityBoxes,
    };
  }
}
