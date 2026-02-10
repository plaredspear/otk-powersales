import '../../domain/entities/store_schedule_item.dart';

/// 거래처 일정 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 StoreScheduleItem 엔티티로 변환합니다.
class StoreScheduleItemModel {
  final int storeId;
  final String storeName;
  final String storeCode;
  final String workCategory;
  final String address;
  final bool isRegistered;
  final String? registeredWorkType;

  const StoreScheduleItemModel({
    required this.storeId,
    required this.storeName,
    required this.storeCode,
    required this.workCategory,
    required this.address,
    required this.isRegistered,
    this.registeredWorkType,
  });

  /// snake_case JSON에서 파싱
  factory StoreScheduleItemModel.fromJson(Map<String, dynamic> json) {
    return StoreScheduleItemModel(
      storeId: json['store_id'] as int,
      storeName: json['store_name'] as String,
      storeCode: json['store_code'] as String,
      workCategory: json['work_category'] as String,
      address: json['address'] as String,
      isRegistered: json['is_registered'] as bool,
      registeredWorkType: json['registered_work_type'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'store_id': storeId,
      'store_name': storeName,
      'store_code': storeCode,
      'work_category': workCategory,
      'address': address,
      'is_registered': isRegistered,
      'registered_work_type': registeredWorkType,
    };
  }

  /// Domain Entity로 변환
  StoreScheduleItem toEntity() {
    return StoreScheduleItem(
      storeId: storeId,
      storeName: storeName,
      storeCode: storeCode,
      workCategory: workCategory,
      address: address,
      isRegistered: isRegistered,
      registeredWorkType: registeredWorkType,
    );
  }

  /// Domain Entity에서 생성
  factory StoreScheduleItemModel.fromEntity(StoreScheduleItem entity) {
    return StoreScheduleItemModel(
      storeId: entity.storeId,
      storeName: entity.storeName,
      storeCode: entity.storeCode,
      workCategory: entity.workCategory,
      address: entity.address,
      isRegistered: entity.isRegistered,
      registeredWorkType: entity.registeredWorkType,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is StoreScheduleItemModel &&
        other.storeId == storeId &&
        other.storeName == storeName &&
        other.storeCode == storeCode &&
        other.workCategory == workCategory &&
        other.address == address &&
        other.isRegistered == isRegistered &&
        other.registeredWorkType == registeredWorkType;
  }

  @override
  int get hashCode {
    return Object.hash(
      storeId,
      storeName,
      storeCode,
      workCategory,
      address,
      isRegistered,
      registeredWorkType,
    );
  }

  @override
  String toString() {
    return 'StoreScheduleItemModel(storeId: $storeId, storeName: $storeName, '
        'storeCode: $storeCode, workCategory: $workCategory, '
        'address: $address, isRegistered: $isRegistered, '
        'registeredWorkType: $registeredWorkType)';
  }
}
