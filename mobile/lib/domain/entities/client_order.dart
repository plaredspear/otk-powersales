import 'order_detail.dart';

/// 거래처별 주문 목록 항목 엔티티
///
/// 거래처별 주문 현황에 표시되는 개별 주문 정보입니다.
/// "내 주문"과 달리 SAP 주문번호를 기반으로 합니다.
class ClientOrder {
  /// SAP 주문번호 (예: 300011396)
  final String sapOrderNumber;

  /// 거래처 ID
  final int clientId;

  /// 거래처명
  final String clientName;

  /// 총 주문금액 (원)
  final int totalAmount;

  const ClientOrder({
    required this.sapOrderNumber,
    required this.clientId,
    required this.clientName,
    required this.totalAmount,
  });

  ClientOrder copyWith({
    String? sapOrderNumber,
    int? clientId,
    String? clientName,
    int? totalAmount,
  }) {
    return ClientOrder(
      sapOrderNumber: sapOrderNumber ?? this.sapOrderNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      totalAmount: totalAmount ?? this.totalAmount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'clientId': clientId,
      'clientName': clientName,
      'totalAmount': totalAmount,
    };
  }

  factory ClientOrder.fromJson(Map<String, dynamic> json) {
    return ClientOrder(
      sapOrderNumber: json['sapOrderNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      totalAmount: json['totalAmount'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrder &&
        other.sapOrderNumber == sapOrderNumber &&
        other.clientId == clientId &&
        other.clientName == clientName &&
        other.totalAmount == totalAmount;
  }

  @override
  int get hashCode {
    return Object.hash(
      sapOrderNumber,
      clientId,
      clientName,
      totalAmount,
    );
  }

  @override
  String toString() {
    return 'ClientOrder(sapOrderNumber: $sapOrderNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'totalAmount: $totalAmount)';
  }
}

/// 거래처별 주문 제품 엔티티
///
/// 거래처별 주문 상세의 제품 목록에 표시되는 개별 제품 정보입니다.
/// F18의 ProcessingItem과 유사하지만, 거래처별 주문 상세 전용입니다.
class ClientOrderItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 납품 수량 (예: "0 BOX")
  final String deliveredQuantity;

  /// 배송 상태
  final DeliveryStatus deliveryStatus;

  const ClientOrderItem({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.deliveryStatus,
  });

  ClientOrderItem copyWith({
    String? productCode,
    String? productName,
    String? deliveredQuantity,
    DeliveryStatus? deliveryStatus,
  }) {
    return ClientOrderItem(
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

  factory ClientOrderItem.fromJson(Map<String, dynamic> json) {
    return ClientOrderItem(
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
    return other is ClientOrderItem &&
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
    return 'ClientOrderItem(productCode: $productCode, '
        'productName: $productName, '
        'deliveredQuantity: $deliveredQuantity, '
        'deliveryStatus: $deliveryStatus)';
  }
}

/// 거래처별 주문 상세 엔티티
///
/// 거래처별 주문 상세 화면에 표시되는 전체 정보를 담는 도메인 엔티티입니다.
class ClientOrderDetail {
  /// SAP 주문번호
  final String sapOrderNumber;

  /// 거래처 ID
  final int clientId;

  /// 거래처명
  final String clientName;

  /// 거래처 마감시간 (HH:mm) - nullable
  final String? clientDeadlineTime;

  /// 주문일
  final DateTime orderDate;

  /// 납기일
  final DateTime deliveryDate;

  /// 총 승인 금액 (원)
  final int totalApprovedAmount;

  /// 주문한 제품 수
  final int orderedItemCount;

  /// 주문한 제품 목록
  final List<ClientOrderItem> orderedItems;

  const ClientOrderDetail({
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

  ClientOrderDetail copyWith({
    String? sapOrderNumber,
    int? clientId,
    String? clientName,
    String? clientDeadlineTime,
    DateTime? orderDate,
    DateTime? deliveryDate,
    int? totalApprovedAmount,
    int? orderedItemCount,
    List<ClientOrderItem>? orderedItems,
  }) {
    return ClientOrderDetail(
      sapOrderNumber: sapOrderNumber ?? this.sapOrderNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      clientDeadlineTime: clientDeadlineTime ?? this.clientDeadlineTime,
      orderDate: orderDate ?? this.orderDate,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      totalApprovedAmount: totalApprovedAmount ?? this.totalApprovedAmount,
      orderedItemCount: orderedItemCount ?? this.orderedItemCount,
      orderedItems: orderedItems ?? this.orderedItems,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'clientId': clientId,
      'clientName': clientName,
      'clientDeadlineTime': clientDeadlineTime,
      'orderDate': orderDate.toIso8601String(),
      'deliveryDate': deliveryDate.toIso8601String(),
      'totalApprovedAmount': totalApprovedAmount,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
    };
  }

  factory ClientOrderDetail.fromJson(Map<String, dynamic> json) {
    return ClientOrderDetail(
      sapOrderNumber: json['sapOrderNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      clientDeadlineTime: json['clientDeadlineTime'] as String?,
      orderDate: DateTime.parse(json['orderDate'] as String),
      deliveryDate: DateTime.parse(json['deliveryDate'] as String),
      totalApprovedAmount: json['totalApprovedAmount'] as int,
      orderedItemCount: json['orderedItemCount'] as int,
      orderedItems: (json['orderedItems'] as List<dynamic>)
          .map((e) => ClientOrderItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClientOrderDetail) return false;
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
    return 'ClientOrderDetail(sapOrderNumber: $sapOrderNumber, '
        'clientName: $clientName, '
        'orderedItemCount: $orderedItemCount)';
  }
}

/// 거래처별 주문 목록 조회 결과 값 객체
///
/// 페이지네이션 정보를 포함한 거래처별 주문 목록 결과입니다.
class ClientOrderListResult {
  /// 거래처별 주문 목록
  final List<ClientOrder> orders;

  /// 전체 결과 수
  final int totalElements;

  /// 전체 페이지 수
  final int totalPages;

  /// 현재 페이지 번호 (0부터 시작)
  final int currentPage;

  /// 페이지 크기
  final int pageSize;

  /// 첫 번째 페이지 여부
  final bool isFirst;

  /// 마지막 페이지 여부
  final bool isLast;

  const ClientOrderListResult({
    required this.orders,
    required this.totalElements,
    required this.totalPages,
    required this.currentPage,
    required this.pageSize,
    required this.isFirst,
    required this.isLast,
  });

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => !isLast;

  /// 이전 페이지가 있는지 여부
  bool get hasPreviousPage => !isFirst;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClientOrderListResult) return false;
    if (other.totalElements != totalElements) return false;
    if (other.totalPages != totalPages) return false;
    if (other.currentPage != currentPage) return false;
    if (other.pageSize != pageSize) return false;
    if (other.isFirst != isFirst) return false;
    if (other.isLast != isLast) return false;
    if (other.orders.length != orders.length) return false;
    for (var i = 0; i < orders.length; i++) {
      if (other.orders[i] != orders[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(orders),
      totalElements,
      totalPages,
      currentPage,
      pageSize,
      isFirst,
      isLast,
    );
  }

  @override
  String toString() {
    return 'ClientOrderListResult(orders: ${orders.length}, '
        'totalElements: $totalElements, totalPages: $totalPages, '
        'currentPage: $currentPage, pageSize: $pageSize, '
        'isFirst: $isFirst, isLast: $isLast)';
  }
}
