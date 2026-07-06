/// 오늘 일정 엔티티
///
/// 홈화면에 표시되는 오늘의 스케줄 정보를 나타낸다.
/// Backend ScheduleInfo DTO에 맞춘 필드 구조.
class Schedule {
  final int scheduleId;
  final int? displayWorkScheduleId;
  final String employeeName;
  final String employeeCode;
  final String? accountName;
  final int? accountId;

  /// 근무구분 (진열/행사) — 레거시 workingcategory1__c.
  final String workCategory;

  /// 근무형태 (고정/순회/격고) — 레거시 workingcategory3__c.
  ///
  /// 순회/격고 근무자는 출근 전 일정을 숨기는 등 홈 카드 표시 분기에 사용된다.
  final String? workType;

  final bool isCommuteRegistered;
  final DateTime? commuteRegisteredAt;

  const Schedule({
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

  Schedule copyWith({
    int? scheduleId,
    int? displayWorkScheduleId,
    String? employeeName,
    String? employeeCode,
    String? accountName,
    int? accountId,
    String? workCategory,
    String? workType,
    bool? isCommuteRegistered,
    DateTime? commuteRegisteredAt,
    bool clearAccountName = false,
    bool clearAccountId = false,
    bool clearWorkType = false,
    bool clearCommuteRegisteredAt = false,
    bool clearDisplayWorkScheduleId = false,
  }) {
    return Schedule(
      scheduleId: scheduleId ?? this.scheduleId,
      displayWorkScheduleId: clearDisplayWorkScheduleId ? null : (displayWorkScheduleId ?? this.displayWorkScheduleId),
      employeeName: employeeName ?? this.employeeName,
      employeeCode: employeeCode ?? this.employeeCode,
      accountName: clearAccountName ? null : (accountName ?? this.accountName),
      accountId: clearAccountId ? null : (accountId ?? this.accountId),
      workCategory: workCategory ?? this.workCategory,
      workType: clearWorkType ? null : (workType ?? this.workType),
      isCommuteRegistered: isCommuteRegistered ?? this.isCommuteRegistered,
      commuteRegisteredAt: clearCommuteRegisteredAt
          ? null
          : (commuteRegisteredAt ?? this.commuteRegisteredAt),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Schedule &&
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
    return 'Schedule(scheduleId: $scheduleId, displayWorkScheduleId: $displayWorkScheduleId, employeeName: $employeeName, accountName: $accountName, workCategory: $workCategory, isCommuteRegistered: $isCommuteRegistered)';
  }
}
