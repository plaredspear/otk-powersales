
/// 배송 상태 열거형
///
/// 주문 처리 현황 + 거래처 출하 상세에서 각 제품의 배송 상태를 나타냅니다.
/// 백엔드 권위: heroku 의 한글 4종 (`'대기'`/`'배송중'`/`'배송 완료'`/`'결품'`) 과 1:1 대응.
/// 응답은 영문 코드(`PENDING`/`SHIPPING`/`DELIVERED`/`OUT_OF_STOCK`)로 직렬화됩니다.
enum DeliveryStatus {
  pending('대기', 'PENDING'),
  shipping('배송중', 'SHIPPING'),
  // displayName 은 **거래처주문 도메인**(SF inbound ClientOrderReceive.cls:158) 표기 '배송 완료'(공백).
  // 주문상세(SF 조회 클래스 cls:157 은 공백 없는 '배송완료')는 처리현황 위젯이 별도 라벨로 표시한다
  // — 두 도메인 표기가 SF 원문상 달라 공유 enum 라벨 하나로 둘 다 만족 불가하기 때문.
  delivered('배송 완료', 'DELIVERED'),
  outOfStock('결품', 'OUT_OF_STOCK'),
  // 레거시 cls:153-159 가 어느 조건에도 안 걸려 status='' 로 남기는 케이스 (예: LineItemStatus 만
  // 채워지고 배차/완료 시각 없음). 레거시 화면은 빈 상태 텍스트로 표시하므로 displayName 도 ''.
  unknown('', 'UNKNOWN');

  const DeliveryStatus(this.displayName, this.code);

  /// 화면에 표시되는 이름
  final String displayName;

  /// API 코드 값
  final String code;

  /// API 코드에서 DeliveryStatus로 변환. 미정의 코드는 안전 기본값 [pending] 으로 fallback.
  static DeliveryStatus fromCode(String code) {
    return DeliveryStatus.values.firstWhere(
      (status) => status.code == code,
      orElse: () => DeliveryStatus.pending,
    );
  }

  String toJson() => code;
  static DeliveryStatus fromJson(String json) => fromCode(json);
}

/// 주문한 제품 엔티티
///
/// 주문 상세의 제품 목록에 표시되는 개별 제품 정보입니다.
class OrderedItem {
  /// 주문 라인 PK (`OrderRequestProduct.id`).
  ///
  /// 주문 취소 API(`POST .../cancel`)의 `orderProductIds` 식별자로 사용됩니다.
  /// 동일 제품코드가 복수 라인으로 존재할 수 있으므로 취소 선택은 이 PK 기준입니다.
  final int orderProductId;

  /// 제품 코드 (예: 01101123)
  final String productCode;

  /// 제품명 (예: 갈릭 아이올리소스 240g)
  final String productName;

  /// 총 주문수량 - 박스 단위 (예: 5, 60.5)
  final double totalQuantityBoxes;

  /// 총 주문수량 - 낱개 단위 (예: 100, 1150)
  final int totalQuantityPieces;

  /// 취소 여부
  final bool isCancelled;

  /// 결품 여부 — SAP `OrderRequestDetail` 응답의 `DefaultReason` 이 있는 제품.
  /// 레거시 `view.jsp:414` 동등 — 결품 제품은 "주문한 제품" 리스트에 회색+사유로 표시.
  final bool isOutOfStock;

  /// 결품 사유 (SAP `DefaultReason`). `isOutOfStock == true` 일 때만 채워진다.
  final String? outOfStockReason;

  const OrderedItem({
    required this.orderProductId,
    required this.productCode,
    required this.productName,
    required this.totalQuantityBoxes,
    required this.totalQuantityPieces,
    required this.isCancelled,
    this.isOutOfStock = false,
    this.outOfStockReason,
  });

  OrderedItem copyWith({
    int? orderProductId,
    String? productCode,
    String? productName,
    double? totalQuantityBoxes,
    int? totalQuantityPieces,
    bool? isCancelled,
    bool? isOutOfStock,
    String? outOfStockReason,
  }) {
    return OrderedItem(
      orderProductId: orderProductId ?? this.orderProductId,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      totalQuantityBoxes: totalQuantityBoxes ?? this.totalQuantityBoxes,
      totalQuantityPieces: totalQuantityPieces ?? this.totalQuantityPieces,
      isCancelled: isCancelled ?? this.isCancelled,
      isOutOfStock: isOutOfStock ?? this.isOutOfStock,
      outOfStockReason: outOfStockReason ?? this.outOfStockReason,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'orderProductId': orderProductId,
      'productCode': productCode,
      'productName': productName,
      'totalQuantityBoxes': totalQuantityBoxes,
      'totalQuantityPieces': totalQuantityPieces,
      'isCancelled': isCancelled,
      'isOutOfStock': isOutOfStock,
      'outOfStockReason': outOfStockReason,
    };
  }

