import '../../domain/entities/shelf_life_item.dart';

/// 유통기한 항목 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ShelfLifeItem 엔티티로 변환합니다.
class ShelfLifeItemModel {
  final int id;
  final String productCode;
  final String productName;
  final String storeName;
  final int storeId;
  final String expiryDate;
  final String alertDate;
  final int dDay;
  final String description;
  final bool isExpired;

  const ShelfLifeItemModel({
    required this.id,
    required this.productCode,
    required this.productName,
    required this.storeName,
    required this.storeId,
    required this.expiryDate,
    required this.alertDate,
    required this.dDay,
    this.description = '',
    required this.isExpired,
  });

  /// snake_case JSON에서 파싱
  factory ShelfLifeItemModel.fromJson(Map<String, dynamic> json) {
    return ShelfLifeItemModel(
      id: json['id'] as int,
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      storeName: json['store_name'] as String,
      storeId: json['store_id'] as int,
      expiryDate: json['expiry_date'] as String,
      alertDate: json['alert_date'] as String,
      dDay: json['d_day'] as int,
      description: json['description'] as String? ?? '',
      isExpired: json['is_expired'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'product_code': productCode,
      'product_name': productName,
      'store_name': storeName,
      'store_id': storeId,
      'expiry_date': expiryDate,
      'alert_date': alertDate,
      'd_day': dDay,
      'description': description,
      'is_expired': isExpired,
    };
  }

  /// Domain Entity로 변환
  ShelfLifeItem toEntity() {
    return ShelfLifeItem(
      id: id,
      productCode: productCode,
      productName: productName,
      storeName: storeName,
      storeId: storeId,
      expiryDate: DateTime.parse(expiryDate),
      alertDate: DateTime.parse(alertDate),
      dDay: dDay,
      description: description,
      isExpired: isExpired,
    );
  }

  /// Domain Entity에서 생성
  factory ShelfLifeItemModel.fromEntity(ShelfLifeItem entity) {
    return ShelfLifeItemModel(
      id: entity.id,
      productCode: entity.productCode,
      productName: entity.productName,
      storeName: entity.storeName,
      storeId: entity.storeId,
      expiryDate: entity.expiryDate.toIso8601String().substring(0, 10),
      alertDate: entity.alertDate.toIso8601String().substring(0, 10),
      dDay: entity.dDay,
      description: entity.description,
      isExpired: entity.isExpired,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeItemModel) return false;
    return other.id == id &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.storeName == storeName &&
        other.storeId == storeId &&
        other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.dDay == dDay &&
        other.description == description &&
        other.isExpired == isExpired;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      productCode,
      productName,
      storeName,
      storeId,
      expiryDate,
      alertDate,
      dDay,
      description,
      isExpired,
    );
  }

  @override
  String toString() {
    return 'ShelfLifeItemModel(id: $id, productCode: $productCode, '
        'productName: $productName, storeName: $storeName, '
        'storeId: $storeId, expiryDate: $expiryDate, '
        'alertDate: $alertDate, dDay: $dDay, '
        'description: $description, isExpired: $isExpired)';
  }
}
