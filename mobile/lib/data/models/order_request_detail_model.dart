import '../../domain/entities/order_detail.dart';

/// 주문한 제품 API 모델 (DTO)
class OrderedItemModel {
  final int orderProductId;
  final String productCode;
  final String productName;
  final double totalQuantityBoxes;
  final int totalQuantityPieces;
  final bool isCancelled;
  final bool isOutOfStock;
  final String? outOfStockReason;

  const OrderedItemModel({
    required this.orderProductId,
    required this.productCode,
    required this.productName,
    required this.totalQuantityBoxes,
    required this.totalQuantityPieces,
    required this.isCancelled,
    this.isOutOfStock = false,
    this.outOfStockReason,
  });

  factory OrderedItemModel.fromJson(Map<String, dynamic> json) {
    return OrderedItemModel(
      orderProductId: (json['orderProductId'] as num).toInt(),
      productCode: json['productCode'] as String,
      productName: json['productName'] as String? ?? '',
      totalQuantityBoxes: (json['totalQuantityBoxes'] as num).toDouble(),
      totalQuantityPieces: (json['totalQuantityPieces'] as num).toInt(),
      isCancelled: json['isCancelled'] as bool,
      isOutOfStock: json['isOutOfStock'] as bool? ?? false,
      outOfStockReason: json['outOfStockReason'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'orderProductId': orderProductId,
      'productCode': productCode,
      'productName': productName,
      'totalQuantityBoxes': totalQuantityBoxes,
      'totalQuantityPieces': totalQuantityPieces,
      'isCancelled': isCancelled,
      'isOutOfStock': isOutOfStock,
      'outOfStockReason': outOfStockReason,
    };
  }

  OrderedItem toEntity() {
    return OrderedItem(
      orderProductId: orderProductId,
      productCode: productCode,
      productName: productName,
      totalQuantityBoxes: totalQuantityBoxes,
      totalQuantityPieces: totalQuantityPieces,
      isCancelled: isCancelled,
      isOutOfStock: isOutOfStock,
      outOfStockReason: outOfStockReason,
    );
  }

  factory OrderedItemModel.fromEntity(OrderedItem entity) {
    return OrderedItemModel(
      orderProductId: entity.orderProductId,
      productCode: entity.productCode,
      productName: entity.productName,
      totalQuantityBoxes: entity.totalQuantityBoxes,
      totalQuantityPieces: entity.totalQuantityPieces,
      isCancelled: entity.isCancelled,
      isOutOfStock: entity.isOutOfStock,
      outOfStockReason: entity.outOfStockReason,
    );
  }
}

/// 처리 항목 API 모델 (DTO).
///
/// 차량/기사 5필드 (Q5) — `SHIPPING`/`DELIVERED` 라인 탭 팝업용. 시각 필드(`scheduleTime`/
/// `completeTime`)는 레거시 동등으로 SAP 응답 `HHmmss` 문자열을 **무가공** 으로 수신한다
/// (`'000000'` sentinel 도 그대로). 빈 문자열만 서버가 `null` 로 매핑한다.
class ProcessingItemModel {
  final String productCode;
  final String productName;
  final String deliveredQuantity;
  final String deliveryStatus;
  final String? driverName;
  final String? vehicle;
  final String? driverPhone;
  final String? scheduleTime;
  final String? completeTime;

  const ProcessingItemModel({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.deliveryStatus,
    this.driverName,
    this.vehicle,
    this.driverPhone,
    this.scheduleTime,
    this.completeTime,
  });

