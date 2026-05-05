import '../../domain/entities/order_request.dart';

/// 주문 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 OrderRequest 엔티티로 변환합니다.
class OrderRequestModel {
  final int id;
  final String orderRequestNumber;
  final int clientId;
  final String clientName;
  final String orderDate;
  final String deliveryDate;
  final int totalAmount;
  final String approvalStatus;
  final bool isClosed;

  const OrderRequestModel({
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
  factory OrderRequestModel.fromJson(Map<String, dynamic> json) {
    return OrderRequestModel(
      id: json['id'] as int,
      orderRequestNumber: json['orderRequestNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      orderDate: json['orderDate'] as String,
      deliveryDate: json['deliveryDate'] as String,
      totalAmount: json['totalAmount'] as int,
      approvalStatus: json['approvalStatus'] as String,
      isClosed: json['isClosed'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'orderRequestNumber': orderRequestNumber,
      'clientId': clientId,
      'clientName': clientName,
      'orderDate': orderDate,
      'deliveryDate': deliveryDate,
      'totalAmount': totalAmount,
      'approvalStatus': approvalStatus,
      'isClosed': isClosed,
    };
  }

  /// Domain Entity로 변환
  OrderRequest toEntity() {
    return OrderRequest(
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
  factory OrderRequestModel.fromEntity(OrderRequest entity) {
    return OrderRequestModel(
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
    return other is OrderRequestModel &&
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
    return 'OrderRequestModel(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'orderDate: $orderDate, deliveryDate: $deliveryDate, '
        'totalAmount: $totalAmount, approvalStatus: $approvalStatus, '
        'isClosed: $isClosed)';
  }
}
