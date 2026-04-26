import 'package:flutter/material.dart';

/// 승인 상태 열거형
///
/// 주문의 승인 상태를 나타내며, 각 상태별 화면 표시 텍스트와 색상을 포함합니다.
enum ApprovalStatus {
  approved('승인완료', 'APPROVED', Colors.green),
  pending('승인상태', 'PENDING', Colors.amber),
  sendFailed('전송실패', 'SEND_FAILED', Colors.red),
  resend('재전송', 'RESEND', Colors.orange);

  const ApprovalStatus(this.displayName, this.code, this.color);

  /// 화면에 표시되는 이름
  final String displayName;

  /// API 코드 값
  final String code;

  /// 뱃지 색상
  final Color color;

  /// API 코드에서 ApprovalStatus로 변환
  static ApprovalStatus fromCode(String code) {
    return ApprovalStatus.values.firstWhere(
      (status) => status.code == code,
      orElse: () => ApprovalStatus.pending,
    );
  }

  String toJson() => code;
  static ApprovalStatus fromJson(String json) => fromCode(json);
}

/// 주문 정렬 타입 열거형
///
/// 주문 목록의 정렬 기준을 나타내며, API 요청 시 사용되는 sortBy/sortDir 값을 포함합니다.
enum OrderSortType {
  latestOrder('최신주문일순', 'orderDate', 'DESC'),
  oldestOrder('오래된주문일순', 'orderDate', 'ASC'),
  latestDelivery('최신납기일순', 'deliveryDate', 'DESC'),
  oldestDelivery('오래된납기일순', 'deliveryDate', 'ASC'),
  amountHigh('금액높은순', 'totalAmount', 'DESC'),
  amountLow('금액낮은순', 'totalAmount', 'ASC');

  const OrderSortType(this.displayName, this.sortBy, this.sortDir);

  /// 화면에 표시되는 이름
  final String displayName;

  /// API sortBy 파라미터 값
  final String sortBy;

  /// API sortDir 파라미터 값
  final String sortDir;
}

/// 주문 엔티티
///
/// 주문현황 목록에 표시되는 주문 정보를 담는 도메인 엔티티입니다.
class Order {
  /// 주문 고유 ID
  final int id;

  /// 주문 요청번호 (예: OP00000074)
  final String orderRequestNumber;

  /// 거래처 ID
  final int clientId;

  /// 거래처명
  final String clientName;

  /// 주문일
  final DateTime orderDate;

  /// 납기일
  final DateTime deliveryDate;

  /// 총 주문금액 (원)
  final int totalAmount;

  /// 승인상태
  final ApprovalStatus approvalStatus;

  /// 마감 여부
  final bool isClosed;

  const Order({
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

  Order copyWith({
    int? id,
    String? orderRequestNumber,
    int? clientId,
    String? clientName,
    DateTime? orderDate,
    DateTime? deliveryDate,
    int? totalAmount,
    ApprovalStatus? approvalStatus,
    bool? isClosed,
  }) {
    return Order(
      id: id ?? this.id,
      orderRequestNumber: orderRequestNumber ?? this.orderRequestNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      orderDate: orderDate ?? this.orderDate,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      totalAmount: totalAmount ?? this.totalAmount,
      approvalStatus: approvalStatus ?? this.approvalStatus,
      isClosed: isClosed ?? this.isClosed,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'orderRequestNumber': orderRequestNumber,
      'clientId': clientId,
      'clientName': clientName,
      'orderDate': orderDate.toIso8601String(),
      'deliveryDate': deliveryDate.toIso8601String(),
      'totalAmount': totalAmount,
      'approvalStatus': approvalStatus.code,
      'isClosed': isClosed,
    };
  }

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: json['id'] as int,
      orderRequestNumber: json['orderRequestNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      orderDate: DateTime.parse(json['orderDate'] as String),
      deliveryDate: DateTime.parse(json['deliveryDate'] as String),
      totalAmount: json['totalAmount'] as int,
      approvalStatus: ApprovalStatus.fromCode(json['approvalStatus'] as String),
      isClosed: json['isClosed'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Order &&
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
    return 'Order(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'orderDate: $orderDate, deliveryDate: $deliveryDate, '
        'totalAmount: $totalAmount, approvalStatus: $approvalStatus, '
        'isClosed: $isClosed)';
  }
}
