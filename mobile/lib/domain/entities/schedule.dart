/// 오늘 일정 엔티티
///
/// 홈화면에 표시되는 오늘의 스케줄 정보를 나타낸다.
/// Backend ScheduleInfo DTO에 맞춘 필드 구조.
class Schedule {
  final String scheduleId;
  final String employeeName;
  final String employeeSfid;
  final String? storeName;
  final String? storeSfid;
  final String workCategory;
  final String? workType;
  final bool isCommuteRegistered;
  final DateTime? commuteRegisteredAt;

  const Schedule({
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

  Schedule copyWith({
    String? scheduleId,
    String? employeeName,
    String? employeeSfid,
    String? storeName,
    String? storeSfid,
    String? workCategory,
    String? workType,
    bool? isCommuteRegistered,
    DateTime? commuteRegisteredAt,
    bool clearStoreName = false,
    bool clearStoreSfid = false,
    bool clearWorkType = false,
    bool clearCommuteRegisteredAt = false,
  }) {
    return Schedule(
      scheduleId: scheduleId ?? this.scheduleId,
      employeeName: employeeName ?? this.employeeName,
      employeeSfid: employeeSfid ?? this.employeeSfid,
      storeName: clearStoreName ? null : (storeName ?? this.storeName),
      storeSfid: clearStoreSfid ? null : (storeSfid ?? this.storeSfid),
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
    return 'Schedule(scheduleId: $scheduleId, employeeName: $employeeName, storeName: $storeName, workCategory: $workCategory, isCommuteRegistered: $isCommuteRegistered)';
  }
}
