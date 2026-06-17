/// 주문 취소 요청 값 객체
///
/// 주문 취소 API에 전달할 요청 데이터를 담는 도메인 레벨 값 객체입니다.
/// 백엔드(Spec #597)는 라인 PK(`orderProductIds`) 기준으로 부분/전체 취소를 처리합니다.
/// `orderProductIds` 가 비어 있으면 해당 주문의 취소 가능한 전체 라인을 취소합니다.
class OrderCancelRequest {
  /// 주문 ID
  final int orderId;

  /// 취소 요청할 주문 라인 PK 목록 (`OrderRequestProduct.id`)
  final List<int> orderProductIds;

  const OrderCancelRequest({
    required this.orderId,
    required this.orderProductIds,
  });

  OrderCancelRequest copyWith({
    int? orderId,
    List<int>? orderProductIds,
  }) {
    return OrderCancelRequest(
      orderId: orderId ?? this.orderId,
      orderProductIds: orderProductIds ?? this.orderProductIds,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'orderId': orderId,
      'orderProductIds': orderProductIds,
    };
  }

  factory OrderCancelRequest.fromJson(Map<String, dynamic> json) {
    return OrderCancelRequest(
      orderId: json['orderId'] as int,
      orderProductIds: (json['orderProductIds'] as List<dynamic>)
          .map((e) => (e as num).toInt())
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderCancelRequest) return false;
    if (other.orderId != orderId) return false;
    if (other.orderProductIds.length != orderProductIds.length) return false;
    for (var i = 0; i < orderProductIds.length; i++) {
      if (other.orderProductIds[i] != orderProductIds[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      orderId,
      Object.hashAll(orderProductIds),
    );
  }

  @override
  String toString() {
    return 'OrderCancelRequest(orderId: $orderId, '
        'orderProductIds: $orderProductIds)';
  }
}

/// 취소된 주문 라인 값 객체
///
/// 주문 취소 API 응답의 `cancelledLines` 항목 1건에 대응합니다.
class CancelledLine {
  /// 취소된 주문 라인 PK (`OrderRequestProduct.id`)
  final int orderProductId;

  /// 라인 번호
  final double lineNumber;

  /// 제품 코드
  final String productCode;

  const CancelledLine({
    required this.orderProductId,
    required this.lineNumber,
    required this.productCode,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is CancelledLine &&
        other.orderProductId == orderProductId &&
        other.lineNumber == lineNumber &&
        other.productCode == productCode;
  }

  @override
  int get hashCode => Object.hash(orderProductId, lineNumber, productCode);

  @override
  String toString() {
    return 'CancelledLine(orderProductId: $orderProductId, '
        'lineNumber: $lineNumber, productCode: $productCode)';
  }
}

/// 주문 취소 결과 값 객체
///
/// 주문 취소 API(Spec #597) 응답에서 반환되는 결과 데이터를 담는 도메인 레벨 값 객체입니다.
class OrderCancelResult {
  /// 주문 ID
  final int orderRequestId;

  /// 주문요청 번호
  final String orderRequestNumber;

  /// 취소 처리 후 주문 상태 (예: `CANCELED`, `SENT`)
  final String orderRequestStatus;

  /// 취소된 라인 목록
  final List<CancelledLine> cancelledLines;

  const OrderCancelResult({
    required this.orderRequestId,
    required this.orderRequestNumber,
    required this.orderRequestStatus,
    required this.cancelledLines,
  });

  /// 취소된 라인 수
  int get cancelledCount => cancelledLines.length;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderCancelResult) return false;
    if (other.orderRequestId != orderRequestId) return false;
    if (other.orderRequestNumber != orderRequestNumber) return false;
    if (other.orderRequestStatus != orderRequestStatus) return false;
    if (other.cancelledLines.length != cancelledLines.length) return false;
    for (var i = 0; i < cancelledLines.length; i++) {
      if (other.cancelledLines[i] != cancelledLines[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      orderRequestId,
      orderRequestNumber,
      orderRequestStatus,
      Object.hashAll(cancelledLines),
    );
  }

  @override
  String toString() {
    return 'OrderCancelResult(orderRequestId: $orderRequestId, '
        'orderRequestNumber: $orderRequestNumber, '
        'orderRequestStatus: $orderRequestStatus, '
        'cancelledLines: $cancelledLines)';
  }
}
