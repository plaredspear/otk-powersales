import '../../domain/entities/event.dart';

/// EventProduct API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class EventProductModel {
  final String productCode;
  final String productName;
  final bool isMainProduct;

  const EventProductModel({
    required this.productCode,
    required this.productName,
    required this.isMainProduct,
  });

  factory EventProductModel.fromJson(Map<String, dynamic> json) {
    return EventProductModel(
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      isMainProduct: json['is_main_product'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'product_code': productCode,
      'product_name': productName,
      'is_main_product': isMainProduct,
    };
  }

  EventProduct toEntity() {
    return EventProduct(
      productCode: productCode,
      productName: productName,
      isMainProduct: isMainProduct,
    );
  }

  factory EventProductModel.fromEntity(EventProduct entity) {
    return EventProductModel(
      productCode: entity.productCode,
      productName: entity.productName,
      isMainProduct: entity.isMainProduct,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EventProductModel &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.isMainProduct == isMainProduct;
  }

  @override
  int get hashCode {
    return Object.hash(productCode, productName, isMainProduct);
  }

  @override
  String toString() {
    return 'EventProductModel(productCode: $productCode, productName: $productName, isMainProduct: $isMainProduct)';
  }
}

/// Event API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class EventModel {
  final String id;
  final String eventType;
  final String eventName;
  final String startDate;
  final String endDate;
  final String customerId;
  final String customerName;
  final String assigneeId;
  final EventProductModel? mainProduct;
  final List<EventProductModel> subProducts;

  const EventModel({
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

  factory EventModel.fromJson(Map<String, dynamic> json) {
    return EventModel(
      id: json['id'] as String,
      eventType: json['event_type'] as String,
      eventName: json['event_name'] as String,
      startDate: json['start_date'] as String,
      endDate: json['end_date'] as String,
      customerId: json['customer_id'] as String,
      customerName: json['customer_name'] as String,
      assigneeId: json['assignee_id'] as String,
      mainProduct: json['main_product'] != null
          ? EventProductModel.fromJson(json['main_product'] as Map<String, dynamic>)
          : null,
      subProducts: json['sub_products'] != null
          ? (json['sub_products'] as List<dynamic>)
              .map((item) => EventProductModel.fromJson(item as Map<String, dynamic>))
              .toList()
          : const [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'event_type': eventType,
      'event_name': eventName,
      'start_date': startDate,
      'end_date': endDate,
      'customer_id': customerId,
      'customer_name': customerName,
      'assignee_id': assigneeId,
      'main_product': mainProduct?.toJson(),
      'sub_products': subProducts.map((product) => product.toJson()).toList(),
    };
  }

  Event toEntity() {
    return Event(
      id: id,
      eventType: eventType,
      eventName: eventName,
      startDate: DateTime.parse(startDate),
      endDate: DateTime.parse(endDate),
      customerId: customerId,
      customerName: customerName,
      assigneeId: assigneeId,
      mainProduct: mainProduct?.toEntity(),
      subProducts: subProducts.map((model) => model.toEntity()).toList(),
    );
  }

  factory EventModel.fromEntity(Event entity) {
    return EventModel(
      id: entity.id,
      eventType: entity.eventType,
      eventName: entity.eventName,
      startDate: entity.startDate.toIso8601String(),
      endDate: entity.endDate.toIso8601String(),
      customerId: entity.customerId,
      customerName: entity.customerName,
      assigneeId: entity.assigneeId,
      mainProduct: entity.mainProduct != null
          ? EventProductModel.fromEntity(entity.mainProduct!)
          : null,
      subProducts: entity.subProducts
          .map((product) => EventProductModel.fromEntity(product))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EventModel &&
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
    return 'EventModel(id: $id, eventType: $eventType, eventName: $eventName, '
        'startDate: $startDate, endDate: $endDate, '
        'customerId: $customerId, customerName: $customerName, '
        'assigneeId: $assigneeId, mainProduct: $mainProduct, '
        'subProducts: ${subProducts.length} items)';
  }
}

/// List equality helper
bool _listEquals<T>(List<T> a, List<T> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
