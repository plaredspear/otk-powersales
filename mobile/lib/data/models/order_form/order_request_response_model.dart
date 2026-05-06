/// 주문 등록 응답 모델 (Spec #592 §5.2).
class OrderRequestResponseModel {
  /// 백엔드 PK
  final int orderRequestId;

  /// 백엔드 채번 (`ORD-YYYYMMDD-{seq}`)
  final String orderRequestNumber;

  /// 등록 직후 = `SENT` (그 외 `APPROVED` / `SEND_FAILED` / `CANCELED`)
  final String status;

  /// 등록 합계 금액
  final int totalAmount;

  const OrderRequestResponseModel({
    required this.orderRequestId,
    required this.orderRequestNumber,
    required this.status,
    required this.totalAmount,
  });

  factory OrderRequestResponseModel.fromJson(Map<String, dynamic> json) {
    return OrderRequestResponseModel(
      orderRequestId: (json['orderRequestId'] as num).toInt(),
      orderRequestNumber: json['orderRequestNumber'] as String,
      status: json['status'] as String,
      totalAmount: (json['totalAmount'] as num).toInt(),
    );
  }
}
