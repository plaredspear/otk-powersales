/// 오늘 일정 엔티티
///
/// 홈화면에 표시되는 오늘의 스케줄 정보를 나타낸다.
/// Backend ScheduleInfo DTO에 맞춘 필드 구조.
class Schedule {
  final int scheduleId;
  final String employeeName;
  final String employeeSfid;
  final String? accountName;
  final String? accountSfid;
  final String workCategory;
  final String? workType;
  final bool isCommuteRegistered;
  final DateTime? commuteRegisteredAt;

  const Schedule({
    required this.scheduleId,
    required this.employeeName,
    required this.employeeSfid,
    this.accountName,
    this.accountSfid,
    required this.workCategory,
    this.workType,
    required this.isCommuteRegistered,
    this.commuteRegisteredAt,
  });

  Schedule copyWith({
    int? scheduleId,
    String? employeeName,
    String? employeeSfid,
    String? accountName,
    String? accountSfid,
    String? workCategory,
    String? workType,
    bool? isCommuteRegistered,
    DateTime? commuteRegisteredAt,
    bool clearAccountName = false,
    bool clearAccountSfid = false,
    bool clearWorkType = false,
    bool clearCommuteRegisteredAt = false,
  }) {
    return Schedule(
      scheduleId: scheduleId ?? this.scheduleId,
      employeeName: employeeName ?? this.employeeName,
      employeeSfid: employeeSfid ?? this.employeeSfid,
      accountName: clearAccountName ? null : (accountName ?? this.accountName),
      accountSfid: clearAccountSfid ? null : (accountSfid ?? this.accountSfid),
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
        other.employeeName == employeeName &&
        other.employeeSfid == employeeSfid &&
        other.accountName == accountName &&
        other.accountSfid == accountSfid &&
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
      accountName,
      accountSfid,
      workCategory,
      workType,
      isCommuteRegistered,
      commuteRegisteredAt,
    );
  }

  @override
  String toString() {
    return 'Schedule(scheduleId: $scheduleId, employeeName: $employeeName, accountName: $accountName, workCategory: $workCategory, isCommuteRegistered: $isCommuteRegistered)';
  }
}
