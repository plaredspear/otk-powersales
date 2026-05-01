/// 조장 대리 일정 등록 결과.
class LeaderScheduleCreated {
  final int scheduleId;
  final int targetEmployeeId;
  final DateTime workingDate;
  final String workingType;
  final String workingCategory3;
  final int proxyRegisteredBy;

  const LeaderScheduleCreated({
    required this.scheduleId,
    required this.targetEmployeeId,
    required this.workingDate,
    required this.workingType,
    required this.workingCategory3,
    required this.proxyRegisteredBy,
  });
}
