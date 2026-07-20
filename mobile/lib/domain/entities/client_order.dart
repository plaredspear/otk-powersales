import 'order_detail.dart';

/// 거래처별 주문 목록 항목 엔티티
///
/// 거래처별 주문 현황에 표시되는 개별 주문 정보입니다.
/// "내 주문"과 달리 SAP 주문번호를 기반으로 합니다.
class ClientOrder {
  /// SAP 주문번호 (예: 300011396)
  final String sapOrderNumber;

  /// 거래처 ID
  final int clientId;

  /// 거래처명
  final String clientName;

  /// 총 주문금액 (원)
  final int totalAmount;

  /// 로그인 사용자가 등록한 주문인지 여부 (서버 권위 판정). 목록에서 "내 주문" 강조용.
  final bool isMine;

  /// 주문자명 (사번 기반 서버 배치 조회로 해석한 실제 이름). 담당자 무관 목록에서 누가 넣은 주문인지 표시.
  final String? ordererName;

  const ClientOrder({
    required this.sapOrderNumber,
    required this.clientId,
    required this.clientName,
    required this.totalAmount,
    this.isMine = false,
    this.ordererName,
  });

  ClientOrder copyWith({
    String? sapOrderNumber,
    int? clientId,
    String? clientName,
    int? totalAmount,
    bool? isMine,
    String? ordererName,
  }) {
    return ClientOrder(
      sapOrderNumber: sapOrderNumber ?? this.sapOrderNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      totalAmount: totalAmount ?? this.totalAmount,
      isMine: isMine ?? this.isMine,
      ordererName: ordererName ?? this.ordererName,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'clientId': clientId,
      'clientName': clientName,
      'totalAmount': totalAmount,
      'isMine': isMine,
      'ordererName': ordererName,
    };
  }

  factory ClientOrder.fromJson(Map<String, dynamic> json) {
    return ClientOrder(
      sapOrderNumber: json['sapOrderNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      totalAmount: json['totalAmount'] as int,
      isMine: json['isMine'] as bool? ?? false,
      ordererName: json['ordererName'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrder &&
        other.sapOrderNumber == sapOrderNumber &&
        other.clientId == clientId &&
        other.clientName == clientName &&
        other.totalAmount == totalAmount &&
        other.isMine == isMine &&
        other.ordererName == ordererName;
  }

  @override
  int get hashCode {
    return Object.hash(
      sapOrderNumber,
      clientId,
      clientName,
      totalAmount,
      isMine,
      ordererName,
    );
  }

  @override
  String toString() {
    return 'ClientOrder(sapOrderNumber: $sapOrderNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'totalAmount: $totalAmount, isMine: $isMine, '
        'ordererName: $ordererName)';
  }
}

/// 거래처별 주문 제품 엔티티
///
/// 거래처별 주문 상세의 제품 목록에 표시되는 개별 제품 정보입니다.
/// F18의 ProcessingItem과 유사하지만, 거래처별 주문 상세 전용입니다.
class ClientOrderItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 납품 수량 (예: "0 BOX") — confirmQuantityBox 기반, 배송 전에도 주문 확정 수량.
  final String deliveredQuantity;

  /// 배송 수량 (예: "10 BOX (300 EA)") — SAP 실제 출하량(shippingQuantityBox/shippingQuantity).
  /// 배송 전(대기/결품) 라인은 "0 BOX (0 EA)". 신규 추가(2026-07-21 사용자 결정).
  final String shippedQuantity;

  /// 배송 상태 코드 (서버 문자열 그대로 — [OrderDeliveryStatus] 상수와 비교). enum 미사용(crash 방어).
  final String deliveryStatus;

  /// 배송 정보 5필드 (배송중/배송완료 라인 탭 팝업용), 없으면 null
  final String? driverName;
  final String? vehicle;
  final String? driverPhone;
  final String? scheduleTime;
  final String? completeTime;

  const ClientOrderItem({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.shippedQuantity,
    required this.deliveryStatus,
    this.driverName,
    this.vehicle,
    this.driverPhone,
    this.scheduleTime,
    this.completeTime,
  });

  ClientOrderItem copyWith({
    String? productCode,
    String? productName,
    String? deliveredQuantity,
    String? shippedQuantity,
    String? deliveryStatus,
    String? driverName,
    String? vehicle,
    String? driverPhone,
    String? scheduleTime,
    String? completeTime,
  }) {
    return ClientOrderItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      deliveredQuantity: deliveredQuantity ?? this.deliveredQuantity,
      shippedQuantity: shippedQuantity ?? this.shippedQuantity,
      deliveryStatus: deliveryStatus ?? this.deliveryStatus,
      driverName: driverName ?? this.driverName,
      vehicle: vehicle ?? this.vehicle,
      driverPhone: driverPhone ?? this.driverPhone,
      scheduleTime: scheduleTime ?? this.scheduleTime,
      completeTime: completeTime ?? this.completeTime,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'deliveredQuantity': deliveredQuantity,
      'shippedQuantity': shippedQuantity,
      'deliveryStatus': deliveryStatus,
      'driverName': driverName,
      'vehicle': vehicle,
      'driverPhone': driverPhone,
      'scheduleTime': scheduleTime,
      'completeTime': completeTime,
    };
  }

  factory ClientOrderItem.fromJson(Map<String, dynamic> json) {
    return ClientOrderItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      deliveredQuantity: json['deliveredQuantity'] as String,
      // 하위호환 — 구버전 서버 응답엔 shippedQuantity 부재 가능 → 0 BOX (0 EA) 폴백.
      shippedQuantity: json['shippedQuantity'] as String? ?? '0 BOX (0 EA)',
      // 서버 문자열 그대로 보유 — enum 파싱 없음(미정의 코드에도 crash 없이 안전).
      deliveryStatus: json['deliveryStatus'] as String,
      driverName: json['driverName'] as String?,
      vehicle: json['vehicle'] as String?,
      driverPhone: json['driverPhone'] as String?,
      scheduleTime: json['scheduleTime'] as String?,
      completeTime: json['completeTime'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrderItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.deliveredQuantity == deliveredQuantity &&
        other.shippedQuantity == shippedQuantity &&
        other.deliveryStatus == deliveryStatus &&
        other.driverName == driverName &&
        other.vehicle == vehicle &&
        other.driverPhone == driverPhone &&
        other.scheduleTime == scheduleTime &&
        other.completeTime == completeTime;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      deliveredQuantity,
      shippedQuantity,
      deliveryStatus,
      driverName,
      vehicle,
      driverPhone,
      scheduleTime,
      completeTime,
    );
  }

  @override
  String toString() {
    return 'ClientOrderItem(productCode: $productCode, '
        'productName: $productName, '
        'deliveredQuantity: $deliveredQuantity, '
        'shippedQuantity: $shippedQuantity, '
        'deliveryStatus: $deliveryStatus)';
  }
}

/// 거래처별 주문 상세 엔티티
///
/// 거래처별 주문 상세 화면에 표시되는 전체 정보를 담는 도메인 엔티티입니다.
class ClientOrderDetail {
  /// SAP 주문번호
  final String sapOrderNumber;

