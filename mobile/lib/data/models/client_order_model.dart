import '../../domain/entities/client_order.dart';
import '../../domain/entities/order_detail.dart';

/// 거래처별 주문 목록 항목 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ClientOrder 엔티티로 변환합니다.
class ClientOrderModel {
  final String sapOrderNumber;
  final int clientId;
  final String clientName;
  final int totalAmount;

  const ClientOrderModel({
    required this.sapOrderNumber,
    required this.clientId,
    required this.clientName,
    required this.totalAmount,
  });

  /// snake_case JSON에서 파싱
  factory ClientOrderModel.fromJson(Map<String, dynamic> json) {
    return ClientOrderModel(
      sapOrderNumber: json['sap_order_number'] as String,
      clientId: json['client_id'] as int,
      clientName: json['client_name'] as String,
      totalAmount: json['total_amount'] as int,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'sap_order_number': sapOrderNumber,
      'client_id': clientId,
      'client_name': clientName,
      'total_amount': totalAmount,
    };
  }

  /// Domain Entity로 변환
  ClientOrder toEntity() {
    return ClientOrder(
      sapOrderNumber: sapOrderNumber,
      clientId: clientId,
      clientName: clientName,
      totalAmount: totalAmount,
    );
  }

  /// Domain Entity에서 생성
  factory ClientOrderModel.fromEntity(ClientOrder entity) {
    return ClientOrderModel(
      sapOrderNumber: entity.sapOrderNumber,
      clientId: entity.clientId,
      clientName: entity.clientName,
      totalAmount: entity.totalAmount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrderModel &&
        other.sapOrderNumber == sapOrderNumber &&
        other.clientId == clientId &&
        other.clientName == clientName &&
        other.totalAmount == totalAmount;
  }

  @override
  int get hashCode {
    return Object.hash(sapOrderNumber, clientId, clientName, totalAmount);
  }

  @override
  String toString() {
    return 'ClientOrderModel(sapOrderNumber: $sapOrderNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'totalAmount: $totalAmount)';
  }
}

/// 거래처별 주문 제품 API 모델 (DTO)
class ClientOrderItemModel {
  final String productCode;
  final String productName;
  final String deliveredQuantity;
  final String deliveryStatus;

  const ClientOrderItemModel({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.deliveryStatus,
  });

