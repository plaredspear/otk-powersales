/// 임시저장 GET 응답 모델 (Spec #596 §5.2).
///
/// API: `GET /api/v1/mobile/orders/draft`
/// `data` 가 null 이면 임시저장 없음.
class OrderDraftResponseModel {
  final int draftId;
  /// 거래처 — 미선택 상태로 저장된 임시저장이면 null.
  final int? accountId;
  final String accountName;

  /// 거래처 SF external_key. 백엔드 계약상 nullable —
  /// 거래처 행이 없거나(삭제) 레거시 이관 draft 의 `account_id` 가 비어 있으면 null 로 내려온다.
  /// non-null 로 캐스팅하면 주문서 진입 자체가 "임시저장 조회 오류" 로 막히므로 nullable 유지.
  final String? accountExternalKey;
  final String? deliveryDate;
  final int totalAmount;
  final String savedAt;
  final List<OrderDraftLineModel> lines;

  const OrderDraftResponseModel({
    required this.draftId,
    this.accountId,
    required this.accountName,
    this.accountExternalKey,
    this.deliveryDate,
    required this.totalAmount,
    required this.savedAt,
    required this.lines,
  });

  factory OrderDraftResponseModel.fromJson(Map<String, dynamic> json) {
    return OrderDraftResponseModel(
      draftId: (json['draftId'] as num).toInt(),
      accountId: (json['accountId'] as num?)?.toInt(),
      accountName: json['accountName'] as String? ?? '',
      accountExternalKey: json['accountExternalKey'] as String?,
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

  /// 제품명. 백엔드가 제품 마스터에서 재조회해 내려주므로 단종/삭제/미적재 코드면 null 이다
  /// (`tmp_order_product` 에는 제품명 컬럼이 없어 저장값 fallback 이 불가능).
  /// 표시가 끊기지 않도록 그 경우 제품코드로 대체해 채운다.
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
    final productCode = json['productCode'] as String? ?? '';
    return OrderDraftLineModel(
      lineNumber: (json['lineNumber'] as num).toInt(),
      productCode: productCode,
      productName: json['productName'] as String? ?? productCode,
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
