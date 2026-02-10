import '../../domain/entities/order_cancel.dart';

/// 주문 취소 요청 모델 (DTO)
///
/// API 요청 body에 전달할 JSON 형식의 주문 취소 요청 데이터입니다.
class OrderCancelRequestModel {
  /// 취소할 제품코드 목록
  final List<String> productCodes;

  const OrderCancelRequestModel({
    required this.productCodes,
  });

  /// Domain Entity에서 변환
  factory OrderCancelRequestModel.fromEntity(OrderCancelRequest entity) {
    return OrderCancelRequestModel(
      productCodes: entity.productCodes,
    );
  }

  /// API 요청 JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'productCodes': productCodes,
    };
  }
}

/// 주문 취소 응답 모델 (DTO)
///
/// API 응답에서 파싱하여 도메인 엔티티로 변환하는 모델입니다.
class OrderCancelResponseModel {
  /// 취소된 제품 수
  final int cancelledCount;

  /// 취소된 제품코드 목록
  final List<String> cancelledProductCodes;

  const OrderCancelResponseModel({
    required this.cancelledCount,
    required this.cancelledProductCodes,
  });

  /// API 응답 JSON에서 파싱
  ///
  /// 예상 응답 구조:
  /// ```json
  /// {
  ///   "success": true,
  ///   "data": {
  ///     "cancelledCount": 2,
  ///     "cancelledProductCodes": ["01101123", "01101222"]
  ///   },
  ///   "message": "주문이 취소되었습니다"
  /// }
  /// ```
  factory OrderCancelResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    return OrderCancelResponseModel(
      cancelledCount: data['cancelledCount'] as int,
      cancelledProductCodes: (data['cancelledProductCodes'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
    );
  }

  /// Domain Entity로 변환
  OrderCancelResult toEntity() {
    return OrderCancelResult(
      cancelledCount: cancelledCount,
      cancelledProductCodes: cancelledProductCodes,
    );
  }
}
