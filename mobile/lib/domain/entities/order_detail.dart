
/// 배송 상태 코드 상수 (서버 `deliveryStatus` 코드값).
///
/// **enum 이 아니라 문자열 상수**로 관리한다 — 서버가 미정의 상태 코드를 내려줘도 enum 파싱/exhaustive
/// switch 에서 crash 나지 않도록 하기 위함(방어). 엔티티의 `deliveryStatus` 는 서버 문자열을 그대로
/// 보유하고, 화면은 이 상수와 `==` 로 비교하거나 [OrderDeliveryStatus.displayName] 로 라벨을 얻는다.
///
/// 주문 처리 현황 + 거래처 출하 상세에서 각 제품의 배송 상태를 나타낸다.
/// 백엔드 권위: heroku 의 한글 4종 (`'대기'`/`'배송중'`/`'배송 완료'`/`'결품'`) 과 1:1 대응.
/// 응답은 영문 코드(`PENDING`/`SHIPPING`/`DELIVERED`/`OUT_OF_STOCK`, 빈 상태는 `UNKNOWN`)로 직렬화된다.
abstract final class OrderDeliveryStatus {
  static const String pending = 'PENDING';
  static const String shipping = 'SHIPPING';
  static const String delivered = 'DELIVERED';
  static const String outOfStock = 'OUT_OF_STOCK';

  /// 레거시 cls:153-159 가 어느 조건에도 안 걸려 status='' 로 남기는 케이스(예: LineItemStatus 만
  /// 채워지고 배차/완료 시각 없음). 화면은 빈 라벨로 표시.
  static const String unknown = 'UNKNOWN';

  /// 상태 코드별 화면 표시명. 미정의/`null` 코드는 빈 문자열(crash 대신 빈 라벨).
  ///
  /// delivered 는 **거래처주문 도메인**(SF inbound ClientOrderReceive.cls:158) 표기 '배송 완료'(공백).
  /// 주문상세(SF 조회 클래스 cls:157 은 공백 없는 '배송완료')는 처리현황 위젯이 별도 라벨로 표시한다.
  static String displayName(String? code) {
    switch (code) {
      case pending:
        return '대기';
      case shipping:
        return '배송중';
      case delivered:
        return '배송 완료';
      case outOfStock:
        return '결품';
      case unknown:
      default:
        return '';
    }
  }
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

  /// 취소 여부 — 마이그레이션된 과거 취소 이력(`line_change_type='X'`)만 표시(Spec #845).
  /// 신규 취소는 이 플래그를 세팅하지 않고 [isCancelRequested]/[isCancelledBySap] 로 구분한다.
  final bool isCancelled;

  /// 취소 요청 흔적(로컬) — `cancel_requested_at != null` (Spec #845).
  /// "취소요청" 배지. SAP 실제 반영([isCancelledBySap]) 여부와 독립적으로 켜질 수 있다.
  final bool isCancelRequested;

  /// 결품 여부 — SAP `OrderRequestDetail` 응답의 `DefaultReason` 코드가 결품셋({F1,L1,L2,L3}).
  /// 레거시 `view.jsp:414` 동등 — 결품 제품은 "주문한 제품" 리스트에 회색+사유로 표시.
  final bool isOutOfStock;

  /// 결품 사유 (SAP `DefaultReason`, `"{코드} {설명}"` 포맷). `isOutOfStock == true` 일 때만 채워진다.
  final String? outOfStockReason;

  /// SAP 실제 취소 여부 — `DefaultReason` 코드가 채워졌고 결품셋이 아닌 경우(Spec #845).
  /// "SAP취소됨" 배지. [isOutOfStock] 과 상호배타적(서버가 한 라인당 하나만 채움).
  final bool isCancelledBySap;

  /// 취소 사유 (SAP `DefaultReason`, `"{코드} {설명}"` 포맷). `isCancelledBySap == true` 일 때 채워진다.
  final String? cancelReason;

  const OrderedItem({
    required this.orderProductId,
    required this.productCode,
    required this.productName,
    required this.totalQuantityBoxes,
    required this.totalQuantityPieces,
    required this.isCancelled,
    this.isCancelRequested = false,
    this.isOutOfStock = false,
    this.outOfStockReason,
    this.isCancelledBySap = false,
    this.cancelReason,
  });

