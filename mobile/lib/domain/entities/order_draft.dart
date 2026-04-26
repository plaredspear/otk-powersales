import 'validation_error.dart';

/// 주문서 초안 엔티티
///
/// 주문서 작성 화면에서 사용되는 주문서 초안 정보를 담는 도메인 엔티티입니다.
/// 신규 주문 작성, 임시저장, 기존 주문 수정 등에서 활용됩니다.
class OrderDraft {
  /// 주문 ID (수정 시에만 존재)
  final int? id;

  /// 거래처 ID
  final int? clientId;

  /// 거래처명
  final String? clientName;

  /// 여신 잔액 (원)
  final int? creditBalance;

  /// 납기일
  final DateTime? deliveryDate;

  /// 주문 제품 목록
  final List<OrderDraftItem> items;

  /// 총 주문금액 (계산됨)
  final int totalAmount;

  /// 임시저장 여부
  final bool isDraft;

  /// 마지막 수정 시간
  final DateTime lastModified;

  const OrderDraft({
    this.id,
    this.clientId,
    this.clientName,
    this.creditBalance,
    this.deliveryDate,
    required this.items,
    required this.totalAmount,
    required this.isDraft,
    required this.lastModified,
  });

  /// 빈 주문서 초안 생성
  factory OrderDraft.empty() {
    return OrderDraft(
      items: const [],
      totalAmount: 0,
      isDraft: true,
      lastModified: DateTime.now(),
    );
  }

  /// 총 주문금액을 제품 목록에서 재계산
  int get calculatedTotalAmount {
    return items.fold(0, (sum, item) => sum + item.totalPrice);
  }

  /// 선택된 제품 목록
  List<OrderDraftItem> get selectedItems {
    return items.where((item) => item.isSelected).toList();
  }

  /// 모든 제품이 선택되었는지 여부
  bool get allItemsSelected {
    return items.isNotEmpty && items.every((item) => item.isSelected);
  }

  /// 유효성 에러가 있는 제품 목록
  List<OrderDraftItem> get itemsWithErrors {
    return items.where((item) => item.validationError != null).toList();
  }

  /// 필수 입력이 모두 완료되었는지 여부
  bool get isRequiredFieldsFilled {
    return clientId != null && deliveryDate != null && items.isNotEmpty;
  }

  OrderDraft copyWith({
    int? id,
    int? clientId,
    String? clientName,
    int? creditBalance,
    DateTime? deliveryDate,
    List<OrderDraftItem>? items,
    int? totalAmount,
    bool? isDraft,
    DateTime? lastModified,
  }) {
    return OrderDraft(
      id: id ?? this.id,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      creditBalance: creditBalance ?? this.creditBalance,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      items: items ?? this.items,
      totalAmount: totalAmount ?? this.totalAmount,
      isDraft: isDraft ?? this.isDraft,
      lastModified: lastModified ?? this.lastModified,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'clientId': clientId,
      'clientName': clientName,
      'creditBalance': creditBalance,
      'deliveryDate': deliveryDate?.toIso8601String(),
      'items': items.map((e) => e.toJson()).toList(),
      'totalAmount': totalAmount,
      'isDraft': isDraft,
      'lastModified': lastModified.toIso8601String(),
    };
  }

