import '../../domain/entities/schedule.dart';

/// Schedule API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class ScheduleModel {
  final int scheduleId;
  final int? displayWorkScheduleId;
  final String employeeName;
  final String employeeCode;
  final String? accountName;
  final int? accountId;
  final String workCategory;
  final String? workType;
  final bool isCommuteRegistered;
  final DateTime? commuteRegisteredAt;

  const ScheduleModel({
    required this.scheduleId,
    this.displayWorkScheduleId,
    required this.employeeName,
    required this.employeeCode,
    this.accountName,
    this.accountId,
    required this.workCategory,
    this.workType,
    required this.isCommuteRegistered,
    this.commuteRegisteredAt,
  });

  factory ScheduleModel.fromJson(Map<String, dynamic> json) {
    return ScheduleModel(
      scheduleId: json['schedule_id'] as int,
      displayWorkScheduleId: json['display_work_schedule_id'] as int?,
      employeeName: json['employee_name'] as String,
      employeeCode: json['employee_code'] as String,
      accountName: json['account_name'] as String?,
      accountId: json['account_id'] as int?,
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
      'display_work_schedule_id': displayWorkScheduleId,
      'employee_name': employeeName,
      'employee_code': employeeCode,
      'account_name': accountName,
      'account_id': accountId,
      'work_category': workCategory,
      'work_type': workType,
      'is_commute_registered': isCommuteRegistered,
      'commute_registered_at': commuteRegisteredAt?.toIso8601String(),
    };
  }

  Schedule toEntity() {
    return Schedule(
      scheduleId: scheduleId,
      displayWorkScheduleId: displayWorkScheduleId,
      employeeName: employeeName,
      employeeCode: employeeCode,
      accountName: accountName,
      accountId: accountId,
      workCategory: workCategory,
      workType: workType,
      isCommuteRegistered: isCommuteRegistered,
      commuteRegisteredAt: commuteRegisteredAt,
    );
  }

  factory ScheduleModel.fromEntity(Schedule entity) {
    return ScheduleModel(
      scheduleId: entity.scheduleId,
      displayWorkScheduleId: entity.displayWorkScheduleId,
      employeeName: entity.employeeName,
      employeeCode: entity.employeeCode,
      accountName: entity.accountName,
      accountId: entity.accountId,
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
        other.displayWorkScheduleId == displayWorkScheduleId &&
        other.employeeName == employeeName &&
        other.employeeCode == employeeCode &&
        other.accountName == accountName &&
        other.accountId == accountId &&
        other.workCategory == workCategory &&
        other.workType == workType &&
        other.isCommuteRegistered == isCommuteRegistered &&
        other.commuteRegisteredAt == commuteRegisteredAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      displayWorkScheduleId,
      employeeName,
      employeeCode,
      accountName,
      accountId,
      workCategory,
      workType,
      isCommuteRegistered,
      commuteRegisteredAt,
    );
  }

  @override
  String toString() {
    return 'ScheduleModel(scheduleId: $scheduleId, displayWorkScheduleId: $displayWorkScheduleId, employeeName: $employeeName, accountName: $accountName, workCategory: $workCategory, isCommuteRegistered: $isCommuteRegistered)';
  }
}