  factory ClientOrderItemModel.fromJson(Map<String, dynamic> json) {
    return ClientOrderItemModel(
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      deliveredQuantity: json['delivered_quantity'] as String,
      deliveryStatus: json['delivery_status'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'product_code': productCode,
      'product_name': productName,
      'delivered_quantity': deliveredQuantity,
      'delivery_status': deliveryStatus,
    };
  }

  ClientOrderItem toEntity() {
    return ClientOrderItem(
      productCode: productCode,
      productName: productName,
      deliveredQuantity: deliveredQuantity,
      deliveryStatus: DeliveryStatus.fromCode(deliveryStatus),
    );
  }

  factory ClientOrderItemModel.fromEntity(ClientOrderItem entity) {
    return ClientOrderItemModel(
      productCode: entity.productCode,
      productName: entity.productName,
      deliveredQuantity: entity.deliveredQuantity,
      deliveryStatus: entity.deliveryStatus.code,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrderItemModel &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.deliveredQuantity == deliveredQuantity &&
        other.deliveryStatus == deliveryStatus;
  }

  @override
  int get hashCode {
    return Object.hash(productCode, productName, deliveredQuantity,
        deliveryStatus);
  }

  @override
  String toString() {
    return 'ClientOrderItemModel(productCode: $productCode, '
        'productName: $productName, '
        'deliveredQuantity: $deliveredQuantity, '
        'deliveryStatus: $deliveryStatus)';
  }
}

/// 거래처별 주문 상세 API 모델 (DTO)
class ClientOrderDetailModel {
  final String sapOrderNumber;
  final int clientId;
  final String clientName;
  final String? clientDeadlineTime;
  final String orderDate;
  final String deliveryDate;
  final int totalApprovedAmount;
  final int orderedItemCount;
  final List<ClientOrderItemModel> orderedItems;

  const ClientOrderDetailModel({
    required this.sapOrderNumber,
    required this.clientId,
    required this.clientName,
    this.clientDeadlineTime,
    required this.orderDate,
    required this.deliveryDate,
    required this.totalApprovedAmount,
    required this.orderedItemCount,
    required this.orderedItems,
  });

  factory ClientOrderDetailModel.fromJson(Map<String, dynamic> json) {
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;
    final itemsJson = data['ordered_items'] as List<dynamic>? ?? [];

    return ClientOrderDetailModel(
      sapOrderNumber: data['sap_order_number'] as String,
      clientId: data['client_id'] as int,
      clientName: data['client_name'] as String,
      clientDeadlineTime: data['client_deadline_time'] as String?,
      orderDate: data['order_date'] as String,
      deliveryDate: data['delivery_date'] as String,
      totalApprovedAmount: data['total_approved_amount'] as int,
      orderedItemCount: data['ordered_item_count'] as int,
      orderedItems: itemsJson
          .map((e) =>
              ClientOrderItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sap_order_number': sapOrderNumber,
      'client_id': clientId,
      'client_name': clientName,
      'client_deadline_time': clientDeadlineTime,
      'order_date': orderDate,
      'delivery_date': deliveryDate,
      'total_approved_amount': totalApprovedAmount,
      'ordered_item_count': orderedItemCount,
      'ordered_items': orderedItems.map((e) => e.toJson()).toList(),
    };
  }

  ClientOrderDetail toEntity() {
    return ClientOrderDetail(
      sapOrderNumber: sapOrderNumber,
      clientId: clientId,
      clientName: clientName,
      clientDeadlineTime: clientDeadlineTime,
      orderDate: DateTime.parse(orderDate),
      deliveryDate: DateTime.parse(deliveryDate),
      totalApprovedAmount: totalApprovedAmount,
      orderedItemCount: orderedItemCount,
      orderedItems: orderedItems.map((e) => e.toEntity()).toList(),
    );
  }

  factory ClientOrderDetailModel.fromEntity(ClientOrderDetail entity) {
    return ClientOrderDetailModel(
      sapOrderNumber: entity.sapOrderNumber,
      clientId: entity.clientId,
      clientName: entity.clientName,
      clientDeadlineTime: entity.clientDeadlineTime,
      orderDate: entity.orderDate.toIso8601String().split('T')[0],
      deliveryDate: entity.deliveryDate.toIso8601String().split('T')[0],
      totalApprovedAmount: entity.totalApprovedAmount,
      orderedItemCount: entity.orderedItemCount,
      orderedItems: entity.orderedItems
          .map((e) => ClientOrderItemModel.fromEntity(e))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClientOrderDetailModel) return false;
    if (other.sapOrderNumber != sapOrderNumber) return false;
    if (other.clientId != clientId) return false;
    if (other.clientName != clientName) return false;
    if (other.clientDeadlineTime != clientDeadlineTime) return false;
    if (other.orderDate != orderDate) return false;
    if (other.deliveryDate != deliveryDate) return false;
    if (other.totalApprovedAmount != totalApprovedAmount) return false;
    if (other.orderedItemCount != orderedItemCount) return false;
    if (other.orderedItems.length != orderedItems.length) return false;
    for (var i = 0; i < orderedItems.length; i++) {
      if (other.orderedItems[i] != orderedItems[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      sapOrderNumber,
      clientId,
      clientName,
      clientDeadlineTime,
      orderDate,
      deliveryDate,
      totalApprovedAmount,
      orderedItemCount,
      Object.hashAll(orderedItems),
    );
  }

  @override
  String toString() {
    return 'ClientOrderDetailModel(sapOrderNumber: $sapOrderNumber, '
        'clientName: $clientName, '
        'orderedItemCount: $orderedItemCount)';
  }
}

/// 거래처별 주문 목록 API 응답 모델
///
/// 페이지네이션을 포함한 거래처별 주문 목록 응답을 파싱합니다.
class ClientOrderListResponseModel {
  final List<ClientOrderModel> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  final bool first;
  final bool last;

  const ClientOrderListResponseModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.first,
    required this.last,
  });

  /// API 응답 JSON에서 파싱
  factory ClientOrderListResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final contentJson = data['content'] as List<dynamic>? ?? [];
    final content = contentJson
        .map((e) => ClientOrderModel.fromJson(e as Map<String, dynamic>))
        .toList();

    return ClientOrderListResponseModel(
      content: content,
      totalElements: data['total_elements'] as int,
      totalPages: data['total_pages'] as int,
      number: data['number'] as int,
      size: data['size'] as int,
      first: data['first'] as bool,
      last: data['last'] as bool,
    );
  }
}