  factory OrderDraft.fromJson(Map<String, dynamic> json) {
    return OrderDraft(
      id: json['id'] as int?,
      clientId: json['clientId'] as int?,
      clientName: json['clientName'] as String?,
      creditBalance: json['creditBalance'] as int?,
      deliveryDate: json['deliveryDate'] != null
          ? DateTime.parse(json['deliveryDate'] as String)
          : null,
      items: (json['items'] as List<dynamic>)
          .map((e) => OrderDraftItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalAmount: json['totalAmount'] as int,
      isDraft: json['isDraft'] as bool,
      lastModified: DateTime.parse(json['lastModified'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderDraft) return false;
    if (other.id != id) return false;
    if (other.clientId != clientId) return false;
    if (other.clientName != clientName) return false;
    if (other.creditBalance != creditBalance) return false;
    if (other.deliveryDate != deliveryDate) return false;
    if (other.totalAmount != totalAmount) return false;
    if (other.isDraft != isDraft) return false;
    if (other.lastModified != lastModified) return false;
    if (other.items.length != items.length) return false;
    for (var i = 0; i < items.length; i++) {
      if (other.items[i] != items[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      clientId,
      clientName,
      creditBalance,
      deliveryDate,
      Object.hashAll(items),
      totalAmount,
      isDraft,
      lastModified,
    );
  }

  @override
  String toString() {
    return 'OrderDraft(id: $id, clientId: $clientId, clientName: $clientName, '
        'deliveryDate: $deliveryDate, items: ${items.length}, '
        'totalAmount: $totalAmount, isDraft: $isDraft)';
  }
}

/// 주문 제품 항목 엔티티
///
/// 주문서 초안에 포함된 개별 제품 항목 정보입니다.
/// 수량 입력, 선택 상태, 유효성 검증 결과 등을 포함합니다.
class OrderDraftItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 박스 수량 (소수점 가능)
  final double quantityBoxes;

  /// 낱개 수량
  final int quantityPieces;

  /// 제품 단가 (원)
  final int unitPrice;

  /// 1박스당 개수
  final int boxSize;

  /// 해당 제품 총액 (계산됨)
  final int totalPrice;

  /// 체크박스 선택 여부
  final bool isSelected;

  /// 유효성 검증 에러 정보
  final ValidationError? validationError;

  const OrderDraftItem({
    required this.productCode,
    required this.productName,
    required this.quantityBoxes,
    required this.quantityPieces,
    required this.unitPrice,
    required this.boxSize,
    required this.totalPrice,
    this.isSelected = false,
    this.validationError,
  });

  /// 총액을 수량과 단가에서 재계산
  int get calculatedTotalPrice {
    final totalPieces = (quantityBoxes * boxSize).round() + quantityPieces;
    return (totalPieces * unitPrice / boxSize).round();
  }

  /// 유효성 에러가 있는지 여부
  bool get hasError => validationError != null;

  OrderDraftItem copyWith({
    String? productCode,
    String? productName,
    double? quantityBoxes,
    int? quantityPieces,
    int? unitPrice,
    int? boxSize,
    int? totalPrice,
    bool? isSelected,
    ValidationError? validationError,
  }) {
    return OrderDraftItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      quantityBoxes: quantityBoxes ?? this.quantityBoxes,
      quantityPieces: quantityPieces ?? this.quantityPieces,
      unitPrice: unitPrice ?? this.unitPrice,
      boxSize: boxSize ?? this.boxSize,
      totalPrice: totalPrice ?? this.totalPrice,
      isSelected: isSelected ?? this.isSelected,
      validationError: validationError ?? this.validationError,
    );
  }

  /// validationError를 null로 클리어하기 위한 별도 메서드
  OrderDraftItem clearValidationError() {
    return OrderDraftItem(
      productCode: productCode,
      productName: productName,
      quantityBoxes: quantityBoxes,
      quantityPieces: quantityPieces,
      unitPrice: unitPrice,
      boxSize: boxSize,
      totalPrice: totalPrice,
      isSelected: isSelected,
      validationError: null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'quantityBoxes': quantityBoxes,
      'quantityPieces': quantityPieces,
      'unitPrice': unitPrice,
      'boxSize': boxSize,
      'totalPrice': totalPrice,
      'isSelected': isSelected,
      'validationError': validationError?.toJson(),
    };
  }

  factory OrderDraftItem.fromJson(Map<String, dynamic> json) {
    return OrderDraftItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      quantityBoxes: (json['quantityBoxes'] as num).toDouble(),
      quantityPieces: json['quantityPieces'] as int,
      unitPrice: json['unitPrice'] as int,
      boxSize: json['boxSize'] as int,
      totalPrice: json['totalPrice'] as int,
      isSelected: json['isSelected'] as bool? ?? false,
      validationError: json['validationError'] != null
          ? ValidationError.fromJson(
              json['validationError'] as Map<String, dynamic>)
          : null,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderDraftItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.quantityBoxes == quantityBoxes &&
        other.quantityPieces == quantityPieces &&
        other.unitPrice == unitPrice &&
        other.boxSize == boxSize &&
        other.totalPrice == totalPrice &&
        other.isSelected == isSelected &&
        other.validationError == validationError;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      quantityBoxes,
      quantityPieces,
      unitPrice,
      boxSize,
      totalPrice,
      isSelected,
      validationError,
    );
  }

  @override
  String toString() {
    return 'OrderDraftItem(productCode: $productCode, '
        'productName: $productName, '
        'quantityBoxes: $quantityBoxes, quantityPieces: $quantityPieces, '
        'unitPrice: $unitPrice, boxSize: $boxSize, '
        'totalPrice: $totalPrice, isSelected: $isSelected, '
        'hasError: $hasError)';
  }
}
