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
      eventId: json['eventId'] as String,
      salesDate: json['salesDate'] as String,
      mainProductPrice: json['mainProductPrice'] as int?,
      mainProductQuantity: json['mainProductQuantity'] as int?,
      mainProductAmount: json['mainProductAmount'] as int?,
      subProductCode: json['subProductCode'] as String?,
      subProductName: json['subProductName'] as String?,
      subProductQuantity: json['subProductQuantity'] as int?,
      subProductAmount: json['subProductAmount'] as int?,
      photoUrl: json['photoUrl'] as String?,
      status: json['status'] as String,
      registeredAt: json['registeredAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'eventId': eventId,
      'salesDate': salesDate,
      'mainProductPrice': mainProductPrice,
      'mainProductQuantity': mainProductQuantity,
      'mainProductAmount': mainProductAmount,
      'subProductCode': subProductCode,
      'subProductName': subProductName,
      'subProductQuantity': subProductQuantity,
      'subProductAmount': subProductAmount,
      'photoUrl': photoUrl,
      'status': status,
      'registeredAt': registeredAt,
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
