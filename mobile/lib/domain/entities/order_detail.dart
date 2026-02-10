import 'order.dart';

/// 배송 상태 열거형
///
/// 주문 처리 현황에서 각 제품의 배송 상태를 나타냅니다.
enum DeliveryStatus {
  waiting('대기', 'WAITING'),
  shipping('배송중', 'SHIPPING'),
  delivered('배송완료', 'DELIVERED');

  const DeliveryStatus(this.displayName, this.code);

  /// 화면에 표시되는 이름
  final String displayName;

  /// API 코드 값
  final String code;

  /// API 코드에서 DeliveryStatus로 변환
  static DeliveryStatus fromCode(String code) {
    return DeliveryStatus.values.firstWhere(
      (status) => status.code == code,
      orElse: () => DeliveryStatus.waiting,
    );
  }

  String toJson() => code;
  static DeliveryStatus fromJson(String json) => fromCode(json);
}

/// 주문한 제품 엔티티
///
/// 주문 상세의 제품 목록에 표시되는 개별 제품 정보입니다.
class OrderedItem {
  /// 제품 코드 (예: 01101123)
  final String productCode;

  /// 제품명 (예: 갈릭 아이올리소스 240g)
  final String productName;

  /// 총 주문수량 - 박스 단위 (예: 5, 60.5)
  final double totalQuantityBoxes;

  /// 총 주문수량 - 낱개 단위 (예: 100, 1150)
  final int totalQuantityPieces;

  /// 취소 여부
  final bool isCancelled;

  const OrderedItem({
    required this.productCode,
    required this.productName,
    required this.totalQuantityBoxes,
    required this.totalQuantityPieces,
    required this.isCancelled,
  });

  OrderedItem copyWith({
    String? productCode,
    String? productName,
    double? totalQuantityBoxes,
    int? totalQuantityPieces,
    bool? isCancelled,
  }) {
    return OrderedItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      totalQuantityBoxes: totalQuantityBoxes ?? this.totalQuantityBoxes,
      totalQuantityPieces: totalQuantityPieces ?? this.totalQuantityPieces,
      isCancelled: isCancelled ?? this.isCancelled,
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

  factory OrderedItem.fromJson(Map<String, dynamic> json) {
    return OrderedItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      totalQuantityBoxes: (json['totalQuantityBoxes'] as num).toDouble(),
      totalQuantityPieces: json['totalQuantityPieces'] as int,
      isCancelled: json['isCancelled'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderedItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.totalQuantityBoxes == totalQuantityBoxes &&
        other.totalQuantityPieces == totalQuantityPieces &&
        other.isCancelled == isCancelled;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      totalQuantityBoxes,
      totalQuantityPieces,
      isCancelled,
    );
  }

  @override
  String toString() {
    return 'OrderedItem(productCode: $productCode, productName: $productName, '
        'totalQuantityBoxes: $totalQuantityBoxes, '
        'totalQuantityPieces: $totalQuantityPieces, '
        'isCancelled: $isCancelled)';
  }
}

/// 처리 항목 엔티티
///
/// 주문 처리 현황의 개별 항목 (SAP 주문번호 하위의 제품별 처리 상태)
class ProcessingItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 납품 수량 (예: "0 EA")
  final String deliveredQuantity;

  /// 배송 상태
  final DeliveryStatus deliveryStatus;

  const ProcessingItem({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.deliveryStatus,
  });

  ProcessingItem copyWith({
    String? productCode,
    String? productName,
    String? deliveredQuantity,
    DeliveryStatus? deliveryStatus,
  }) {
    return ProcessingItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      deliveredQuantity: deliveredQuantity ?? this.deliveredQuantity,
      deliveryStatus: deliveryStatus ?? this.deliveryStatus,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'deliveredQuantity': deliveredQuantity,
      'deliveryStatus': deliveryStatus.code,
    };
  }

  factory ProcessingItem.fromJson(Map<String, dynamic> json) {
    return ProcessingItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      deliveredQuantity: json['deliveredQuantity'] as String,
      deliveryStatus:
          DeliveryStatus.fromCode(json['deliveryStatus'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ProcessingItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.deliveredQuantity == deliveredQuantity &&
        other.deliveryStatus == deliveryStatus;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      deliveredQuantity,
      deliveryStatus,
    );
  }

  @override
  String toString() {
    return 'ProcessingItem(productCode: $productCode, '
        'productName: $productName, '
        'deliveredQuantity: $deliveredQuantity, '
        'deliveryStatus: $deliveryStatus)';
  }
}

/// 주문 처리 현황 엔티티
///
/// SAP 주문번호별 처리 현황 정보입니다. 마감후 화면에서 사용됩니다.
class OrderProcessingStatus {
  /// SAP 주문번호 (예: 0300013650)
  final String sapOrderNumber;