  /// SAP 거래처 코드 (응답 노출, 화면 표시 안 함)
  final String? sapAccountCode;

  /// SAP 거래처명 (화면 "거래처" 라벨)
  final String? sapAccountName;

  /// 거래처 마감시간 (HH:mm) - nullable
  final String? clientDeadlineTime;

  /// 주문일
  final DateTime? orderDate;

  /// 납기일
  final DateTime? deliveryDate;

  /// 총 승인 금액 (원)
  final int? totalApprovedAmount;

  /// 주문자명 (주문 등록 사원)
  final String? ordererName;

  /// 주문자 사번
  final String? ordererCode;

  /// 주문한 제품 수
  final int orderedItemCount;

  /// 주문한 제품 목록
  final List<ClientOrderItem> orderedItems;

  const ClientOrderDetail({
    required this.sapOrderNumber,
    this.sapAccountCode,
    this.sapAccountName,
    this.clientDeadlineTime,
    this.orderDate,
    this.deliveryDate,
    this.totalApprovedAmount,
    this.ordererName,
    this.ordererCode,
    required this.orderedItemCount,
    required this.orderedItems,
  });

  ClientOrderDetail copyWith({
    String? sapOrderNumber,
    String? sapAccountCode,
    String? sapAccountName,
    String? clientDeadlineTime,
    DateTime? orderDate,
    DateTime? deliveryDate,
    int? totalApprovedAmount,
    String? ordererName,
    String? ordererCode,
    int? orderedItemCount,
    List<ClientOrderItem>? orderedItems,
  }) {
    return ClientOrderDetail(
      sapOrderNumber: sapOrderNumber ?? this.sapOrderNumber,
      sapAccountCode: sapAccountCode ?? this.sapAccountCode,
      sapAccountName: sapAccountName ?? this.sapAccountName,
      clientDeadlineTime: clientDeadlineTime ?? this.clientDeadlineTime,
      orderDate: orderDate ?? this.orderDate,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      totalApprovedAmount: totalApprovedAmount ?? this.totalApprovedAmount,
      ordererName: ordererName ?? this.ordererName,
      ordererCode: ordererCode ?? this.ordererCode,
      orderedItemCount: orderedItemCount ?? this.orderedItemCount,
      orderedItems: orderedItems ?? this.orderedItems,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'sapAccountCode': sapAccountCode,
      'sapAccountName': sapAccountName,
      'clientDeadlineTime': clientDeadlineTime,
      'orderDate': orderDate?.toIso8601String(),
      'deliveryDate': deliveryDate?.toIso8601String(),
      'totalApprovedAmount': totalApprovedAmount,
      'ordererName': ordererName,
      'ordererCode': ordererCode,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
    };
  }

