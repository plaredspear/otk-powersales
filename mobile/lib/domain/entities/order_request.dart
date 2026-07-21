import 'package:flutter/material.dart';

/// 주문요청 상태 코드 상수 (서버 `orderRequestStatus` 코드값).
///
/// 상태 표시명(한글)은 서버가 `orderRequestStatusName` 으로 함께 내려주므로 클라이언트에서 매핑하지 않는다.
/// 코드는 색상/분기 로직(취소·재전송 버튼 등)에만 사용한다.
/// 라이프사이클: DRAFT → SENT → APPROVED 또는 SEND_FAILED → (사용자 취소 시) CANCELED
abstract final class OrderStatusCode {
  static const String draft = 'DRAFT';
  static const String sent = 'SENT';
  static const String approved = 'APPROVED';
  static const String sendFailed = 'SEND_FAILED';
  static const String canceled = 'CANCELED';

  /// 상태 코드별 뱃지 색상. 미정의 코드/`null`(SF nillable NULL row)은 회색.
  static Color color(String? code) {
    switch (code) {
      case sent:
        return Colors.blue;
      case approved:
        return Colors.green;
      case sendFailed:
        return Colors.red;
      case canceled:
        return Colors.brown;
      case draft:
      default:
        return Colors.grey;
    }
  }

  /// 상태 필터 옵션 (코드, 표시명) — 목록 화면 상태 필터 드롭다운용.
  ///
  /// `DRAFT`(임시저장)는 제외한다 — 임시저장은 별도 `tmp_order` 도메인에서만 관리되어
  /// `order_request` 헤더가 `DRAFT` 로 생성되는 경로가 없다(생성 즉시 `SENT`). 즉 목록 조회에
  /// `임시저장` 상태의 주문이 나타나지 않으므로, 항상 0건인 dead 필터 옵션을 노출하지 않는다.
  /// (레거시 Heroku `list.jsp` 도 임시저장을 상태 필터에서 제외 — 필터 UX 정합.)
  static const List<({String code, String label})> filterOptions = [
    (code: sent, label: '전송'),
    (code: approved, label: '전송완료'),
    (code: sendFailed, label: '전송실패'),
    (code: canceled, label: '주문취소'),
  ];
}

/// 주문 정렬 타입 열거형
///
/// 주문 목록의 정렬 기준을 나타내며, API 요청 시 사용되는 sortBy/sortDir 값을 포함합니다.
/// 레거시(Heroku) 주문 현황과 동일하게 2개 옵션만 제공합니다.
enum OrderSortType {
  latestOrder('최신주문일순', 'orderDate', 'DESC'),
  latestDelivery('최근납기일순', 'deliveryDate', 'DESC');

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

  /// 승인상태 코드 (서버 `orderRequestStatus`, 예: APPROVED). 색상/분기 로직용.
  ///
  /// 서버가 SF nillable=true 정합으로 `null` 을 내려줄 수 있다(마이그레이션 SF NULL row 보존).
  /// 그런 경우 색상은 회색, 분기 비교는 어느 상태 코드와도 불일치로 처리된다.
  final String? orderRequestStatus;

  /// 승인상태 표시명 (서버 `orderRequestStatusName`, 예: 승인완료). 화면 출력용. 서버 `null` 가능.
  final String? orderRequestStatusName;

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
    required this.orderRequestStatusName,
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
    String? orderRequestStatus,
    String? orderRequestStatusName,
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
      orderRequestStatusName:
          orderRequestStatusName ?? this.orderRequestStatusName,
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
      'orderRequestStatus': orderRequestStatus,
      'orderRequestStatusName': orderRequestStatusName,
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
      orderRequestStatus: json['orderRequestStatus'] as String?,
      orderRequestStatusName: json['orderRequestStatusName'] as String?,
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
        other.orderRequestStatusName == orderRequestStatusName &&
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
      orderRequestStatusName,
      isClosed,
    );
  }

  @override
  String toString() {
    return 'OrderRequest(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'orderDate: $orderDate, deliveryDate: $deliveryDate, '
        'totalAmount: $totalAmount, orderRequestStatus: $orderRequestStatus, '
        'orderRequestStatusName: $orderRequestStatusName, '
        'isClosed: $isClosed)';
  }
}