  OrderedItem copyWith({
    int? orderProductId,
    String? productCode,
    String? productName,
    double? totalQuantityBoxes,
    int? totalQuantityPieces,
    bool? isCancelled,
    bool? isCancelRequested,
    bool? isOutOfStock,
    String? outOfStockReason,
    bool? isCancelledBySap,
    String? cancelReason,
  }) {
    return OrderedItem(
      orderProductId: orderProductId ?? this.orderProductId,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      totalQuantityBoxes: totalQuantityBoxes ?? this.totalQuantityBoxes,
      totalQuantityPieces: totalQuantityPieces ?? this.totalQuantityPieces,
      isCancelled: isCancelled ?? this.isCancelled,
      isCancelRequested: isCancelRequested ?? this.isCancelRequested,
      isOutOfStock: isOutOfStock ?? this.isOutOfStock,
      outOfStockReason: outOfStockReason ?? this.outOfStockReason,
      isCancelledBySap: isCancelledBySap ?? this.isCancelledBySap,
      cancelReason: cancelReason ?? this.cancelReason,
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
      'isCancelRequested': isCancelRequested,
      'isOutOfStock': isOutOfStock,
      'outOfStockReason': outOfStockReason,
      'isCancelledBySap': isCancelledBySap,
      'cancelReason': cancelReason,
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
      isCancelRequested: json['isCancelRequested'] as bool? ?? false,
      isOutOfStock: json['isOutOfStock'] as bool? ?? false,
      outOfStockReason: json['outOfStockReason'] as String?,
      isCancelledBySap: json['isCancelledBySap'] as bool? ?? false,
      cancelReason: json['cancelReason'] as String?,
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
        other.isCancelRequested == isCancelRequested &&
        other.isOutOfStock == isOutOfStock &&
        other.outOfStockReason == outOfStockReason &&
        other.isCancelledBySap == isCancelledBySap &&
        other.cancelReason == cancelReason;
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
      isCancelRequested,
      isOutOfStock,
      outOfStockReason,
      isCancelledBySap,
      cancelReason,
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

  /// 배송 상태 코드 (서버 문자열 그대로 — [OrderDeliveryStatus] 상수와 비교). enum 미사용(crash 방어).
  final String deliveryStatus;

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
    String? deliveryStatus,
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
      'deliveryStatus': deliveryStatus,
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

/// 미납 제품 엔티티 (신규 정책, 2026-07-20 사용자 결정 — SF 레거시엔 없던 분류)
///
/// SAP 주문번호가 **있는** 라인 중 `LineItemStatus` 가 채워져 있으면서 "OK" 가 아닌 제품.
/// 반려(SAP 주문번호 없음)와 구분되며, 결품(DefaultReason)은 포함하지 않습니다.
/// 제품 표시 UI 최상단의 "미납 제품" 섹션에 표시됩니다.
class UnfulfilledItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 주문 수량 (BOX) — 서버 `BigDecimal` 정합으로 `double` (`RejectedItem` 과 동일)
  final double orderQuantityBoxes;

  /// 미납 사유 (SAP `LineItemStatus` 원문)
  final String reason;

  const UnfulfilledItem({
    required this.productCode,
    required this.productName,
    required this.orderQuantityBoxes,
    required this.reason,
  });

  UnfulfilledItem copyWith({
    String? productCode,
    String? productName,
    double? orderQuantityBoxes,
    String? reason,
  }) {
    return UnfulfilledItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      orderQuantityBoxes: orderQuantityBoxes ?? this.orderQuantityBoxes,
      reason: reason ?? this.reason,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'orderQuantityBoxes': orderQuantityBoxes,
      'reason': reason,
    };
  }

  factory UnfulfilledItem.fromJson(Map<String, dynamic> json) {
    return UnfulfilledItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      orderQuantityBoxes: (json['orderQuantityBoxes'] as num).toDouble(),
      reason: json['reason'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is UnfulfilledItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.orderQuantityBoxes == orderQuantityBoxes &&
        other.reason == reason;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      orderQuantityBoxes,
      reason,
    );
  }

  @override
  String toString() {
    return 'UnfulfilledItem(productCode: $productCode, '
        'productName: $productName, '
        'orderQuantityBoxes: $orderQuantityBoxes, '
        'reason: $reason)';
  }
}

/// 결품 제품 엔티티 (2026-07-23 사용자 결정 — 반려처럼 별도 섹션으로 분리)
///
/// SAP `DefaultReason` 코드가 결품셋({F1,L1,L2,L3})으로 분류된 제품. 기존에는 "주문한 제품" 목록에
/// 회색 배지로 인라인 표시했으나, 반려 섹션 다음에 전용 "결품 제품" 영역으로 분리하고 "주문한 제품"
/// 목록에서는 제외합니다. [reason] 은 `"{코드} {설명}"`(예: `"L1 [물류] 재고부족"`).
class OutOfStockItem {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 주문 수량 (BOX) — 서버 `BigDecimal` 정합으로 `double` (`RejectedItem` 과 동일)
  final double orderQuantityBoxes;

  /// 결품 사유 (`"{코드} {설명}"`)
  final String reason;

  const OutOfStockItem({
    required this.productCode,
    required this.productName,
    required this.orderQuantityBoxes,
    required this.reason,
  });

