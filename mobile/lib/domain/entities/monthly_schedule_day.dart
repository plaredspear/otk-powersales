/// 월간 일정 - 특정 날짜의 근무 여부를 나타냄
class MonthlyScheduleDay {
  /// 날짜
  final DateTime date;

  /// 근무 여부
  final bool hasWork;

  /// 근무유형 (예: "근무", "연차" 등). 스케줄 없는 날은 null
  final String? workingType;

  /// 보고완료 거래처 수 (레거시 캘린더 셀 sum)
  final int completedCount;

  /// 담당 거래처 수 (레거시 캘린더 셀 cnt)
  final int totalCount;

  const MonthlyScheduleDay({
    required this.date,
    required this.hasWork,
    this.workingType,
    this.completedCount = 0,
    this.totalCount = 0,
  });

  /// 연차 여부
  bool get isAnnualLeave => workingType == '연차';

  /// 대휴 여부
  bool get isSubstituteHoliday => workingType == '대휴';

  MonthlyScheduleDay copyWith({
    DateTime? date,
    bool? hasWork,
    String? workingType,
    int? completedCount,
    int? totalCount,
  }) {
    return MonthlyScheduleDay(
      date: date ?? this.date,
      hasWork: hasWork ?? this.hasWork,
      workingType: workingType ?? this.workingType,
      completedCount: completedCount ?? this.completedCount,
      totalCount: totalCount ?? this.totalCount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'date': date.toIso8601String(),
      'hasWork': hasWork,
      'workingType': workingType,
      'completedCount': completedCount,
      'totalCount': totalCount,
    };
  }

  factory MonthlyScheduleDay.fromJson(Map<String, dynamic> json) {
    return MonthlyScheduleDay(
      date: DateTime.parse(json['date'] as String),
      hasWork: json['hasWork'] as bool,
      workingType: json['workingType'] as String?,
      completedCount: json['completedCount'] as int? ?? 0,
      totalCount: json['totalCount'] as int? ?? 0,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlyScheduleDay &&
        other.date == date &&
        other.hasWork == hasWork &&
        other.workingType == workingType &&
        other.completedCount == completedCount &&
        other.totalCount == totalCount;
  }

  @override
  int get hashCode =>
      Object.hash(date, hasWork, workingType, completedCount, totalCount);

  @override
  String toString() {
    return 'MonthlyScheduleDay(date: $date, hasWork: $hasWork, workingType: $workingType, '
        'completedCount: $completedCount, totalCount: $totalCount)';
  }
}
