/// 조장 여사원 일별 현황 (레거시 `employee/mngDaily.jsp` 대응 — 조회 전용).
///
/// 백엔드 `GET /api/v1/mobile/leader/daily-status` 응답 `LeaderDailyStatusResponse` 대응.
/// 진열/행사 근무자 + 연차자 + 상단 요약을 담는다.
class LeaderDailyStatus {
  final DateTime date;
  final LeaderDailyStatusSummary summary;
  final List<LeaderDailyWorker> displayWorkers;
  final List<LeaderDailyWorker> eventWorkers;
  final List<LeaderDailyEmployee> annualLeaveWorkers;

  const LeaderDailyStatus({
    required this.date,
    required this.summary,
    this.displayWorkers = const [],
    this.eventWorkers = const [],
    this.annualLeaveWorkers = const [],
  });
}

/// 상단 요약 — 진열/행사 출근/전체, 연차 인원.
class LeaderDailyStatusSummary {
  final int displayTotal;
  final int displayAttended;
  final int eventTotal;
  final int eventAttended;
  final int annualLeaveCount;

  const LeaderDailyStatusSummary({
    this.displayTotal = 0,
    this.displayAttended = 0,
    this.eventTotal = 0,
    this.eventAttended = 0,
    this.annualLeaveCount = 0,
  });
}

/// 진열/행사 근무자 1건(여사원 × 거래처 일정).
class LeaderDailyWorker {
  final int scheduleId;

  /// 진열 거래처 대리출근 등록용 진열 마스터 ID(진열 행만). 행사 행은 null.
  final int? displayWorkScheduleId;
  final int? employeeId;
  final String employeeName;
  final String employeeCode;
  final String accountName;
  final String accountCode;
  final String? workingCategory1;
  final String? workingCategory2;
  final String? workingCategory3;
  final bool attended;

  const LeaderDailyWorker({
    required this.scheduleId,
    this.displayWorkScheduleId,
    this.employeeId,
    required this.employeeName,
    required this.employeeCode,
    required this.accountName,
    required this.accountCode,
    this.workingCategory1,
    this.workingCategory2,
    this.workingCategory3,
    required this.attended,
  });

  /// 근무유형 표시용 — 비어있지 않은 카테고리만 `/` 로 연결 (레거시 `진열/상시/순회`).
  String get workCategoryLabel => [
        workingCategory1,
        workingCategory2,
        workingCategory3,
      ].whereType<String>().where((e) => e.isNotEmpty).join('/');

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is LeaderDailyWorker &&
        other.scheduleId == scheduleId &&
        other.accountCode == accountCode &&
        other.attended == attended;
  }

  @override
  int get hashCode => Object.hash(scheduleId, accountCode, attended);
}

/// 연차 여사원 항목 (이름/사번만).
class LeaderDailyEmployee {
  final int? employeeId;
  final String employeeName;
  final String employeeCode;

  const LeaderDailyEmployee({
    this.employeeId,
    required this.employeeName,
    required this.employeeCode,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is LeaderDailyEmployee &&
        other.employeeId == employeeId &&
        other.employeeCode == employeeCode;
  }

  @override
  int get hashCode => Object.hash(employeeId, employeeCode);
}