  factory OrderedItem.fromJson(Map<String, dynamic> json) {
    return OrderedItem(
      orderProductId: (json['orderProductId'] as num).toInt(),
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      totalQuantityBoxes: (json['totalQuantityBoxes'] as num).toDouble(),
      totalQuantityPieces: json['totalQuantityPieces'] as int,
      isCancelled: json['isCancelled'] as bool,
      isOutOfStock: json['isOutOfStock'] as bool? ?? false,
      outOfStockReason: json['outOfStockReason'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderedItem &&
        other.orderProductId == orderProductId &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.totalQuantityBoxes == totalQuantityBoxes &&
        other.totalQuantityPieces == totalQuantityPieces &&
        other.isCancelled == isCancelled &&
        other.isOutOfStock == isOutOfStock &&
        other.outOfStockReason == outOfStockReason;
  }

  @override
  int get hashCode {
    return Object.hash(
      orderProductId,
      productCode,
      productName,
      totalQuantityBoxes,
      totalQuantityPieces,
      isCancelled,
      isOutOfStock,
      outOfStockReason,
    );
  }

  @override
  String toString() {
    return 'OrderedItem(orderProductId: $orderProductId, '
        'productCode: $productCode, productName: $productName, '
        'totalQuantityBoxes: $totalQuantityBoxes, '
        'totalQuantityPieces: $totalQuantityPieces, '
        'isCancelled: $isCancelled, isOutOfStock: $isOutOfStock)';
  }
}

/// 처리 항목 엔티티
///
/// 주문 처리 현황의 개별 항목 (SAP 주문번호 하위의 제품별 처리 상태).
/// 차량/기사 5필드 (`driverName/vehicle/driverPhone/scheduleTime/completeTime`) 는
/// `SHIPPING`/`DELIVERED` 라인 탭 → 팝업 표시용 (Spec #595 P2-M, Q5).
class ProcessingItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 납품 수량 (예: "10 BOX (300 EA)")
  final String deliveredQuantity;

  /// 배송 상태
  final DeliveryStatus deliveryStatus;

  /// 기사명 (배송중/배송완료 라인 팝업용)
  final String? driverName;

  /// 차량번호 (배송중/배송완료 라인 팝업용)
  final String? vehicle;

  /// 기사 연락처 (배송중/배송완료 라인 팝업용)
  final String? driverPhone;

  /// 배송 예정 시각 — 레거시 동등으로 SAP `HHmmss` 무가공 ('000000' 도 그대로). 빈 값만 null
  final String? scheduleTime;

  /// 배송 완료 시각 — 레거시 동등으로 SAP `HHmmss` 무가공 ('000000' 도 그대로). 빈 값만 null
  final String? completeTime;

  const ProcessingItem({
    required this.productCode,
    required this.productName,
    required this.deliveredQuantity,
    required this.deliveryStatus,
    this.driverName,
    this.vehicle,
    this.driverPhone,
    this.scheduleTime,
    this.completeTime,
  });

  /// 차량/기사 5필드 모두 null 인지 여부 (탭 무반응 판단용).
  bool get hasNoDeliveryDetail =>
      driverName == null &&
      vehicle == null &&
      driverPhone == null &&
      scheduleTime == null &&
      completeTime == null;

  ProcessingItem copyWith({
    String? productCode,
    String? productName,
    String? deliveredQuantity,
    DeliveryStatus? deliveryStatus,
    String? driverName,
    String? vehicle,
    String? driverPhone,
    String? scheduleTime,
    String? completeTime,
  }) {
    return ProcessingItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      deliveredQuantity: deliveredQuantity ?? this.deliveredQuantity,
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
      'deliveryStatus': deliveryStatus.code,
      'driverName': driverName,
      'vehicle': vehicle,
      'driverPhone': driverPhone,
      'scheduleTime': scheduleTime,
      'completeTime': completeTime,
    };
  }

  factory ProcessingItem.fromJson(Map<String, dynamic> json) {
    return ProcessingItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      deliveredQuantity: json['deliveredQuantity'] as String,
      deliveryStatus:
          DeliveryStatus.fromCode(json['deliveryStatus'] as String),
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
    return other is ProcessingItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.deliveredQuantity == deliveredQuantity &&
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
    return 'ProcessingItem(productCode: $productCode, '
        'productName: $productName, '
        'deliveredQuantity: $deliveredQuantity, '
        'deliveryStatus: $deliveryStatus)';
  }
}

