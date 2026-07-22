import '../../domain/entities/order_cancel.dart';

/// 주문 취소 요청 모델 (DTO)
///
/// API 요청 body에 전달할 JSON 형식의 주문 취소 요청 데이터입니다.
/// 백엔드(Spec #597) 계약: `{ "orderProductIds": [라인 PK ...] }`.
/// 빈 배열이면 전체 라인 취소.
class OrderCancelRequestModel {
  /// 취소할 주문 라인 PK 목록 (`OrderRequestProduct.id`)
  final List<int> orderProductIds;

  const OrderCancelRequestModel({
    required this.orderProductIds,
  });

  /// Domain Entity에서 변환
  factory OrderCancelRequestModel.fromEntity(OrderCancelRequest entity) {
    return OrderCancelRequestModel(
      orderProductIds: entity.orderProductIds,
    );
  }

  /// API 요청 JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'orderProductIds': orderProductIds,
    };
  }
}

/// 취소된 라인 모델 (DTO)
class CancelledLineModel {
  final int orderProductId;
  final double lineNumber;
  final String productCode;

  const CancelledLineModel({
    required this.orderProductId,
    required this.lineNumber,
    required this.productCode,
  });

  factory CancelledLineModel.fromJson(Map<String, dynamic> json) {
    return CancelledLineModel(
      orderProductId: (json['orderProductId'] as num).toInt(),
      lineNumber: (json['lineNumber'] as num).toDouble(),
      productCode: json['productCode'] as String,
    );
  }

  CancelledLine toEntity() {
    return CancelledLine(
      orderProductId: orderProductId,
      lineNumber: lineNumber,
      productCode: productCode,
    );
  }
}

/// 주문 취소 응답 모델 (DTO)
///
/// API 응답에서 파싱하여 도메인 엔티티로 변환하는 모델입니다.
class OrderCancelResponseModel {
  final int orderRequestId;
  final String orderRequestNumber;
  final String orderRequestStatus;
  final List<CancelledLineModel> cancelledLines;

  const OrderCancelResponseModel({
    required this.orderRequestId,
    required this.orderRequestNumber,
    required this.orderRequestStatus,
    required this.cancelledLines,
  });

  /// API 응답 JSON에서 파싱
  ///
  /// 예상 응답 구조 (Spec #597 §5):
  /// ```json
  /// {
  ///   "success": true,
  ///   "data": {
  ///     "orderRequestId": 12,
  ///     "orderRequestNumber": "OP20260301",
  ///     "orderRequestStatus": "CANCEL_REQUESTED",
  ///     "cancelledLines": [
  ///       {
  ///         "orderProductId": 101,
  ///         "lineNumber": 1,
  ///         "productCode": "01101123",
  ///         "cancelledAt": "2026-06-17T10:00:00"
  ///       }
  ///     ]
  ///   },
  ///   "message": "주문이 취소되었습니다"
  /// }
  /// ```
  factory OrderCancelResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final linesJson = data['cancelledLines'] as List<dynamic>? ?? const [];
    return OrderCancelResponseModel(
      orderRequestId: (data['orderRequestId'] as num).toInt(),
      orderRequestNumber: data['orderRequestNumber'] as String,
      orderRequestStatus: data['orderRequestStatus'] as String,
      cancelledLines: linesJson
          .map((e) => CancelledLineModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// Domain Entity로 변환
  OrderCancelResult toEntity() {
    return OrderCancelResult(
      orderRequestId: orderRequestId,
      orderRequestNumber: orderRequestNumber,
      orderRequestStatus: orderRequestStatus,
      cancelledLines: cancelledLines.map((e) => e.toEntity()).toList(),
    );
  }
}
