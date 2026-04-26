import '../../domain/entities/daily_sales.dart';

/// DailySales API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환합니다.
class DailySalesModel {
  final String id;
  final String eventId;
  final String salesDate;
  final int? mainProductPrice;
  final int? mainProductQuantity;
  final int? mainProductAmount;
  final String? subProductCode;
  final String? subProductName;
  final int? subProductQuantity;
  final int? subProductAmount;
  final String? photoUrl;
  final String status;
  final String? registeredAt;

  const DailySalesModel({
    required this.id,
    required this.eventId,
    required this.salesDate,
    this.mainProductPrice,
    this.mainProductQuantity,
    this.mainProductAmount,
    this.subProductCode,
    this.subProductName,
    this.subProductQuantity,
    this.subProductAmount,
    this.photoUrl,
    required this.status,
    this.registeredAt,
  });

  factory DailySalesModel.fromJson(Map<String, dynamic> json) {
    return DailySalesModel(
      id: json['id'] as String,
      eventId: json['event_id'] as String,
      salesDate: json['sales_date'] as String,
      mainProductPrice: json['main_product_price'] as int?,
      mainProductQuantity: json['main_product_quantity'] as int?,
      mainProductAmount: json['main_product_amount'] as int?,
      subProductCode: json['sub_product_code'] as String?,
      subProductName: json['sub_product_name'] as String?,
      subProductQuantity: json['sub_product_quantity'] as int?,
      subProductAmount: json['sub_product_amount'] as int?,
      photoUrl: json['photo_url'] as String?,
      status: json['status'] as String,
      registeredAt: json['registered_at'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'event_id': eventId,
      'sales_date': salesDate,
      'main_product_price': mainProductPrice,
      'main_product_quantity': mainProductQuantity,
      'main_product_amount': mainProductAmount,
      'sub_product_code': subProductCode,
      'sub_product_name': subProductName,
      'sub_product_quantity': subProductQuantity,
      'sub_product_amount': subProductAmount,
      'photo_url': photoUrl,
      'status': status,
      'registered_at': registeredAt,
    };
  }

  /// Model을 Entity로 변환
  DailySales toEntity() {
    return DailySales(
      id: id,
      eventId: eventId,
      salesDate: DateTime.parse(salesDate),
      mainProductPrice: mainProductPrice,
      mainProductQuantity: mainProductQuantity,
      mainProductAmount: mainProductAmount,
      subProductCode: subProductCode,
      subProductName: subProductName,
      subProductQuantity: subProductQuantity,
      subProductAmount: subProductAmount,
      photoUrl: photoUrl,
      status: DailySalesStatus.fromString(status),
      registeredAt:
          registeredAt != null ? DateTime.parse(registeredAt!) : null,
    );
  }

  /// Entity에서 Model 생성
  factory DailySalesModel.fromEntity(DailySales entity) {
    return DailySalesModel(
      id: entity.id,
      eventId: entity.eventId,
      salesDate: entity.salesDate.toIso8601String(),
      mainProductPrice: entity.mainProductPrice,
      mainProductQuantity: entity.mainProductQuantity,
      mainProductAmount: entity.mainProductAmount,
      subProductCode: entity.subProductCode,
      subProductName: entity.subProductName,
      subProductQuantity: entity.subProductQuantity,
      subProductAmount: entity.subProductAmount,
      photoUrl: entity.photoUrl,
      status: entity.status.value,
      registeredAt: entity.registeredAt?.toIso8601String(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailySalesModel &&
        other.id == id &&
        other.eventId == eventId &&
        other.salesDate == salesDate &&
        other.mainProductPrice == mainProductPrice &&
        other.mainProductQuantity == mainProductQuantity &&
        other.mainProductAmount == mainProductAmount &&
        other.subProductCode == subProductCode &&
        other.subProductName == subProductName &&
        other.subProductQuantity == subProductQuantity &&
        other.subProductAmount == subProductAmount &&
        other.photoUrl == photoUrl &&
        other.status == status &&
        other.registeredAt == registeredAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      eventId,
      salesDate,
      mainProductPrice,
      mainProductQuantity,
      mainProductAmount,
      subProductCode,
      subProductName,
      subProductQuantity,
      subProductAmount,
      photoUrl,
      status,
      registeredAt,
    );
  }

  @override
  String toString() {
    return 'DailySalesModel(id: $id, eventId: $eventId, salesDate: $salesDate, '
        'mainProductPrice: $mainProductPrice, mainProductQuantity: $mainProductQuantity, '
        'mainProductAmount: $mainProductAmount, '
        'subProductCode: $subProductCode, subProductName: $subProductName, '
        'subProductQuantity: $subProductQuantity, subProductAmount: $subProductAmount, '
        'photoUrl: $photoUrl, status: $status, registeredAt: $registeredAt)';
  }
}
