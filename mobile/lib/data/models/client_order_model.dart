import '../../domain/entities/client_order.dart';
import '../../domain/entities/order_detail.dart';

/// 거래처별 주문 목록 항목 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ClientOrder 엔티티로 변환합니다.
class ClientOrderModel {
  final String sapOrderNumber;
  final int clientId;
  final String clientName;
  final int totalAmount;

  const ClientOrderModel({
    required this.sapOrderNumber,
    required this.clientId,
    required this.clientName,
    required this.totalAmount,
  });

  /// snake_case JSON에서 파싱
  factory ClientOrderModel.fromJson(Map<String, dynamic> json) {
    return ClientOrderModel(
      sapOrderNumber: json['sapOrderNumber'] as String,
      clientId: json['clientId'] as int,
      clientName: json['clientName'] as String,
      totalAmount: (json['totalAmount'] as num).toInt(),
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'clientId': clientId,
      'clientName': clientName,
      'totalAmount': totalAmount,
    };
  }

  /// Domain Entity로 변환
  ClientOrder toEntity() {
    return ClientOrder(
      sapOrderNumber: sapOrderNumber,
      clientId: clientId,
      clientName: clientName,
      totalAmount: totalAmount,
    );
  }

  /// Domain Entity에서 생성
  factory ClientOrderModel.fromEntity(ClientOrder entity) {
    return ClientOrderModel(
      sapOrderNumber: entity.sapOrderNumber,
      clientId: entity.clientId,
      clientName: entity.clientName,
      totalAmount: entity.totalAmount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrderModel &&
        other.sapOrderNumber == sapOrderNumber &&
        other.clientId == clientId &&
        other.clientName == clientName &&
        other.totalAmount == totalAmount;
  }

  @override
  int get hashCode {
    return Object.hash(sapOrderNumber, clientId, clientName, totalAmount);
  }

  @override
  String toString() {
    return 'ClientOrderModel(sapOrderNumber: $sapOrderNumber, '
        'clientId: $clientId, clientName: $clientName, '
        'totalAmount: $totalAmount)';
  }
}

/// 거래처별 주문 제품 API 모델 (DTO)
class ClientOrderItemModel {
  final String productCode;
  final String productName;
  final String deliveredQuantity;
  final String deliveryStatus;

  /// 배송 정보 5필드 (배송중/배송완료 라인 탭 팝업용). 서버가 빈 값/`'000000'` sentinel 을
  /// `null` 로 매핑한 결과를 그대로 수신한다.
  final String? driverName;
  final String? vehicle;
  final String? driverPhone;
  final String? scheduleTime;
  final String? completeTime;

