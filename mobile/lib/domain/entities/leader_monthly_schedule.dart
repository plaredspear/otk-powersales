/// 조장 여사원 월간 일정 캘린더 — 레거시 `employee/mgnSchedule.jsp` + `calSchedule`.
class LeaderMonthlyCalendar {
  final int year;
  final int month;
  final List<LeaderCalendarDay> days;

  const LeaderMonthlyCalendar({
    required this.year,
    required this.month,
    required this.days,
  });
}

/// 일자별 출근 집계 — 레거시 캘린더 셀의 `출근완료수 / 전체수`(sum/cnt).
class LeaderCalendarDay {
  /// YYYY-MM-DD
  final String date;

  /// 전체 근무 건수 (레거시 cnt)
  final int total;

  /// 출근완료 건수 (레거시 sum)
  final int attended;

  const LeaderCalendarDay({
    required this.date,
    required this.total,
    required this.attended,
  });
}
