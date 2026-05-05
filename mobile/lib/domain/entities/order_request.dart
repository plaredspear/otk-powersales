import 'package:flutter/material.dart';

/// 주문요청 상태 열거형 (5단계 라이프사이클).
///
/// SF `DKRetail__RequestStatus__c` 매핑 + 신규 시스템 자체 상태.
/// 라이프사이클: DRAFT → SENT → APPROVED 또는 SEND_FAILED → (사용자 취소 시) CANCELED
enum OrderRequestStatus {
  draft('임시저장', 'DRAFT', Colors.grey),
  sent('전송', 'SENT', Colors.blue),
  approved('승인완료', 'APPROVED', Colors.green),
  sendFailed('전송실패', 'SEND_FAILED', Colors.red),
  canceled('주문취소', 'CANCELED', Colors.brown);

  const OrderRequestStatus(this.displayName, this.code, this.color);

  /// 화면에 표시되는 이름
  final String displayName;

  /// API 코드 값
  final String code;

  /// 뱃지 색상
  final Color color;

  /// API 코드에서 OrderRequestStatus로 변환
  static OrderRequestStatus fromCode(String code) {
    return OrderRequestStatus.values.firstWhere(
      (status) => status.code == code,
      orElse: () => OrderRequestStatus.draft,
    );
  }

  String toJson() => code;
  static OrderRequestStatus fromJson(String json) => fromCode(json);
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

/// 주문요청 엔티티
///
/// 주문현황 목록에 표시되는 주문요청 정보를 담는 도메인 엔티티입니다.
class OrderRequest {
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
  final OrderRequestStatus orderRequestStatus;

  /// 마감 여부
  final bool isClosed;

  const OrderRequest({
    required this.id,
    required this.orderRequestNumber,
    required this.clientId,
    required this.clientName,
    required this.orderDate,
    required this.deliveryDate,
    required this.totalAmount,
    required this.orderRequestStatus,
    required this.isClosed,
  });

  OrderRequest copyWith({
    int? id,
    String? orderRequestNumber,
    int? clientId,
    String? clientName,
    DateTime? orderDate,
    DateTime? deliveryDate,
    int? totalAmount,
    OrderRequestStatus? orderRequestStatus,
    bool? isClosed,
  }) {
    return OrderRequest(
      id: id ?? this.id,
      orderRequestNumber: orderRequestNumber ?? this.orderRequestNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      orderDate: orderDate ?? this.orderDate,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      totalAmount: totalAmount ?? this.totalAmount,
      orderRequestStatus: orderRequestStatus ?? this.orderRequestStatus,
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
      'orderRequestStatus': orderRequestStatus.code,
      'isClosed': isClosed,
    };
  }

  factory OrderRequest.fromJson(Map<String, dynamic> json) {
    return OrderRequest(
      id: json['id'] as int,
      orderRequestNumber: json['orderRequestNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      orderDate: DateTime.parse(json['orderDate'] as String),
      deliveryDate: DateTime.parse(json['deliveryDate'] as String),
      totalAmount: json['totalAmount'] as int,
      orderRequestStatus: OrderRequestStatus.fromCode(json['orderRequestStatus'] as String),
      isClosed: json['isClosed'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderRequest &&
        other.id == id &&
        other.orderRequestNumber == orderRequestNumber &&
        other.clientId == clientId &&
        other.clientName == clientName &&
        other.orderDate == orderDate &&
        other.deliveryDate == deliveryDate &&
        other.totalAmount == totalAmount &&
        other.orderRequestStatus == orderRequestStatus &&
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
      orderRequestStatus,
      isClosed,
    );
  }

  @override
  String toString() {
    return 'OrderRequest(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'orderDate: $orderDate, deliveryDate: $deliveryDate, '
        'totalAmount: $totalAmount, orderRequestStatus: $orderRequestStatus, '
        'isClosed: $isClosed)';
  }
}
