/// 거래처 일정 엔티티
///
/// 오늘 출근해야 할 거래처 정보를 나타냅니다.
class AccountScheduleItem {
  final int scheduleId;
  final int? displayWorkScheduleId;
  final int? accountId;
  final String accountName;
  final String? accountTypeCode;
  final String workCategory;
  final String? workCategory2;
  final String? workCategory3;
  final String address;
  final double? latitude;
  final double? longitude;
  final bool isRegistered;
  final String? registeredWorkType;
  final String source;

  const AccountScheduleItem({
    required this.scheduleId,
    this.displayWorkScheduleId,
    this.accountId,
    required this.accountName,
    this.accountTypeCode,
    required this.workCategory,
    this.workCategory2,
    this.workCategory3,
    required this.address,
    this.latitude,
    this.longitude,
    required this.isRegistered,
    this.registeredWorkType,
    this.source = 'schedule',
  });

  AccountScheduleItem copyWith({
    int? scheduleId,
    int? displayWorkScheduleId,
    int? accountId,
    String? accountName,
    String? accountTypeCode,
    String? workCategory,
    String? workCategory2,
    String? workCategory3,
    String? address,
    double? latitude,
    double? longitude,
    bool? isRegistered,
    String? registeredWorkType,
    String? source,
  }) {
    return AccountScheduleItem(
      scheduleId: scheduleId ?? this.scheduleId,
      displayWorkScheduleId:
          displayWorkScheduleId ?? this.displayWorkScheduleId,
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
      accountTypeCode: accountTypeCode ?? this.accountTypeCode,
      workCategory: workCategory ?? this.workCategory,
      workCategory2: workCategory2 ?? this.workCategory2,
      workCategory3: workCategory3 ?? this.workCategory3,
      address: address ?? this.address,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      isRegistered: isRegistered ?? this.isRegistered,
      registeredWorkType: registeredWorkType ?? this.registeredWorkType,
      source: source ?? this.source,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleId': scheduleId,
      'displayWorkScheduleId': displayWorkScheduleId,
      'accountId': accountId,
      'accountName': accountName,
      'accountTypeCode': accountTypeCode,
      'workCategory': workCategory,
      'workCategory2': workCategory2,
      'workCategory3': workCategory3,
      'address': address,
      'latitude': latitude,
      'longitude': longitude,
      'isRegistered': isRegistered,
      'registeredWorkType': registeredWorkType,
      'source': source,
    };
  }

  factory AccountScheduleItem.fromJson(Map<String, dynamic> json) {
    return AccountScheduleItem(
      scheduleId: json['scheduleId'] as int,
      displayWorkScheduleId: json['displayWorkScheduleId'] as int?,
      accountId: json['accountId'] as int?,
      accountName: json['accountName'] as String,
      accountTypeCode: json['accountTypeCode'] as String?,
      workCategory: json['workCategory'] as String,
      workCategory2: json['workCategory2'] as String?,
      workCategory3: json['workCategory3'] as String?,
      address: json['address'] as String? ?? '',
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      isRegistered: json['isRegistered'] as bool,
      registeredWorkType: json['registeredWorkType'] as String?,
      source: json['source'] as String? ?? 'schedule',
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AccountScheduleItem &&
        other.scheduleId == scheduleId &&
        other.accountName == accountName &&
        other.workCategory == workCategory &&
        other.address == address &&
        other.isRegistered == isRegistered;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      accountName,
      workCategory,
      address,
      isRegistered,
    );
  }

  @override
  String toString() {
    return 'AccountScheduleItem(scheduleId: $scheduleId, accountName: $accountName, '
        'workCategory: $workCategory, address: $address, '
        'isRegistered: $isRegistered, source: $source)';
  }
}
