/// 임시저장 GET 응답 모델 (Spec #596 §5.2).
///
/// API: `GET /api/v1/mobile/orders/draft`
/// `data` 가 null 이면 임시저장 없음.
class OrderDraftResponseModel {
  final int draftId;
  final int accountId;
  final String accountName;
  final String accountExternalKey;
  final String? deliveryDate;
  final int totalAmount;
  final String savedAt;
  final List<OrderDraftLineModel> lines;

  const OrderDraftResponseModel({
    required this.draftId,
    required this.accountId,
    required this.accountName,
    required this.accountExternalKey,
    this.deliveryDate,
    required this.totalAmount,
    required this.savedAt,
    required this.lines,
  });

  factory OrderDraftResponseModel.fromJson(Map<String, dynamic> json) {
    return OrderDraftResponseModel(
      draftId: (json['draftId'] as num).toInt(),
      accountId: (json['accountId'] as num).toInt(),
      accountName: json['accountName'] as String,
      accountExternalKey: json['accountExternalKey'] as String,
      deliveryDate: json['deliveryDate'] as String?,
      totalAmount: (json['totalAmount'] as num).toInt(),
      savedAt: json['savedAt'] as String,
      lines: (json['lines'] as List<dynamic>)
          .map((e) => OrderDraftLineModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class OrderDraftLineModel {
  final int lineNumber;
  final String productCode;
  final String productName;
  final String unit;
  final double quantity;
  final int? quantityPieces;
  final double? quantityBoxes;

  /// 1박스당 입수. 복원 시 박스→낱개 환산·소계 재계산에 사용 (백엔드가 제품 마스터에서 재조회).
  final int? boxSize;
  final double? unitPrice;
  final double? amount;

  const OrderDraftLineModel({
    required this.lineNumber,
    required this.productCode,
    required this.productName,
    required this.unit,
    required this.quantity,
    this.quantityPieces,
    this.quantityBoxes,
    this.boxSize,
    this.unitPrice,
    this.amount,
  });

  factory OrderDraftLineModel.fromJson(Map<String, dynamic> json) {
    return OrderDraftLineModel(
      lineNumber: (json['lineNumber'] as num).toInt(),
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      unit: json['unit'] as String,
      quantity: (json['quantity'] as num).toDouble(),
      quantityPieces: (json['quantityPieces'] as num?)?.toInt(),
      quantityBoxes: (json['quantityBoxes'] as num?)?.toDouble(),
      boxSize: (json['boxSize'] as num?)?.toInt(),
      unitPrice: (json['unitPrice'] as num?)?.toDouble(),
      amount: (json['amount'] as num?)?.toDouble(),
    );
  }
}

/// `POST /api/v1/mobile/orders/draft` 응답 (Spec #596 §5.3).
class OrderDraftSavedModel {
  final int draftId;
  final String savedAt;

  const OrderDraftSavedModel({
    required this.draftId,
    required this.savedAt,
  });

  factory OrderDraftSavedModel.fromJson(Map<String, dynamic> json) {
    return OrderDraftSavedModel(
      draftId: (json['draftId'] as num).toInt(),
      savedAt: json['savedAt'] as String,
    );
  }
}
