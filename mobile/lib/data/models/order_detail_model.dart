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
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      totalQuantityBoxes: (json['totalQuantityBoxes'] as num).toDouble(),
      totalQuantityPieces: json['totalQuantityPieces'] as int,
      isCancelled: json['isCancelled'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'totalQuantityBoxes': totalQuantityBoxes,
      'totalQuantityPieces': totalQuantityPieces,
      'isCancelled': isCancelled,
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
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      deliveredQuantity: json['deliveredQuantity'] as String,
      deliveryStatus: json['deliveryStatus'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'deliveredQuantity': deliveredQuantity,
      'deliveryStatus': deliveryStatus,
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
      sapOrderNumber: json['sapOrderNumber'] as String,
      items: (json['items'] as List<dynamic>)
          .map((e) => ProcessingItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
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
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      orderQuantityBoxes: json['orderQuantityBoxes'] as int,
      rejectionReason: json['rejectionReason'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'orderQuantityBoxes': orderQuantityBoxes,
      'rejectionReason': rejectionReason,
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

    final orderedItemsJson = data['orderedItems'] as List<dynamic>? ?? [];
    final rejectedItemsJson = data['rejectedItems'] as List<dynamic>?;

    return OrderDetailModel(
      id: data['id'] as int,
      orderRequestNumber: data['orderRequestNumber'] as String,
      clientId: data['clientId'] as int,
      clientName: data['clientName'] as String,
      clientDeadlineTime: data['clientDeadlineTime'] as String?,
      orderDate: data['orderDate'] as String,
      deliveryDate: data['deliveryDate'] as String,
      totalAmount: data['totalAmount'] as int,
      totalApprovedAmount: data['totalApprovedAmount'] as int?,
      approvalStatus: data['approvalStatus'] as String,
      isClosed: data['isClosed'] as bool,
      orderedItemCount: data['orderedItemCount'] as int,
      orderedItems: orderedItemsJson
          .map((e) => OrderedItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      orderProcessingStatus: data['orderProcessingStatus'] != null
          ? OrderProcessingStatusModel.fromJson(
              data['orderProcessingStatus'] as Map<String, dynamic>)
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
      'orderRequestNumber': orderRequestNumber,
      'clientId': clientId,
      'clientName': clientName,
      'clientDeadlineTime': clientDeadlineTime,
      'orderDate': orderDate,
      'deliveryDate': deliveryDate,
      'totalAmount': totalAmount,
      'totalApprovedAmount': totalApprovedAmount,
      'approvalStatus': approvalStatus,
      'isClosed': isClosed,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
      'orderProcessingStatus': orderProcessingStatus?.toJson(),
      'rejectedItems': rejectedItems?.map((e) => e.toJson()).toList(),
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