/// 주문 처리 현황 엔티티
///
/// SAP 주문번호별 처리 현황 정보입니다. 마감후 화면에서 사용됩니다.
class OrderProcessingStatus {
  /// SAP 주문번호 (예: 0300013650)
  final String sapOrderNumber;

  /// 처리 항목 목록
  final List<ProcessingItem> items;

  const OrderProcessingStatus({
    required this.sapOrderNumber,
    required this.items,
  });

  OrderProcessingStatus copyWith({
    String? sapOrderNumber,
    List<ProcessingItem>? items,
  }) {
    return OrderProcessingStatus(
      sapOrderNumber: sapOrderNumber ?? this.sapOrderNumber,
      items: items ?? this.items,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'items': items.map((e) => e.toJson()).toList(),
    };
  }

  factory OrderProcessingStatus.fromJson(Map<String, dynamic> json) {
    return OrderProcessingStatus(
      sapOrderNumber: json['sapOrderNumber'] as String,
      items: (json['items'] as List<dynamic>)
          .map((e) => ProcessingItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderProcessingStatus) return false;
    if (other.sapOrderNumber != sapOrderNumber) return false;
    if (other.items.length != items.length) return false;
    for (var i = 0; i < items.length; i++) {
      if (other.items[i] != items[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      sapOrderNumber,
      Object.hashAll(items),
    );
  }

  @override
  String toString() {
    return 'OrderProcessingStatus(sapOrderNumber: $sapOrderNumber, '
        'items: ${items.length})';
  }
}

/// 반려 제품 엔티티
///
/// 마감후 반려된 제품 정보입니다.
class RejectedItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 주문 수량 (BOX). 서버가 `BigDecimal` 로 내려주므로 소수 박스가 올 수 있어 `double` 로 받는다
  /// (`OrderedItem.totalQuantityBoxes` 와 동일 정합 — `as int` 캐스팅은 소수 값에서 파싱 예외).
  final double orderQuantityBoxes;

  /// 반려 사유
  final String rejectionReason;

  const RejectedItem({
    required this.productCode,
    required this.productName,
    required this.orderQuantityBoxes,
    required this.rejectionReason,
  });

  RejectedItem copyWith({
    String? productCode,
    String? productName,
    double? orderQuantityBoxes,
    String? rejectionReason,
  }) {
    return RejectedItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      orderQuantityBoxes: orderQuantityBoxes ?? this.orderQuantityBoxes,
      rejectionReason: rejectionReason ?? this.rejectionReason,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'orderQuantityBoxes': orderQuantityBoxes,
      'rejectionReason': rejectionReason,
    };
  }

  factory RejectedItem.fromJson(Map<String, dynamic> json) {
    return RejectedItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      orderQuantityBoxes: (json['orderQuantityBoxes'] as num).toDouble(),
      rejectionReason: json['rejectionReason'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is RejectedItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.orderQuantityBoxes == orderQuantityBoxes &&
        other.rejectionReason == rejectionReason;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      orderQuantityBoxes,
      rejectionReason,
    );
  }

  @override
  String toString() {
    return 'RejectedItem(productCode: $productCode, '
        'productName: $productName, '
        'orderQuantityBoxes: $orderQuantityBoxes, '
        'rejectionReason: $rejectionReason)';
  }
}

/// 주문 상세 엔티티
///
/// 주문 상세 화면에 표시되는 전체 정보를 담는 도메인 엔티티입니다.
/// 마감 상태와 반려 여부에 따라 3가지 화면 구성이 동적으로 결정됩니다.
class OrderDetail {
  /// 주문 고유 ID
  final int id;

  /// 주문 요청번호 (예: OP00000001)
  final String orderRequestNumber;

  /// 거래처 ID
  final int clientId;

  /// 거래처명
  final String clientName;

  /// 거래처 마감시간 (HH:mm) — nullable
  final String? clientDeadlineTime;

  /// 주문일
  final DateTime orderDate;

  /// 납기일
  final DateTime deliveryDate;

  /// 총 주문금액 (원)
  final int totalAmount;

  /// 총 승인금액 (원) — 마감후에만 의미
  final int? totalApprovedAmount;

  /// 승인상태 코드 (서버 `orderRequestStatus`, 예: APPROVED). 색상/분기 로직용.
  ///
  /// 서버가 SF nillable=true 정합으로 `null` 을 내려줄 수 있다(마이그레이션 SF NULL row 보존).
  /// 그런 경우 색상은 회색, 취소/재전송 분기 비교는 어느 상태 코드와도 불일치로 처리된다.
  final String? orderRequestStatus;

