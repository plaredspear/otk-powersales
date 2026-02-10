/// 거래처 일정 엔티티
///
/// 오늘 출근해야 할 거래처 정보를 나타냅니다.
class StoreScheduleItem {
  final int storeId;
  final String storeName;
  final String storeCode;
  final String workCategory;
  final String address;
  final bool isRegistered;
  final String? registeredWorkType;

  const StoreScheduleItem({
    required this.storeId,
    required this.storeName,
    required this.storeCode,
    required this.workCategory,
    required this.address,
    required this.isRegistered,
    this.registeredWorkType,
  });

  StoreScheduleItem copyWith({
    int? storeId,
    String? storeName,
    String? storeCode,
    String? workCategory,
    String? address,
    bool? isRegistered,
    String? registeredWorkType,
  }) {
    return StoreScheduleItem(
      storeId: storeId ?? this.storeId,
      storeName: storeName ?? this.storeName,
      storeCode: storeCode ?? this.storeCode,
      workCategory: workCategory ?? this.workCategory,
      address: address ?? this.address,
      isRegistered: isRegistered ?? this.isRegistered,
      registeredWorkType: registeredWorkType ?? this.registeredWorkType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'storeId': storeId,
      'storeName': storeName,
      'storeCode': storeCode,
      'workCategory': workCategory,
      'address': address,
      'isRegistered': isRegistered,
      'registeredWorkType': registeredWorkType,
    };
  }

  factory StoreScheduleItem.fromJson(Map<String, dynamic> json) {
    return StoreScheduleItem(
      storeId: json['storeId'] as int,
      storeName: json['storeName'] as String,
      storeCode: json['storeCode'] as String,
      workCategory: json['workCategory'] as String,
      address: json['address'] as String,
      isRegistered: json['isRegistered'] as bool,
      registeredWorkType: json['registeredWorkType'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is StoreScheduleItem &&
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
    return 'StoreScheduleItem(storeId: $storeId, storeName: $storeName, '
        'storeCode: $storeCode, workCategory: $workCategory, '
        'address: $address, isRegistered: $isRegistered, '
        'registeredWorkType: $registeredWorkType)';
  }
}
