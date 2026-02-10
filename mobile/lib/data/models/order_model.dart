import '../../domain/entities/order.dart';

/// 주문 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 Order 엔티티로 변환합니다.
class OrderModel {
  final int id;
  final String orderRequestNumber;
  final int clientId;
  final String clientName;
  final String orderDate;
  final String deliveryDate;
  final int totalAmount;
  final String approvalStatus;
  final bool isClosed;

  const OrderModel({
    required this.id,
    required this.orderRequestNumber,
    required this.clientId,
    required this.clientName,
    required this.orderDate,
    required this.deliveryDate,
    required this.totalAmount,
    required this.approvalStatus,
    required this.isClosed,
  });

  /// snake_case JSON에서 파싱
  factory OrderModel.fromJson(Map<String, dynamic> json) {
    return OrderModel(
      id: json['id'] as int,
      orderRequestNumber: json['order_request_number'] as String,
      clientId: json['client_id'] as int,
      clientName: json['client_name'] as String,
      orderDate: json['order_date'] as String,
      deliveryDate: json['delivery_date'] as String,
      totalAmount: json['total_amount'] as int,
      approvalStatus: json['approval_status'] as String,
      isClosed: json['is_closed'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'order_request_number': orderRequestNumber,
      'client_id': clientId,
      'client_name': clientName,
      'order_date': orderDate,
      'delivery_date': deliveryDate,
      'total_amount': totalAmount,
      'approval_status': approvalStatus,
      'is_closed': isClosed,
    };
  }

  /// Domain Entity로 변환
  Order toEntity() {
    return Order(
      id: id,
      orderRequestNumber: orderRequestNumber,
      clientId: clientId,
      clientName: clientName,
      orderDate: DateTime.parse(orderDate),
      deliveryDate: DateTime.parse(deliveryDate),
      totalAmount: totalAmount,
      approvalStatus: ApprovalStatus.fromCode(approvalStatus),
      isClosed: isClosed,
    );
  }

  /// Domain Entity에서 생성
  factory OrderModel.fromEntity(Order entity) {
    return OrderModel(
      id: entity.id,
      orderRequestNumber: entity.orderRequestNumber,
      clientId: entity.clientId,
      clientName: entity.clientName,
      orderDate: entity.orderDate.toIso8601String().split('T')[0],
      deliveryDate: entity.deliveryDate.toIso8601String().split('T')[0],
      totalAmount: entity.totalAmount,
      approvalStatus: entity.approvalStatus.code,
      isClosed: entity.isClosed,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderModel &&
        other.id == id &&
        other.orderRequestNumber == orderRequestNumber &&
        other.clientId == clientId &&
        other.clientName == clientName &&
        other.orderDate == orderDate &&
        other.deliveryDate == deliveryDate &&
        other.totalAmount == totalAmount &&
        other.approvalStatus == approvalStatus &&
        other.isClosed == isClosed;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      orderRequestNumber,
      clientId,
      clientName,
      orderDate,
      deliveryDate,
      totalAmount,
      approvalStatus,
      isClosed,
    );
  }

  @override
  String toString() {
    return 'OrderModel(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'orderDate: $orderDate, deliveryDate: $deliveryDate, '
        'totalAmount: $totalAmount, approvalStatus: $approvalStatus, '
        'isClosed: $isClosed)';
  }
}