  OutOfStockItem copyWith({
    String? productCode,
    String? productName,
    double? orderQuantityBoxes,
    String? reason,
  }) {
    return OutOfStockItem(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      orderQuantityBoxes: orderQuantityBoxes ?? this.orderQuantityBoxes,
      reason: reason ?? this.reason,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'orderQuantityBoxes': orderQuantityBoxes,
      'reason': reason,
    };
  }

  factory OutOfStockItem.fromJson(Map<String, dynamic> json) {
    return OutOfStockItem(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      orderQuantityBoxes: (json['orderQuantityBoxes'] as num).toDouble(),
      reason: json['reason'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OutOfStockItem &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.orderQuantityBoxes == orderQuantityBoxes &&
        other.reason == reason;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      orderQuantityBoxes,
      reason,
    );
  }

  @override
  String toString() {
    return 'OutOfStockItem(productCode: $productCode, '
        'productName: $productName, '
        'orderQuantityBoxes: $orderQuantityBoxes, '
        'reason: $reason)';
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

  /// SAP 응답의 distinct SAP 주문번호 목록 (마감 무관, 헤더 조기 노출용). 없으면 빈 리스트.
  ///
  /// 처리현황(`orderProcessingStatusList`)은 마감 전 null 이지만, 이 목록은 SAP 응답이 있으면
  /// 마감 전에도 채워져 헤더에 콤마 나열로 표시된다. 한 주문이 여러 SAP 주문으로 분할되면 N개.
  final List<String> sapOrderNumbers;

  /// 반려 제품 목록 (반려 존재 시 — 레거시 동등으로 마감 전후 모두 표시)
  final List<RejectedItem>? rejectedItems;

  /// 결품 제품 목록 (2026-07-23 — 반려처럼 별도 섹션, 마감 전후 모두 표시. "주문한 제품"에서는 제외)
  final List<OutOfStockItem>? outOfStockItems;

  /// 미납 제품 목록 (신규 정책 — LineItemStatus != "OK" && SAP 주문번호 있음, 마감 전후 모두 표시)
  final List<UnfulfilledItem>? unfulfilledItems;

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
    this.sapOrderNumbers = const [],
    this.rejectedItems,
    this.outOfStockItems,
    this.unfulfilledItems,
  });

  /// SAP 주문번호가 하나라도 있는지 여부
  bool get hasSapOrderNumbers => sapOrderNumbers.isNotEmpty;

  /// 반려 제품이 있는지 여부
  bool get hasRejectedItems =>
      rejectedItems != null && rejectedItems!.isNotEmpty;

  /// 결품 제품이 있는지 여부
  bool get hasOutOfStockItems =>
      outOfStockItems != null && outOfStockItems!.isNotEmpty;

  /// 미납 제품이 있는지 여부
  bool get hasUnfulfilledItems =>
      unfulfilledItems != null && unfulfilledItems!.isNotEmpty;

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
    List<String>? sapOrderNumbers,
    List<RejectedItem>? rejectedItems,
    List<OutOfStockItem>? outOfStockItems,
    List<UnfulfilledItem>? unfulfilledItems,
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
      sapOrderNumbers: sapOrderNumbers ?? this.sapOrderNumbers,
      rejectedItems: rejectedItems ?? this.rejectedItems,
      outOfStockItems: outOfStockItems ?? this.outOfStockItems,
      unfulfilledItems: unfulfilledItems ?? this.unfulfilledItems,
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
      'sapOrderNumbers': sapOrderNumbers,
      'rejectedItems': rejectedItems?.map((e) => e.toJson()).toList(),
      'outOfStockItems': outOfStockItems?.map((e) => e.toJson()).toList(),
      'unfulfilledItems': unfulfilledItems?.map((e) => e.toJson()).toList(),
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
      sapOrderNumbers: (json['sapOrderNumbers'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const [],
      rejectedItems: json['rejectedItems'] != null
          ? (json['rejectedItems'] as List<dynamic>)
              .map((e) => RejectedItem.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
      outOfStockItems: json['outOfStockItems'] != null
          ? (json['outOfStockItems'] as List<dynamic>)
              .map((e) => OutOfStockItem.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
      unfulfilledItems: json['unfulfilledItems'] != null
          ? (json['unfulfilledItems'] as List<dynamic>)
              .map((e) => UnfulfilledItem.fromJson(e as Map<String, dynamic>))
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
    if (other.hasUnfulfilledItems != hasUnfulfilledItems) return false;
    if (hasUnfulfilledItems) {
      if (other.unfulfilledItems!.length != unfulfilledItems!.length) {
        return false;
      }
      for (var i = 0; i < unfulfilledItems!.length; i++) {
        if (other.unfulfilledItems![i] != unfulfilledItems![i]) return false;
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
      unfulfilledItems != null ? Object.hashAll(unfulfilledItems!) : null,
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
