/// 조장 진열 일정(마스터) 상세 — 편집 화면 선조회/결과용.
class LeaderDisplaySchedule {
  final int displayWorkScheduleId;
  final int? employeeId;
  final String? employeeName;
  final int? accountId;
  final String? accountName;
  final DateTime? startDate;
  final DateTime? endDate;
  final String? typeOfWork3; // 고정/격고/순회
  final String? typeOfWork4; // 상온/냉동냉장
  final String? typeOfWork5; // 상시/임시

  const LeaderDisplaySchedule({
    required this.displayWorkScheduleId,
    this.employeeId,
    this.employeeName,
    this.accountId,
    this.accountName,
    this.startDate,
    this.endDate,
    this.typeOfWork3,
    this.typeOfWork4,
    this.typeOfWork5,
  });
}
