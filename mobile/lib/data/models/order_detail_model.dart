import '../../domain/entities/order.dart';
import '../../domain/entities/order_detail.dart';

/// 주문한 제품 API 모델 (DTO)
class OrderedItemModel {
  final String productCode;
  final String productName;
  final double totalQuantityBoxes;
  final int totalQuantityPieces;
  final bool isCancelled;

  const OrderedItemModel({
    required this.productCode,
    required this.productName,
    required this.totalQuantityBoxes,
    required this.totalQuantityPieces,
    required this.isCancelled,
  });

  factory OrderedItemModel.fromJson(Map<String, dynamic> json) {
    return OrderedItemModel(
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      totalQuantityBoxes: (json['total_quantity_boxes'] as num).toDouble(),
      totalQuantityPieces: json['total_quantity_pieces'] as int,
      isCancelled: json['is_cancelled'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'product_code': productCode,
      'product_name': productName,
      'total_quantity_boxes': totalQuantityBoxes,
      'total_quantity_pieces': totalQuantityPieces,
      'is_cancelled': isCancelled,
    };
  }

  OrderedItem toEntity() {
    return OrderedItem(
      productCode: productCode,
      productName: productName,
      totalQuantityBoxes: totalQuantityBoxes,
      totalQuantityPieces: totalQuantityPieces,
      isCancelled: isCancelled,
    );
  }

  factory OrderedItemModel.fromEntity(OrderedItem entity) {
    return OrderedItemModel(
      productCode: entity.productCode,
      productName: entity.productName,
      totalQuantityBoxes: entity.totalQuantityBoxes,
      totalQuantityPieces: entity.totalQuantityPieces,
      isCancelled: entity.isCancelled,
    );
  }
}

/// 처리 항목 API 모델 (DTO)
class ProcessingItemModel {
  final String productCode;
  final String productName;
  final String deliveredQuantity;
  final String deliveryStatus;

  const ProcessingItemModel({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.deliveryStatus,
  });