  factory ProcessingItemModel.fromJson(Map<String, dynamic> json) {
    return ProcessingItemModel(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      deliveredQuantity: json['deliveredQuantity'] as String,
      deliveryStatus: json['deliveryStatus'] as String,
      driverName: json['driverName'] as String?,
      vehicle: json['vehicle'] as String?,
      driverPhone: json['driverPhone'] as String?,
      scheduleTime: json['scheduleTime'] as String?,
      completeTime: json['completeTime'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'deliveredQuantity': deliveredQuantity,
      'deliveryStatus': deliveryStatus,
      'driverName': driverName,
      'vehicle': vehicle,
      'driverPhone': driverPhone,
      'scheduleTime': scheduleTime,
      'completeTime': completeTime,
    };
  }

  ProcessingItem toEntity() {
    return ProcessingItem(
      productCode: productCode,
      productName: productName,
      deliveredQuantity: deliveredQuantity,
      deliveryStatus: DeliveryStatus.fromCode(deliveryStatus),
      driverName: driverName,
      vehicle: vehicle,
      driverPhone: driverPhone,
      scheduleTime: scheduleTime,
      completeTime: completeTime,
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
      orderQuantityBoxes: (json['orderQuantityBoxes'] as num).toInt(),
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
class OrderRequestDetailModel {
  final int id;
  final String orderRequestNumber;
  final int clientId;
  final String clientName;
  final String? clientDeadlineTime;
  final String orderDate;
  final String deliveryDate;
  final int totalAmount;
  final int? totalApprovedAmount;
  final String orderRequestStatus;
  final String orderRequestStatusName;
  final bool isClosed;
  final bool cancelable;
  final bool registrationInFlight;
  final int orderedItemCount;
  final List<OrderedItemModel> orderedItems;

  /// SAP 주문번호별 그룹 배열 (Spec #595 Q1 옵션 2). SAP 호출 실패 또는 마감 전 시 null.
  final List<OrderProcessingStatusModel>? orderProcessingStatusList;
  final List<RejectedItemModel>? rejectedItems;

  const OrderRequestDetailModel({
    required this.id,
    required this.orderRequestNumber,
    required this.clientId,
    required this.clientName,
    this.clientDeadlineTime,
    required this.orderDate,
    required this.deliveryDate,
    required this.totalAmount,
    this.totalApprovedAmount,
    required this.orderRequestStatus,
    required this.orderRequestStatusName,
    required this.isClosed,
    this.cancelable = false,
    this.registrationInFlight = false,
    required this.orderedItemCount,
    required this.orderedItems,
    this.orderProcessingStatusList,
    this.rejectedItems,
  });

  /// snake_case JSON에서 파싱
  factory OrderRequestDetailModel.fromJson(Map<String, dynamic> json) {
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;

    final orderedItemsJson = data['orderedItems'] as List<dynamic>? ?? [];
    final rejectedItemsJson = data['rejectedItems'] as List<dynamic>?;
    final processingListJson =
        data['orderProcessingStatusList'] as List<dynamic>?;

    return OrderRequestDetailModel(
      id: data['id'] as int,
      orderRequestNumber: data['orderRequestNumber'] as String,
      clientId: data['clientId'] as int,
      clientName: data['clientName'] as String,
      clientDeadlineTime: data['clientDeadlineTime'] as String?,
      orderDate: data['orderDate'] as String,
      deliveryDate: data['deliveryDate'] as String,
      totalAmount: (data['totalAmount'] as num).toInt(),
      totalApprovedAmount: (data['totalApprovedAmount'] as num?)?.toInt(),
      orderRequestStatus: data['orderRequestStatus'] as String,
      orderRequestStatusName: data['orderRequestStatusName'] as String,
      isClosed: data['isClosed'] as bool,
      cancelable: data['cancelable'] as bool? ?? false,
      registrationInFlight: data['registrationInFlight'] as bool? ?? false,
      orderedItemCount: (data['orderedItemCount'] as num).toInt(),
      orderedItems: orderedItemsJson
          .map((e) => OrderedItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      orderProcessingStatusList: processingListJson != null
          ? processingListJson
              .map((e) => OrderProcessingStatusModel.fromJson(
                  e as Map<String, dynamic>))
              .toList()
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
      'orderRequestStatus': orderRequestStatus,
      'orderRequestStatusName': orderRequestStatusName,
      'isClosed': isClosed,
      'cancelable': cancelable,
      'registrationInFlight': registrationInFlight,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
      'orderProcessingStatusList':
          orderProcessingStatusList?.map((e) => e.toJson()).toList(),
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
      orderRequestStatus: orderRequestStatus,
      orderRequestStatusName: orderRequestStatusName,
      isClosed: isClosed,
      cancelable: cancelable,
      registrationInFlight: registrationInFlight,
      orderedItemCount: orderedItemCount,
      orderedItems: orderedItems.map((e) => e.toEntity()).toList(),
      orderProcessingStatusList:
          orderProcessingStatusList?.map((e) => e.toEntity()).toList(),
      rejectedItems: rejectedItems?.map((e) => e.toEntity()).toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderRequestDetailModel &&
        other.id == id &&
        other.orderRequestNumber == orderRequestNumber;
  }

  @override
  int get hashCode => Object.hash(id, orderRequestNumber);

  @override
  String toString() {
    return 'OrderRequestDetailModel(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientName: $clientName, isClosed: $isClosed)';
  }
}
