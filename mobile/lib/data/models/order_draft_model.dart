import '../../domain/entities/order_draft.dart';
import '../../domain/entities/validation_error.dart';

/// 주문서 초안 항목 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 OrderDraftItem 엔티티로 변환합니다.
class OrderDraftItemModel {
  final String productCode;
  final String productName;
  final double quantityBoxes;
  final int quantityPieces;
  final int unitPrice;
  final int boxSize;
  final int totalPrice;
  final bool isSelected;
  final ValidationError? validationError;

  const OrderDraftItemModel({
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

  /// snake_case JSON에서 파싱 (API 응답 또는 로컬 저장소)
  factory OrderDraftItemModel.fromJson(Map<String, dynamic> json) {
    return OrderDraftItemModel(
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

  /// snake_case JSON으로 직렬화 (로컬 저장소용 - UI 필드 포함)
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

  /// Domain Entity로 변환
  OrderDraftItem toEntity() {
    return OrderDraftItem(
      productCode: productCode,
      productName: productName,
      quantityBoxes: quantityBoxes,
      quantityPieces: quantityPieces,
      unitPrice: unitPrice,
      boxSize: boxSize,
      totalPrice: totalPrice,
      isSelected: isSelected,
      validationError: validationError,
    );
  }

  /// Domain Entity에서 생성
  factory OrderDraftItemModel.fromEntity(OrderDraftItem entity) {
    return OrderDraftItemModel(
      productCode: entity.productCode,
      productName: entity.productName,
      quantityBoxes: entity.quantityBoxes,
      quantityPieces: entity.quantityPieces,
      unitPrice: entity.unitPrice,
      boxSize: entity.boxSize,
      totalPrice: entity.totalPrice,
      isSelected: entity.isSelected,
      validationError: entity.validationError,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderDraftItemModel &&
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
    return 'OrderDraftItemModel(productCode: $productCode, '
        'productName: $productName, quantityBoxes: $quantityBoxes, '
        'quantityPieces: $quantityPieces, unitPrice: $unitPrice, '
        'boxSize: $boxSize, totalPrice: $totalPrice, '
        'isSelected: $isSelected, validationError: $validationError)';
  }
}

/// 주문서 초안 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 OrderDraft 엔티티로 변환합니다.
class OrderDraftModel {
  final int? id;
  final int? clientId;
  final String? clientName;
  final int? creditBalance;
  final String? deliveryDate;
  final List<OrderDraftItemModel> items;
  final int totalAmount;
  final bool isDraft;
  final String lastModified;

  const OrderDraftModel({
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

  /// snake_case JSON에서 파싱 (API 응답 또는 로컬 저장소)
  factory OrderDraftModel.fromJson(Map<String, dynamic> json) {
    // API 응답이 { "data": { ... } } 형태로 감싸져 있을 수 있음
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;

    final itemsJson = data['items'] as List<dynamic>? ?? [];

    return OrderDraftModel(
      id: data['id'] as int?,
      clientId: data['clientId'] as int?,
      clientName: data['clientName'] as String?,
      creditBalance: data['creditBalance'] as int?,
      deliveryDate: data['deliveryDate'] as String?,
      items: itemsJson
          .map((e) => OrderDraftItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalAmount: data['totalAmount'] as int,
      isDraft: data['isDraft'] as bool,
      lastModified: data['lastModified'] as String,
    );
  }

  /// snake_case JSON으로 직렬화 (로컬 저장소용 - 모든 필드 포함)
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'clientId': clientId,
      'clientName': clientName,
      'creditBalance': creditBalance,
      'deliveryDate': deliveryDate,
      'items': items.map((e) => e.toJson()).toList(),
      'totalAmount': totalAmount,
      'isDraft': isDraft,
      'lastModified': lastModified,
    };
  }

  /// API 요청용 JSON으로 직렬화 (submit/update 시 사용)
  /// UI 필드 제외, 필수 필드만 포함
  Map<String, dynamic> toRequestJson() {
    return {
      'clientId': clientId,
      'deliveryDate': deliveryDate,
      'items': items
          .map((e) => {
                'productCode': e.productCode,
                'quantityBoxes': e.quantityBoxes,
                'quantityPieces': e.quantityPieces,
              })
          .toList(),
    };
  }

  /// Domain Entity로 변환
  OrderDraft toEntity() {
    return OrderDraft(
      id: id,
      clientId: clientId,
      clientName: clientName,
      creditBalance: creditBalance,
      deliveryDate:
          deliveryDate != null ? DateTime.parse(deliveryDate!) : null,
      items: items.map((e) => e.toEntity()).toList(),
      totalAmount: totalAmount,
      isDraft: isDraft,
      lastModified: DateTime.parse(lastModified),
    );
  }

  /// Domain Entity에서 생성
  factory OrderDraftModel.fromEntity(OrderDraft entity) {
    return OrderDraftModel(
      id: entity.id,
      clientId: entity.clientId,
      clientName: entity.clientName,
      creditBalance: entity.creditBalance,
      deliveryDate:
          entity.deliveryDate?.toIso8601String().split('T')[0],
      items: entity.items
          .map((e) => OrderDraftItemModel.fromEntity(e))
          .toList(),
      totalAmount: entity.totalAmount,
      isDraft: entity.isDraft,
      lastModified: entity.lastModified.toIso8601String(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderDraftModel) return false;
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
    return 'OrderDraftModel(id: $id, clientId: $clientId, '
        'clientName: $clientName, deliveryDate: $deliveryDate, '
        'items: ${items.length}, totalAmount: $totalAmount, '
        'isDraft: $isDraft, lastModified: $lastModified)';
  }
}