  factory ProcessingItemModel.fromJson(Map<String, dynamic> json) {
    return ProcessingItemModel(
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

  ProcessingItem toEntity() {
    return ProcessingItem(
      productCode: productCode,
      productName: productName,
      deliveredQuantity: deliveredQuantity,
      deliveryStatus: DeliveryStatus.fromCode(deliveryStatus),
    );
  }
}

/// 주문 처리 현황 API 모델 (DTO)
class OrderProcessingStatusModel {
  final String sapOrderNumber;
  final List<ProcessingItemModel> items;

  const OrderProcessingStatusModel({
    required this.sapOrderNumber,
    required this.items,
  });

  factory OrderProcessingStatusModel.fromJson(Map<String, dynamic> json) {
    return OrderProcessingStatusModel(
      sapOrderNumber: json['sap_order_number'] as String,
      items: (json['items'] as List<dynamic>)
          .map((e) => ProcessingItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sap_order_number': sapOrderNumber,
      'items': items.map((e) => e.toJson()).toList(),
    };
  }

  OrderProcessingStatus toEntity() {
    return OrderProcessingStatus(
      sapOrderNumber: sapOrderNumber,
      items: items.map((e) => e.toEntity()).toList(),
    );
  }
}

/// 반려 제품 API 모델 (DTO)
class RejectedItemModel {
  final String productCode;
  final String productName;
  final int orderQuantityBoxes;
  final String rejectionReason;

  const RejectedItemModel({
    required this.productCode,
    required this.productName,
    required this.orderQuantityBoxes,
    required this.rejectionReason,
  });

  factory RejectedItemModel.fromJson(Map<String, dynamic> json) {
    return RejectedItemModel(
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      orderQuantityBoxes: json['order_quantity_boxes'] as int,
      rejectionReason: json['rejection_reason'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'product_code': productCode,
      'product_name': productName,
      'order_quantity_boxes': orderQuantityBoxes,
      'rejection_reason': rejectionReason,
    };
  }

  RejectedItem toEntity() {
    return RejectedItem(
      productCode: productCode,
      productName: productName,
      orderQuantityBoxes: orderQuantityBoxes,
      rejectionReason: rejectionReason,
    );
  }
}

/// 주문 상세 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 OrderDetail 엔티티로 변환합니다.
class OrderDetailModel {
  final int id;
  final String orderRequestNumber;
  final int clientId;
  final String clientName;
  final String? clientDeadlineTime;
  final String orderDate;
  final String deliveryDate;
  final int totalAmount;
  final int? totalApprovedAmount;
  final String approvalStatus;
  final bool isClosed;
  final int orderedItemCount;
  final List<OrderedItemModel> orderedItems;
  final OrderProcessingStatusModel? orderProcessingStatus;
  final List<RejectedItemModel>? rejectedItems;

  const OrderDetailModel({
    required this.id,
    required this.orderRequestNumber,
    required this.clientId,
    required this.clientName,
    this.clientDeadlineTime,
    required this.orderDate,
    required this.deliveryDate,
    required this.totalAmount,
    this.totalApprovedAmount,
    required this.approvalStatus,
    required this.isClosed,
    required this.orderedItemCount,
    required this.orderedItems,
    this.orderProcessingStatus,
    this.rejectedItems,
  });

  /// snake_case JSON에서 파싱
  factory OrderDetailModel.fromJson(Map<String, dynamic> json) {
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;

    final orderedItemsJson = data['ordered_items'] as List<dynamic>? ?? [];
    final rejectedItemsJson = data['rejected_items'] as List<dynamic>?;

    return OrderDetailModel(
      id: data['id'] as int,
      orderRequestNumber: data['order_request_number'] as String,
      clientId: data['client_id'] as int,
      clientName: data['client_name'] as String,
      clientDeadlineTime: data['client_deadline_time'] as String?,
      orderDate: data['order_date'] as String,
      deliveryDate: data['delivery_date'] as String,
      totalAmount: data['total_amount'] as int,
      totalApprovedAmount: data['total_approved_amount'] as int?,
      approvalStatus: data['approval_status'] as String,
      isClosed: data['is_closed'] as bool,
      orderedItemCount: data['ordered_item_count'] as int,
      orderedItems: orderedItemsJson
          .map((e) => OrderedItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      orderProcessingStatus: data['order_processing_status'] != null
          ? OrderProcessingStatusModel.fromJson(
              data['order_processing_status'] as Map<String, dynamic>)
          : null,
      rejectedItems: rejectedItemsJson != null
          ? rejectedItemsJson
              .map(
                  (e) => RejectedItemModel.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'order_request_number': orderRequestNumber,
      'client_id': clientId,
      'client_name': clientName,
      'client_deadline_time': clientDeadlineTime,
      'order_date': orderDate,
      'delivery_date': deliveryDate,
      'total_amount': totalAmount,
      'total_approved_amount': totalApprovedAmount,
      'approval_status': approvalStatus,
      'is_closed': isClosed,
      'ordered_item_count': orderedItemCount,
      'ordered_items': orderedItems.map((e) => e.toJson()).toList(),
      'order_processing_status': orderProcessingStatus?.toJson(),
      'rejected_items': rejectedItems?.map((e) => e.toJson()).toList(),
    };
  }

  /// Domain Entity로 변환
  OrderDetail toEntity() {
    return OrderDetail(
      id: id,
      orderRequestNumber: orderRequestNumber,
      clientId: clientId,
      clientName: clientName,
      clientDeadlineTime: clientDeadlineTime,
      orderDate: DateTime.parse(orderDate),
      deliveryDate: DateTime.parse(deliveryDate),
      totalAmount: totalAmount,
      totalApprovedAmount: totalApprovedAmount,
      approvalStatus: ApprovalStatus.fromCode(approvalStatus),
      isClosed: isClosed,
      orderedItemCount: orderedItemCount,
      orderedItems: orderedItems.map((e) => e.toEntity()).toList(),
      orderProcessingStatus: orderProcessingStatus?.toEntity(),
      rejectedItems: rejectedItems?.map((e) => e.toEntity()).toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderDetailModel &&
        other.id == id &&
        other.orderRequestNumber == orderRequestNumber;
  }

  @override
  int get hashCode => Object.hash(id, orderRequestNumber);

  @override
  String toString() {
    return 'OrderDetailModel(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientName: $clientName, isClosed: $isClosed)';
  }
}