  /// 승인상태 표시명 (서버 `orderRequestStatusName`, 예: 승인완료). 화면 출력용. 서버 `null` 가능.
  final String? orderRequestStatusName;

  /// 마감 여부
  final bool isClosed;

  /// 취소 가능 여부 (서버 권위 판정: 상태 + 마감 + 등록 SAP 전송 not in-flight).
  /// 취소 버튼 노출 게이트의 단일 진실원 — 서버 취소 가드와 정합.
  final bool cancelable;

  /// 등록 SAP 전송이 아직 진행 중(전송 처리 중)인지 여부. 취소 불가 사유 안내용.
  final bool registrationInFlight;

  /// 주문한 제품 수
  final int orderedItemCount;

  /// 주문한 제품 목록
  final List<OrderedItem> orderedItems;

  /// 주문 처리 현황 — SAP 주문번호별 그룹 배열 (Spec #595 P2-M).
  ///
  /// - 마감 전(`isClosed == false`) 시 백엔드가 `null` 반환 (Q6).
  /// - SAP 호출 실패 시 `null`.
  /// - 다중 SAP 주문 분할 케이스에서 N개 그룹 (Q1 옵션 2).
  final List<OrderProcessingStatus>? orderProcessingStatusList;

  /// 반려 제품 목록 (마감후, 반려 존재 시)
  final List<RejectedItem>? rejectedItems;

  const OrderDetail({
    required this.id,
    required this.orderRequestNumber,
    required this.clientId,
    required this.clientName,
    this.clientDeadlineTime,
    required this.orderDate,
    required this.deliveryDate,
    required this.totalAmount,
    this.totalApprovedAmount,
    required this.orderRequestStatus,
    required this.orderRequestStatusName,
    required this.isClosed,
    this.cancelable = false,
    this.registrationInFlight = false,
    required this.orderedItemCount,
    required this.orderedItems,
    this.orderProcessingStatusList,
    this.rejectedItems,
  });

  /// 반려 제품이 있는지 여부
  bool get hasRejectedItems =>
      rejectedItems != null && rejectedItems!.isNotEmpty;

  /// 모든 제품이 취소되었는지 여부
  bool get allItemsCancelled =>
      orderedItems.isNotEmpty && orderedItems.every((item) => item.isCancelled);

  OrderDetail copyWith({
    int? id,
    String? orderRequestNumber,
    int? clientId,
    String? clientName,
    String? clientDeadlineTime,
    DateTime? orderDate,
    DateTime? deliveryDate,
    int? totalAmount,
    int? totalApprovedAmount,
    String? orderRequestStatus,
    String? orderRequestStatusName,
    bool? isClosed,
    bool? cancelable,
    bool? registrationInFlight,
    int? orderedItemCount,
    List<OrderedItem>? orderedItems,
    List<OrderProcessingStatus>? orderProcessingStatusList,
    List<RejectedItem>? rejectedItems,
  }) {
    return OrderDetail(
      id: id ?? this.id,
      orderRequestNumber: orderRequestNumber ?? this.orderRequestNumber,
      clientId: clientId ?? this.clientId,
      clientName: clientName ?? this.clientName,
      clientDeadlineTime: clientDeadlineTime ?? this.clientDeadlineTime,
      orderDate: orderDate ?? this.orderDate,
      deliveryDate: deliveryDate ?? this.deliveryDate,
      totalAmount: totalAmount ?? this.totalAmount,
      totalApprovedAmount: totalApprovedAmount ?? this.totalApprovedAmount,
      orderRequestStatus: orderRequestStatus ?? this.orderRequestStatus,
      orderRequestStatusName:
          orderRequestStatusName ?? this.orderRequestStatusName,
      isClosed: isClosed ?? this.isClosed,
      cancelable: cancelable ?? this.cancelable,
      registrationInFlight: registrationInFlight ?? this.registrationInFlight,
      orderedItemCount: orderedItemCount ?? this.orderedItemCount,
      orderedItems: orderedItems ?? this.orderedItems,
      orderProcessingStatusList:
          orderProcessingStatusList ?? this.orderProcessingStatusList,
      rejectedItems: rejectedItems ?? this.rejectedItems,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'orderRequestNumber': orderRequestNumber,
      'clientId': clientId,
      'clientName': clientName,
      'clientDeadlineTime': clientDeadlineTime,
      'orderDate': orderDate.toIso8601String(),
      'deliveryDate': deliveryDate.toIso8601String(),
      'totalAmount': totalAmount,
      'totalApprovedAmount': totalApprovedAmount,
      'orderRequestStatus': orderRequestStatus,
      'orderRequestStatusName': orderRequestStatusName,
      'isClosed': isClosed,
      'cancelable': cancelable,
      'registrationInFlight': registrationInFlight,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
      'orderProcessingStatusList':
          orderProcessingStatusList?.map((e) => e.toJson()).toList(),
      'rejectedItems': rejectedItems?.map((e) => e.toJson()).toList(),
    };
  }

