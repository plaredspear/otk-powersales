/// 거래처 일정 엔티티
///
/// 오늘 출근해야 할 거래처 정보를 나타냅니다.
class StoreScheduleItem {
  final String scheduleSfid;
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
    required this.scheduleSfid,
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
    String? scheduleSfid,
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
      scheduleSfid: scheduleSfid ?? this.scheduleSfid,
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
      'scheduleSfid': scheduleSfid,
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
      scheduleSfid: json['scheduleSfid'] as String,
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
        other.scheduleSfid == scheduleSfid &&
        other.storeName == storeName &&
        other.workCategory == workCategory &&
        other.address == address &&
        other.isRegistered == isRegistered;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleSfid,
      storeName,
      workCategory,
      address,
      isRegistered,
    );
  }

  @override
  String toString() {
    return 'StoreScheduleItem(scheduleSfid: $scheduleSfid, storeName: $storeName, '
        'workCategory: $workCategory, address: $address, '
        'isRegistered: $isRegistered)';
  }
}
