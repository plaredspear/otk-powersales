/// 주문 취소 요청 값 객체
///
/// 주문 취소 API에 전달할 요청 데이터를 담는 도메인 레벨 값 객체입니다.
class OrderCancelRequest {
  /// 주문 ID
  final int orderId;

  /// 취소 요청할 제품코드 목록
  final List<String> productCodes;

  const OrderCancelRequest({
    required this.orderId,
    required this.productCodes,
  });

  OrderCancelRequest copyWith({
    int? orderId,
    List<String>? productCodes,
  }) {
    return OrderCancelRequest(
      orderId: orderId ?? this.orderId,
      productCodes: productCodes ?? this.productCodes,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'orderId': orderId,
      'productCodes': productCodes,
    };
  }

  factory OrderCancelRequest.fromJson(Map<String, dynamic> json) {
    return OrderCancelRequest(
      orderId: json['orderId'] as int,
      productCodes: (json['productCodes'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderCancelRequest) return false;
    if (other.orderId != orderId) return false;
    if (other.productCodes.length != productCodes.length) return false;
    for (var i = 0; i < productCodes.length; i++) {
      if (other.productCodes[i] != productCodes[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      orderId,
      Object.hashAll(productCodes),
    );
  }

  @override
  String toString() {
    return 'OrderCancelRequest(orderId: $orderId, '
        'productCodes: $productCodes)';
  }
}

/// 주문 취소 결과 값 객체
///
/// 주문 취소 API 응답에서 반환되는 결과 데이터를 담는 도메인 레벨 값 객체입니다.
class OrderCancelResult {
  /// 취소된 제품 수
  final int cancelledCount;

  /// 취소된 제품코드 목록
  final List<String> cancelledProductCodes;

  const OrderCancelResult({
    required this.cancelledCount,
    required this.cancelledProductCodes,
  });

  OrderCancelResult copyWith({
    int? cancelledCount,
    List<String>? cancelledProductCodes,
  }) {
    return OrderCancelResult(
      cancelledCount: cancelledCount ?? this.cancelledCount,
      cancelledProductCodes:
          cancelledProductCodes ?? this.cancelledProductCodes,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'cancelledCount': cancelledCount,
      'cancelledProductCodes': cancelledProductCodes,
    };
  }

  factory OrderCancelResult.fromJson(Map<String, dynamic> json) {
    return OrderCancelResult(
      cancelledCount: json['cancelledCount'] as int,
      cancelledProductCodes: (json['cancelledProductCodes'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderCancelResult) return false;
    if (other.cancelledCount != cancelledCount) return false;
    if (other.cancelledProductCodes.length != cancelledProductCodes.length) {
      return false;
    }
    for (var i = 0; i < cancelledProductCodes.length; i++) {
      if (other.cancelledProductCodes[i] != cancelledProductCodes[i]) {
        return false;
      }
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      cancelledCount,
      Object.hashAll(cancelledProductCodes),
    );
  }

  @override
  String toString() {
    return 'OrderCancelResult(cancelledCount: $cancelledCount, '
        'cancelledProductCodes: $cancelledProductCodes)';
  }
}