  factory ClientOrderDetail.fromJson(Map<String, dynamic> json) {
    return ClientOrderDetail(
      sapOrderNumber: json['sapOrderNumber'] as String,
      sapAccountCode: json['sapAccountCode'] as String?,
      sapAccountName: json['sapAccountName'] as String?,
      clientDeadlineTime: json['clientDeadlineTime'] as String?,
      orderDate: json['orderDate'] != null
          ? DateTime.parse(json['orderDate'] as String)
          : null,
      deliveryDate: json['deliveryDate'] != null
          ? DateTime.parse(json['deliveryDate'] as String)
          : null,
      totalApprovedAmount: json['totalApprovedAmount'] as int?,
      ordererName: json['ordererName'] as String?,
      ordererCode: json['ordererCode'] as String?,
      orderedItemCount: json['orderedItemCount'] as int,
      orderedItems: (json['orderedItems'] as List<dynamic>)
          .map((e) => ClientOrderItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClientOrderDetail) return false;
    if (other.sapOrderNumber != sapOrderNumber) return false;
    if (other.sapAccountCode != sapAccountCode) return false;
    if (other.sapAccountName != sapAccountName) return false;
    if (other.clientDeadlineTime != clientDeadlineTime) return false;
    if (other.orderDate != orderDate) return false;
    if (other.deliveryDate != deliveryDate) return false;
    if (other.totalApprovedAmount != totalApprovedAmount) return false;
    if (other.ordererName != ordererName) return false;
    if (other.ordererCode != ordererCode) return false;
    if (other.orderedItemCount != orderedItemCount) return false;
    if (other.orderedItems.length != orderedItems.length) return false;
    for (var i = 0; i < orderedItems.length; i++) {
      if (other.orderedItems[i] != orderedItems[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      sapOrderNumber,
      sapAccountCode,
      sapAccountName,
      clientDeadlineTime,
      orderDate,
      deliveryDate,
      totalApprovedAmount,
      ordererName,
      ordererCode,
      orderedItemCount,
      Object.hashAll(orderedItems),
    );
  }

  @override
  String toString() {
    return 'ClientOrderDetail(sapOrderNumber: $sapOrderNumber, '
        'sapAccountName: $sapAccountName, '
        'orderedItemCount: $orderedItemCount)';
  }
}

/// 거래처별 주문 목록 조회 결과 값 객체
///
/// 페이지네이션 정보를 포함한 거래처별 주문 목록 결과입니다.
class ClientOrderListResult {
  /// 거래처별 주문 목록
  final List<ClientOrder> orders;

  /// 전체 결과 수
  final int totalElements;

  /// 전체 페이지 수
  final int totalPages;

  /// 현재 페이지 번호 (0부터 시작)
  final int currentPage;

  /// 페이지 크기
  final int pageSize;

  /// 첫 번째 페이지 여부
  final bool isFirst;

  /// 마지막 페이지 여부
  final bool isLast;

  const ClientOrderListResult({
    required this.orders,
    required this.totalElements,
    required this.totalPages,
    required this.currentPage,
    required this.pageSize,
    required this.isFirst,
    required this.isLast,
  });

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => !isLast;

  /// 이전 페이지가 있는지 여부
  bool get hasPreviousPage => !isFirst;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClientOrderListResult) return false;
    if (other.totalElements != totalElements) return false;
    if (other.totalPages != totalPages) return false;
    if (other.currentPage != currentPage) return false;
    if (other.pageSize != pageSize) return false;
    if (other.isFirst != isFirst) return false;
    if (other.isLast != isLast) return false;
    if (other.orders.length != orders.length) return false;
    for (var i = 0; i < orders.length; i++) {
      if (other.orders[i] != orders[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(orders),
      totalElements,
      totalPages,
      currentPage,
      pageSize,
      isFirst,
      isLast,
    );
  }

  @override
  String toString() {
    return 'ClientOrderListResult(orders: ${orders.length}, '
        'totalElements: $totalElements, totalPages: $totalPages, '
        'currentPage: $currentPage, pageSize: $pageSize, '
        'isFirst: $isFirst, isLast: $isLast)';
  }
}
