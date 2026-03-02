import '../../domain/entities/store_schedule_item.dart';

/// 거래처 일정 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 StoreScheduleItem 엔티티로 변환합니다.
class StoreScheduleItemModel {
  final String scheduleSfid;
  final String? storeSfid;
  final String storeName;
  final String? storeTypeCode;
  final String workCategory;
  final String? address;
  final double? latitude;
  final double? longitude;
  final bool isRegistered;

  const StoreScheduleItemModel({
    required this.scheduleSfid,
    this.storeSfid,
    required this.storeName,
    this.storeTypeCode,
    required this.workCategory,
    this.address,
    this.latitude,
    this.longitude,
    required this.isRegistered,
  });

  /// snake_case JSON에서 파싱
  factory StoreScheduleItemModel.fromJson(Map<String, dynamic> json) {
    return StoreScheduleItemModel(
      scheduleSfid: json['schedule_sfid'] as String,
      storeSfid: json['store_sfid'] as String?,
      storeName: json['store_name'] as String,
      storeTypeCode: json['store_type_code'] as String?,
      workCategory: json['work_category'] as String,
      address: json['address'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      isRegistered: json['is_registered'] as bool,
    );
  }

  /// Domain Entity로 변환
  StoreScheduleItem toEntity() {
    return StoreScheduleItem(
      scheduleSfid: scheduleSfid,
      storeSfid: storeSfid,
      storeName: storeName,
      storeTypeCode: storeTypeCode,
      workCategory: workCategory,
      address: address ?? '',
      latitude: latitude,
      longitude: longitude,
      isRegistered: isRegistered,
    );
  }
}