  factory OrderDetail.fromJson(Map<String, dynamic> json) {
    return OrderDetail(
      id: json['id'] as int,
      orderRequestNumber: json['orderRequestNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      clientDeadlineTime: json['clientDeadlineTime'] as String?,
      orderDate: DateTime.parse(json['orderDate'] as String),
      deliveryDate: DateTime.parse(json['deliveryDate'] as String),
      totalAmount: json['totalAmount'] as int,
      totalApprovedAmount: json['totalApprovedAmount'] as int?,
      orderRequestStatus: json['orderRequestStatus'] as String?,
      orderRequestStatusName: json['orderRequestStatusName'] as String?,
      isClosed: json['isClosed'] as bool,
      cancelable: json['cancelable'] as bool? ?? false,
      registrationInFlight: json['registrationInFlight'] as bool? ?? false,
      orderedItemCount: json['orderedItemCount'] as int,
      orderedItems: (json['orderedItems'] as List<dynamic>)
          .map((e) => OrderedItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      orderProcessingStatusList: json['orderProcessingStatusList'] != null
          ? (json['orderProcessingStatusList'] as List<dynamic>)
              .map((e) =>
                  OrderProcessingStatus.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
      rejectedItems: json['rejectedItems'] != null
          ? (json['rejectedItems'] as List<dynamic>)
              .map((e) => RejectedItem.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderDetail) return false;
    if (other.id != id) return false;
    if (other.orderRequestNumber != orderRequestNumber) return false;
    if (other.clientId != clientId) return false;
    if (other.clientName != clientName) return false;
    if (other.clientDeadlineTime != clientDeadlineTime) return false;
    if (other.orderDate != orderDate) return false;
    if (other.deliveryDate != deliveryDate) return false;
    if (other.totalAmount != totalAmount) return false;
    if (other.totalApprovedAmount != totalApprovedAmount) return false;
    if (other.orderRequestStatus != orderRequestStatus) return false;
    if (other.orderRequestStatusName != orderRequestStatusName) return false;
    if (other.isClosed != isClosed) return false;
    if (other.cancelable != cancelable) return false;
    if (other.registrationInFlight != registrationInFlight) return false;
    if (other.orderedItemCount != orderedItemCount) return false;
    if (other.orderedItems.length != orderedItems.length) return false;
    for (var i = 0; i < orderedItems.length; i++) {
      if (other.orderedItems[i] != orderedItems[i]) return false;
    }
    if (other.orderProcessingStatusList?.length !=
        orderProcessingStatusList?.length) {
      return false;
    }
    if (orderProcessingStatusList != null) {
      for (var i = 0; i < orderProcessingStatusList!.length; i++) {
        if (other.orderProcessingStatusList![i] !=
            orderProcessingStatusList![i]) {
          return false;
        }
      }
    }
    if (other.hasRejectedItems != hasRejectedItems) return false;
    if (hasRejectedItems) {
      if (other.rejectedItems!.length != rejectedItems!.length) return false;
      for (var i = 0; i < rejectedItems!.length; i++) {
        if (other.rejectedItems![i] != rejectedItems![i]) return false;
      }
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      orderRequestNumber,
      clientId,
      clientName,
      clientDeadlineTime,
      orderDate,
      deliveryDate,
      totalAmount,
      totalApprovedAmount,
      orderRequestStatus,
      orderRequestStatusName,
      isClosed,
      cancelable,
      registrationInFlight,
      orderedItemCount,
      Object.hashAll(orderedItems),
      orderProcessingStatusList != null
          ? Object.hashAll(orderProcessingStatusList!)
          : null,
      rejectedItems != null ? Object.hashAll(rejectedItems!) : null,
    );
  }

  @override
  String toString() {
    return 'OrderDetail(id: $id, orderRequestNumber: $orderRequestNumber, '
        'clientName: $clientName, isClosed: $isClosed, '
        'orderedItemCount: $orderedItemCount, '
        'hasRejectedItems: $hasRejectedItems)';
  }
}
