/// 행사 제품 엔티티
///
/// 행사에 참여하는 제품 정보를 담는 도메인 엔티티입니다.
class EventProduct {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 대표제품 여부
  final bool isMainProduct;

  const EventProduct({
    required this.productCode,
    required this.productName,
    required this.isMainProduct,
  });

  EventProduct copyWith({
    String? productCode,
    String? productName,
    bool? isMainProduct,
  }) {
    return EventProduct(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      isMainProduct: isMainProduct ?? this.isMainProduct,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'isMainProduct': isMainProduct,
    };
  }

  factory EventProduct.fromJson(Map<String, dynamic> json) {
    return EventProduct(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      isMainProduct: json['isMainProduct'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EventProduct &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.isMainProduct == isMainProduct;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      isMainProduct,
    );
  }

  @override
  String toString() {
    return 'EventProduct(productCode: $productCode, productName: $productName, '
        'isMainProduct: $isMainProduct)';
  }
}

/// 행사 엔티티
///
/// 영업 행사(프로모션) 정보를 담는 도메인 엔티티입니다.
class Event {
  /// 행사 고유 ID
  final String id;

  /// 행사 유형 (예: "[시식]", "[판촉]")
  final String eventType;

  /// 행사명
  final String eventName;

  /// 행사 시작일
  final DateTime startDate;

  /// 행사 종료일
  final DateTime endDate;

  /// 거래처 ID
  final String customerId;

  /// 거래처명
  final String customerName;

  /// 담당자 사번
  final String assigneeId;

  /// 대표 제품
  final EventProduct? mainProduct;

  /// 기타 제품 목록
  final List<EventProduct> subProducts;

  const Event({
    required this.id,
    required this.eventType,
    required this.eventName,
    required this.startDate,
    required this.endDate,
    required this.customerId,
    required this.customerName,
    required this.assigneeId,
    this.mainProduct,
    this.subProducts = const [],
  });

  Event copyWith({
    String? id,
    String? eventType,
    String? eventName,
    DateTime? startDate,
    DateTime? endDate,
    String? customerId,
    String? customerName,
    String? assigneeId,
    EventProduct? mainProduct,
    List<EventProduct>? subProducts,
  }) {
    return Event(
      id: id ?? this.id,
      eventType: eventType ?? this.eventType,
      eventName: eventName ?? this.eventName,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      customerId: customerId ?? this.customerId,
      customerName: customerName ?? this.customerName,
      assigneeId: assigneeId ?? this.assigneeId,
      mainProduct: mainProduct ?? this.mainProduct,
      subProducts: subProducts ?? this.subProducts,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'eventType': eventType,
      'eventName': eventName,
      'startDate': startDate.toIso8601String(),
      'endDate': endDate.toIso8601String(),
      'customerId': customerId,
      'customerName': customerName,
      'assigneeId': assigneeId,
      'mainProduct': mainProduct?.toJson(),
      'subProducts': subProducts.map((p) => p.toJson()).toList(),
    };
  }

  factory Event.fromJson(Map<String, dynamic> json) {
    return Event(
      id: json['id'] as String,
      eventType: json['eventType'] as String,
      eventName: json['eventName'] as String,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      customerId: json['customerId'] as String,
      customerName: json['customerName'] as String,
      assigneeId: json['assigneeId'] as String,
      mainProduct: json['mainProduct'] != null
          ? EventProduct.fromJson(json['mainProduct'] as Map<String, dynamic>)
          : null,
      subProducts: json['subProducts'] != null
          ? (json['subProducts'] as List)
              .map((item) => EventProduct.fromJson(item as Map<String, dynamic>))
              .toList()
          : [],
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Event &&
        other.id == id &&
        other.eventType == eventType &&
        other.eventName == eventName &&
        other.startDate == startDate &&
        other.endDate == endDate &&
        other.customerId == customerId &&
        other.customerName == customerName &&
        other.assigneeId == assigneeId &&
        other.mainProduct == mainProduct &&
        _listEquals(other.subProducts, subProducts);
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      eventType,
      eventName,
      startDate,
      endDate,
      customerId,
      customerName,
      assigneeId,
      mainProduct,
      Object.hashAll(subProducts),
    );
  }

  @override
  String toString() {
    return 'Event(id: $id, eventType: $eventType, eventName: $eventName, '
        'startDate: $startDate, endDate: $endDate, customerId: $customerId, '
        'customerName: $customerName, assigneeId: $assigneeId, '
        'mainProduct: $mainProduct, subProducts: $subProducts)';
  }

  /// List equality helper
  bool _listEquals<T>(List<T>? a, List<T>? b) {
    if (a == null) return b == null;
    if (b == null || a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
