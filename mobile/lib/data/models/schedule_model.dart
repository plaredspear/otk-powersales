import '../../domain/entities/schedule.dart';

/// Schedule API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class ScheduleModel {
  final String scheduleId;
  final String employeeName;
  final String employeeSfid;
  final String? storeName;
  final String? storeSfid;
  final String workCategory;
  final String? workType;
  final bool isCommuteRegistered;
  final DateTime? commuteRegisteredAt;

  const ScheduleModel({
    required this.scheduleId,
    required this.employeeName,
    required this.employeeSfid,
    this.storeName,
    this.storeSfid,
    required this.workCategory,
    this.workType,
    required this.isCommuteRegistered,
    this.commuteRegisteredAt,
  });

  factory ScheduleModel.fromJson(Map<String, dynamic> json) {
    return ScheduleModel(
      scheduleId: json['schedule_id'] as String,
      employeeName: json['employee_name'] as String,
      employeeSfid: json['employee_sfid'] as String,
      storeName: json['store_name'] as String?,
      storeSfid: json['store_sfid'] as String?,
      workCategory: json['work_category'] as String,
      workType: json['work_type'] as String?,
      isCommuteRegistered: json['is_commute_registered'] as bool,
      commuteRegisteredAt: json['commute_registered_at'] != null
          ? DateTime.parse(json['commute_registered_at'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'schedule_id': scheduleId,
      'employee_name': employeeName,
      'employee_sfid': employeeSfid,
      'store_name': storeName,
      'store_sfid': storeSfid,
      'work_category': workCategory,
      'work_type': workType,
      'is_commute_registered': isCommuteRegistered,
      'commute_registered_at': commuteRegisteredAt?.toIso8601String(),
    };
  }

  Schedule toEntity() {
    return Schedule(
      scheduleId: scheduleId,
      employeeName: employeeName,
      employeeSfid: employeeSfid,
      storeName: storeName,
      storeSfid: storeSfid,
      workCategory: workCategory,
      workType: workType,
      isCommuteRegistered: isCommuteRegistered,
      commuteRegisteredAt: commuteRegisteredAt,
    );
  }

  factory ScheduleModel.fromEntity(Schedule entity) {
    return ScheduleModel(
      scheduleId: entity.scheduleId,
      employeeName: entity.employeeName,
      employeeSfid: entity.employeeSfid,
      storeName: entity.storeName,
      storeSfid: entity.storeSfid,
      workCategory: entity.workCategory,
      workType: entity.workType,
      isCommuteRegistered: entity.isCommuteRegistered,
      commuteRegisteredAt: entity.commuteRegisteredAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ScheduleModel &&
        other.scheduleId == scheduleId &&
        other.employeeName == employeeName &&
        other.employeeSfid == employeeSfid &&
        other.storeName == storeName &&
        other.storeSfid == storeSfid &&
        other.workCategory == workCategory &&
        other.workType == workType &&
        other.isCommuteRegistered == isCommuteRegistered &&
        other.commuteRegisteredAt == commuteRegisteredAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      employeeName,
      employeeSfid,
      storeName,
      storeSfid,
      workCategory,
      workType,
      isCommuteRegistered,
      commuteRegisteredAt,
    );
  }

  @override
  String toString() {
    return 'ScheduleModel(scheduleId: $scheduleId, employeeName: $employeeName, storeName: $storeName, workCategory: $workCategory, isCommuteRegistered: $isCommuteRegistered)';
  }
}