  const ClientOrderItemModel({
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

  factory ClientOrderItemModel.fromJson(Map<String, dynamic> json) {
    return ClientOrderItemModel(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      deliveredQuantity: json['deliveredQuantity'] as String,
      deliveryStatus: json['deliveryStatus'] as String,
      driverName: json['driverName'] as String?,
      vehicle: json['vehicle'] as String?,
      driverPhone: json['driverPhone'] as String?,
      scheduleTime: json['scheduleTime'] as String?,
      completeTime: json['completeTime'] as String?,
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

  ClientOrderItem toEntity() {
    return ClientOrderItem(
      productCode: productCode,
      productName: productName,
      deliveredQuantity: deliveredQuantity,
      deliveryStatus: DeliveryStatus.fromCode(deliveryStatus),
      driverName: driverName,
      vehicle: vehicle,
      driverPhone: driverPhone,
      scheduleTime: scheduleTime,
      completeTime: completeTime,
    );
  }

  factory ClientOrderItemModel.fromEntity(ClientOrderItem entity) {
    return ClientOrderItemModel(
      productCode: entity.productCode,
      productName: entity.productName,
      deliveredQuantity: entity.deliveredQuantity,
      deliveryStatus: entity.deliveryStatus.code,
      driverName: entity.driverName,
      vehicle: entity.vehicle,
      driverPhone: entity.driverPhone,
      scheduleTime: entity.scheduleTime,
      completeTime: entity.completeTime,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClientOrderItemModel &&
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
    return Object.hash(productCode, productName, deliveredQuantity,
        deliveryStatus, driverName, vehicle, driverPhone, scheduleTime,
        completeTime);
  }

  @override
  String toString() {
    return 'ClientOrderItemModel(productCode: $productCode, '
        'productName: $productName, '
        'deliveredQuantity: $deliveredQuantity, '
        'deliveryStatus: $deliveryStatus)';
  }
}

/// 거래처별 주문 상세 API 모델 (DTO)
class ClientOrderDetailModel {
  final String sapOrderNumber;
  final String? sapAccountCode;
  final String? sapAccountName;
  final String? clientDeadlineTime;
  final String? orderDate;
  final String? deliveryDate;
  final int? totalApprovedAmount;
  final String? ordererName;
  final String? ordererCode;
  final int orderedItemCount;
  final List<ClientOrderItemModel> orderedItems;

  const ClientOrderDetailModel({
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

  factory ClientOrderDetailModel.fromJson(Map<String, dynamic> json) {
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;
    final itemsJson = data['orderedItems'] as List<dynamic>? ?? [];

    return ClientOrderDetailModel(
      sapOrderNumber: data['sapOrderNumber'] as String,
      sapAccountCode: data['sapAccountCode'] as String?,
      sapAccountName: data['sapAccountName'] as String?,
      clientDeadlineTime: data['clientDeadlineTime'] as String?,
      orderDate: data['orderDate'] as String?,
      deliveryDate: data['deliveryDate'] as String?,
      totalApprovedAmount: (data['totalApprovedAmount'] as num?)?.toInt(),
      ordererName: data['ordererName'] as String?,
      ordererCode: data['ordererCode'] as String?,
      orderedItemCount: (data['orderedItemCount'] as num).toInt(),
      orderedItems: itemsJson
          .map((e) =>
              ClientOrderItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sapOrderNumber': sapOrderNumber,
      'sapAccountCode': sapAccountCode,
      'sapAccountName': sapAccountName,
      'clientDeadlineTime': clientDeadlineTime,
      'orderDate': orderDate,
      'deliveryDate': deliveryDate,
      'totalApprovedAmount': totalApprovedAmount,
      'ordererName': ordererName,
      'ordererCode': ordererCode,
      'orderedItemCount': orderedItemCount,
      'orderedItems': orderedItems.map((e) => e.toJson()).toList(),
    };
  }

  ClientOrderDetail toEntity() {
    return ClientOrderDetail(
      sapOrderNumber: sapOrderNumber,
      sapAccountCode: sapAccountCode,
      sapAccountName: sapAccountName,
      clientDeadlineTime: clientDeadlineTime,
      orderDate: orderDate != null ? DateTime.parse(orderDate!) : null,
      deliveryDate:
          deliveryDate != null ? DateTime.parse(deliveryDate!) : null,
      totalApprovedAmount: totalApprovedAmount,
      ordererName: ordererName,
      ordererCode: ordererCode,
      orderedItemCount: orderedItemCount,
      orderedItems: orderedItems.map((e) => e.toEntity()).toList(),
    );
  }

  factory ClientOrderDetailModel.fromEntity(ClientOrderDetail entity) {
    return ClientOrderDetailModel(
      sapOrderNumber: entity.sapOrderNumber,
      sapAccountCode: entity.sapAccountCode,
      sapAccountName: entity.sapAccountName,
      clientDeadlineTime: entity.clientDeadlineTime,
      orderDate: entity.orderDate?.toIso8601String().split('T')[0],
      deliveryDate: entity.deliveryDate?.toIso8601String().split('T')[0],
      totalApprovedAmount: entity.totalApprovedAmount,
      ordererName: entity.ordererName,
      ordererCode: entity.ordererCode,
      orderedItemCount: entity.orderedItemCount,
      orderedItems: entity.orderedItems
          .map((e) => ClientOrderItemModel.fromEntity(e))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClientOrderDetailModel) return false;
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
    return 'ClientOrderDetailModel(sapOrderNumber: $sapOrderNumber, '
        'sapAccountName: $sapAccountName, '
        'orderedItemCount: $orderedItemCount)';
  }
}

/// 거래처별 주문 목록 API 응답 모델
///
/// 페이지네이션을 포함한 거래처별 주문 목록 응답을 파싱합니다.
class ClientOrderListResponseModel {
  final List<ClientOrderModel> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  final bool first;
  final bool last;

  const ClientOrderListResponseModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.first,
    required this.last,
  });

  /// API 응답 JSON에서 파싱
  factory ClientOrderListResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final contentJson = data['content'] as List<dynamic>? ?? [];
    final content = contentJson
        .map((e) => ClientOrderModel.fromJson(e as Map<String, dynamic>))
        .toList();

    return ClientOrderListResponseModel(
      content: content,
      totalElements: data['totalElements'] as int,
      totalPages: data['totalPages'] as int,
      number: data['number'] as int,
      size: data['size'] as int,
      first: data['first'] as bool,
      last: data['last'] as bool,
    );
  }
}
