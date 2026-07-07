import '../../domain/entities/account_schedule_item.dart';

/// 거래처 일정 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AccountScheduleItem 엔티티로 변환합니다.
class AccountScheduleItemModel {
  final int scheduleId;
  final int? displayWorkScheduleId;
  final int? accountId;
  final String accountName;
  final String? accountTypeCode;
  final String workCategory;
  final String? workCategory2;
  final String? workCategory3;
  final String? address;
  final double? latitude;
  final double? longitude;
  final bool isRegistered;
  final String source;

  const AccountScheduleItemModel({
    required this.scheduleId,
    this.displayWorkScheduleId,
    this.accountId,
    required this.accountName,
    this.accountTypeCode,
    required this.workCategory,
    this.workCategory2,
    this.workCategory3,
    this.address,
    this.latitude,
    this.longitude,
    required this.isRegistered,
    this.source = 'schedule',
  });

  /// snake_case JSON에서 파싱
  factory AccountScheduleItemModel.fromJson(Map<String, dynamic> json) {
    return AccountScheduleItemModel(
      scheduleId: (json['scheduleId'] as int?) ?? 0,
      displayWorkScheduleId: json['displayWorkScheduleId'] as int?,
      accountId: json['accountId'] as int?,
      accountName: json['accountName'] as String,
      accountTypeCode: json['accountTypeCode'] as String?,
      workCategory: json['workCategory'] as String,
      workCategory2: json['workCategory2'] as String?,
      workCategory3: json['workCategory3'] as String?,
      address: json['address'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      isRegistered: json['isRegistered'] as bool,
      source: json['source'] as String? ?? 'schedule',
    );
  }

  /// Domain Entity로 변환
  AccountScheduleItem toEntity() {
    return AccountScheduleItem(
      scheduleId: scheduleId,
      displayWorkScheduleId: displayWorkScheduleId,
      accountId: accountId,
      accountName: accountName,
      accountTypeCode: accountTypeCode,
      workCategory: workCategory,
      workCategory2: workCategory2,
      workCategory3: workCategory3,
      address: address ?? '',
      latitude: latitude,
      longitude: longitude,
      isRegistered: isRegistered,
      source: source,
    );
  }
}
