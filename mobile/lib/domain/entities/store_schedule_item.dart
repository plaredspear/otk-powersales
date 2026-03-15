/// 거래처 일정 엔티티
///
/// 오늘 출근해야 할 거래처 정보를 나타냅니다.
class StoreScheduleItem {
  final int scheduleId;
  final String? storeSfid;
  final String storeName;
  final String? storeTypeCode;
  final String workCategory;
  final String address;
  final double? latitude;
  final double? longitude;
  final bool isRegistered;
  final String? registeredWorkType;

  const StoreScheduleItem({
    required this.scheduleId,
    this.storeSfid,
    required this.storeName,
    this.storeTypeCode,
    required this.workCategory,
    required this.address,
    this.latitude,
    this.longitude,
    required this.isRegistered,
    this.registeredWorkType,
  });

  StoreScheduleItem copyWith({
    int? scheduleId,
    String? storeSfid,
    String? storeName,
    String? storeTypeCode,
    String? workCategory,
    String? address,
    double? latitude,
    double? longitude,
    bool? isRegistered,
    String? registeredWorkType,
  }) {
    return StoreScheduleItem(
      scheduleId: scheduleId ?? this.scheduleId,
      storeSfid: storeSfid ?? this.storeSfid,
      storeName: storeName ?? this.storeName,
      storeTypeCode: storeTypeCode ?? this.storeTypeCode,
      workCategory: workCategory ?? this.workCategory,
      address: address ?? this.address,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      isRegistered: isRegistered ?? this.isRegistered,
      registeredWorkType: registeredWorkType ?? this.registeredWorkType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleId': scheduleId,
      'storeSfid': storeSfid,
      'storeName': storeName,
      'storeTypeCode': storeTypeCode,
      'workCategory': workCategory,
      'address': address,
      'latitude': latitude,
      'longitude': longitude,
      'isRegistered': isRegistered,
      'registeredWorkType': registeredWorkType,
    };
  }

  factory StoreScheduleItem.fromJson(Map<String, dynamic> json) {
    return StoreScheduleItem(
      scheduleId: json['scheduleId'] as int,
      storeSfid: json['storeSfid'] as String?,
      storeName: json['storeName'] as String,
      storeTypeCode: json['storeTypeCode'] as String?,
      workCategory: json['workCategory'] as String,
      address: json['address'] as String? ?? '',
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      isRegistered: json['isRegistered'] as bool,
      registeredWorkType: json['registeredWorkType'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is StoreScheduleItem &&
        other.scheduleId == scheduleId &&
        other.storeName == storeName &&
        other.workCategory == workCategory &&
        other.address == address &&
        other.isRegistered == isRegistered;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      storeName,
      workCategory,
      address,
      isRegistered,
    );
  }

  @override
  String toString() {
    return 'StoreScheduleItem(scheduleId: $scheduleId, storeName: $storeName, '
        'workCategory: $workCategory, address: $address, '
        'isRegistered: $isRegistered)';
  }
}