  /// 처리 항목 목록
  final List<ProcessingItem> items;

  const OrderProcessingStatus({
    required this.sapOrderNumber,
    required this.items,
  });

  OrderProcessingStatus copyWith({
    String? sapOrderNumber,
    List<ProcessingItem>? items,
  }) {
    return OrderProcessingStatus(
      sapOrderNumber: sapOrderNumber ?? this.sapOrderNumber,
      items: items ?? this.items,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'items': items.map((e) => e.toJson()).toList(),
    };
  }

  factory OrderProcessingStatus.fromJson(Map<String, dynamic> json) {
    return OrderProcessingStatus(
      sapOrderNumber: json['sapOrderNumber'] as String,
      items: (json['items'] as List<dynamic>)
          .map((e) => ProcessingItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderProcessingStatus) return false;
    if (other.sapOrderNumber != sapOrderNumber) return false;
    if (other.items.length != items.length) return false;
    for (var i = 0; i < items.length; i++) {
      if (other.items[i] != items[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      sapOrderNumber,
      Object.hashAll(items),
    );
  }

  @override
  String toString() {
    return 'OrderProcessingStatus(sapOrderNumber: $sapOrderNumber, '
        'items: ${items.length})';
  }
}

/// 반려 제품 엔티티
///
/// 마감후 반려된 제품 정보입니다.
class RejectedItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 주문 수량 (BOX)
  final int orderQuantityBoxes;

  /// 반려 사유
  final String rejectionReason;

  const RejectedItem({
    required this.productCode,
    required this.productName,
    required this.orderQuantityBoxes,
    required this.rejectionReason,
  });

  RejectedItem copyWith({
    String? productCode,
    String? productName,
    int? orderQuantityBoxes,
    String? rejectionReason,
  }) {
    return RejectedItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      orderQuantityBoxes: orderQuantityBoxes ?? this.orderQuantityBoxes,
      rejectionReason: rejectionReason ?? this.rejectionReason,
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

  factory RejectedItem.fromJson(Map<String, dynamic> json) {
    return RejectedItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      orderQuantityBoxes: json['orderQuantityBoxes'] as int,
      rejectionReason: json['rejectionReason'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is RejectedItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.orderQuantityBoxes == orderQuantityBoxes &&
        other.rejectionReason == rejectionReason;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      orderQuantityBoxes,
      rejectionReason,
    );
  }

  @override
  String toString() {
    return 'RejectedItem(productCode: $productCode, '
        'productName: $productName, '
        'orderQuantityBoxes: $orderQuantityBoxes, '
        'rejectionReason: $rejectionReason)';
  }
}

/// 주문 상세 엔티티
///
/// 주문 상세 화면에 표시되는 전체 정보를 담는 도메인 엔티티입니다.
/// 마감 상태와 반려 여부에 따라 3가지 화면 구성이 동적으로 결정됩니다.
class OrderDetail {
  /// 주문 고유 ID
  final int id;

  /// 주문 요청번호 (예: OP00000001)
  final String orderRequestNumber;

  /// 거래처 ID
  final int clientId;

  /// 거래처명
  final String clientName;

  /// 거래처 마감시간 (HH:mm) — nullable
  final String? clientDeadlineTime;

  /// 주문일
  final DateTime orderDate;

  /// 납기일
  final DateTime deliveryDate;

  /// 총 주문금액 (원)
  final int totalAmount;

  /// 총 승인금액 (원) — 마감후에만 의미
  final int? totalApprovedAmount;

  /// 승인상태
  final ApprovalStatus approvalStatus;

  /// 마감 여부
  final bool isClosed;

  /// 주문한 제품 수
  final int orderedItemCount;

  /// 주문한 제품 목록
  final List<OrderedItem> orderedItems;

  /// 주문 처리 현황 (마감후에만)
  final OrderProcessingStatus? orderProcessingStatus;

  /// 반려 제품 목록 (마감후, 반려 존재 시)
  final List<RejectedItem>? rejectedItems;

  const OrderDetail({
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

  /// 반려 제품이 있는지 여부
  bool get hasRejectedItems =>
      rejectedItems != null && rejectedItems!.isNotEmpty;

  /// 모든 제품이 취소되었는지 여부
  bool get allItemsCancelled =>
      orderedItems.isNotEmpty && orderedItems.every((item) => item.isCancelled);

  OrderDetail copyWith({
    int? id,
    String? orderRequestNumber,
    int? clientId,
    String? clientName,
    String? clientDeadlineTime,
    DateTime? orderDate,
    DateTime? deliveryDate,
    int? totalAmount,
    int? totalApprovedAmount,
    ApprovalStatus? approvalStatus,
    bool? isClosed,
    int? orderedItemCount,
    List<OrderedItem>? orderedItems,
    OrderProcessingStatus? orderProcessingStatus,
    List<RejectedItem>? rejectedItems,
  }) {
    return OrderDetail(
      id: id ?? this.id,
      orderRequestNumber: orderRequestNumber ?? this.orderRequestNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      clientDeadlineTime: clientDeadlineTime ?? this.clientDeadlineTime,
      orderDate: orderDate ?? this.orderDate,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      totalAmount: totalAmount ?? this.totalAmount,
      totalApprovedAmount: totalApprovedAmount ?? this.totalApprovedAmount,
      approvalStatus: approvalStatus ?? this.approvalStatus,
      isClosed: isClosed ?? this.isClosed,
      orderedItemCount: orderedItemCount ?? this.orderedItemCount,
      orderedItems: orderedItems ?? this.orderedItems,
      orderProcessingStatus:
          orderProcessingStatus ?? this.orderProcessingStatus,
      rejectedItems: rejectedItems ?? this.rejectedItems,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'orderRequestNumber': orderRequestNumber,
      'clientId': clientId,
      'clientName': clientName,
      'clientDeadlineTime': clientDeadlineTime,
      'orderDate': orderDate.toIso8601String(),
      'deliveryDate': deliveryDate.toIso8601String(),
      'totalAmount': totalAmount,
      'totalApprovedAmount': totalApprovedAmount,
      'approvalStatus': approvalStatus.code,
      'isClosed': isClosed,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
      'orderProcessingStatus': orderProcessingStatus?.toJson(),
      'rejectedItems': rejectedItems?.map((e) => e.toJson()).toList(),
    };
  }

  factory OrderDetail.fromJson(Map<String, dynamic> json) {
    return OrderDetail(
      id: json['id'] as int,
      orderRequestNumber: json['orderRequestNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      clientDeadlineTime: json['clientDeadlineTime'] as String?,
      orderDate: DateTime.parse(json['orderDate'] as String),
      deliveryDate: DateTime.parse(json['deliveryDate'] as String),
      totalAmount: json['totalAmount'] as int,
      totalApprovedAmount: json['totalApprovedAmount'] as int?,
      approvalStatus:
          ApprovalStatus.fromCode(json['approvalStatus'] as String),
      isClosed: json['isClosed'] as bool,
      orderedItemCount: json['orderedItemCount'] as int,
      orderedItems: (json['orderedItems'] as List<dynamic>)
          .map((e) => OrderedItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      orderProcessingStatus: json['orderProcessingStatus'] != null
          ? OrderProcessingStatus.fromJson(
              json['orderProcessingStatus'] as Map<String, dynamic>)
          : null,
      rejectedItems: json['rejectedItems'] != null
          ? (json['rejectedItems'] as List<dynamic>)
              .map((e) => RejectedItem.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderDetail) return false;
    if (other.id != id) return false;
    if (other.orderRequestNumber != orderRequestNumber) return false;
    if (other.clientId != clientId) return false;
    if (other.clientName != clientName) return false;
    if (other.clientDeadlineTime != clientDeadlineTime) return false;
    if (other.orderDate != orderDate) return false;
    if (other.deliveryDate != deliveryDate) return false;
    if (other.totalAmount != totalAmount) return false;
    if (other.totalApprovedAmount != totalApprovedAmount) return false;
    if (other.approvalStatus != approvalStatus) return false;
    if (other.isClosed != isClosed) return false;
    if (other.orderedItemCount != orderedItemCount) return false;
    if (other.orderedItems.length != orderedItems.length) return false;
    for (var i = 0; i < orderedItems.length; i++) {
      if (other.orderedItems[i] != orderedItems[i]) return false;
    }
    if (other.orderProcessingStatus != orderProcessingStatus) return false;
    if (other.hasRejectedItems != hasRejectedItems) return false;
    if (hasRejectedItems) {
      if (other.rejectedItems!.length != rejectedItems!.length) return false;
      for (var i = 0; i < rejectedItems!.length; i++) {
        if (other.rejectedItems![i] != rejectedItems![i]) return false;
      }
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      orderRequestNumber,
      clientId,
      clientName,
      clientDeadlineTime,
      orderDate,
      deliveryDate,
      totalAmount,
      totalApprovedAmount,
      approvalStatus,
      isClosed,
      orderedItemCount,
      Object.hashAll(orderedItems),
      orderProcessingStatus,
      rejectedItems != null ? Object.hashAll(rejectedItems!) : null,
    );
  }

  @override
  String toString() {
    return 'OrderDetail(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientName: $clientName, isClosed: $isClosed, '
        'orderedItemCount: $orderedItemCount, '
        'hasRejectedItems: $hasRejectedItems)';
  }
}
