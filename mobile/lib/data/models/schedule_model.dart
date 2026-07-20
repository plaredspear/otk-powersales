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
  final String? workCategory2;
  final String? workType;

  /// 근무유형4 (상온/냉동/냉장/라면/만두…) — 레거시 workingcategory4__c.
  /// 행사 일정만 값이 있고 진열은 null. 오늘의 등록 현황 팝업의 "완료 (상온)" 표기에 사용된다.
  final String? secondWorkType;
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
    this.workCategory2,
    this.workType,
    this.secondWorkType,
    required this.isCommuteRegistered,
    this.commuteRegisteredAt,
  });

  factory ScheduleModel.fromJson(Map<String, dynamic> json) {
    return ScheduleModel(
      scheduleId: json['scheduleId'] as int,
      displayWorkScheduleId: json['displayWorkScheduleId'] as int?,
      employeeName: json['employeeName'] as String,
      employeeCode: json['employeeCode'] as String,
      accountName: json['accountName'] as String?,
      accountId: json['accountId'] as int?,
      workCategory: json['workCategory'] as String,
      workCategory2: json['workCategory2'] as String?,
      workType: json['workType'] as String?,
      secondWorkType: json['secondWorkType'] as String?,
      isCommuteRegistered: json['isCommuteRegistered'] as bool,
      commuteRegisteredAt: json['commuteRegisteredAt'] != null
          ? DateTime.parse(json['commuteRegisteredAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleId': scheduleId,
      'displayWorkScheduleId': displayWorkScheduleId,
      'employeeName': employeeName,
      'employeeCode': employeeCode,
      'accountName': accountName,
      'accountId': accountId,
      'workCategory': workCategory,
      'workCategory2': workCategory2,
      'workType': workType,
      'secondWorkType': secondWorkType,
      'isCommuteRegistered': isCommuteRegistered,
      'commuteRegisteredAt': commuteRegisteredAt?.toIso8601String(),
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
      workCategory2: workCategory2,
      workType: workType,
      secondWorkType: secondWorkType,
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
      workCategory2: entity.workCategory2,
      workType: entity.workType,
      secondWorkType: entity.secondWorkType,
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
        other.workCategory2 == workCategory2 &&
        other.workType == workType &&
        other.secondWorkType == secondWorkType &&
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
      workCategory2,
      workType,
      secondWorkType,
      isCommuteRegistered,
      commuteRegisteredAt,
    );
  }

  @override
  String toString() {
    return 'ScheduleModel(scheduleId: $scheduleId, displayWorkScheduleId: $displayWorkScheduleId, employeeName: $employeeName, accountName: $accountName, workCategory: $workCategory, isCommuteRegistered: $isCommuteRegistered)';
  }
}
