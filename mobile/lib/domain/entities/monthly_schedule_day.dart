/// 월간 일정 - 특정 날짜의 근무 여부를 나타냄
class MonthlyScheduleDay {
  /// 날짜
  final DateTime date;

  /// 근무 여부
  final bool hasWork;

  /// 근무유형 (예: "근무", "연차" 등). 스케줄 없는 날은 null
  final String? workingType;

  const MonthlyScheduleDay({
    required this.date,
    required this.hasWork,
    this.workingType,
  });

  /// 연차 여부
  bool get isAnnualLeave => workingType == '연차';

  /// 대휴 여부
  bool get isSubstituteHoliday => workingType == '대휴';

  MonthlyScheduleDay copyWith({
    DateTime? date,
    bool? hasWork,
    String? workingType,
  }) {
    return MonthlyScheduleDay(
      date: date ?? this.date,
      hasWork: hasWork ?? this.hasWork,
      workingType: workingType ?? this.workingType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'date': date.toIso8601String(),
      'hasWork': hasWork,
      'workingType': workingType,
    };
  }

  factory MonthlyScheduleDay.fromJson(Map<String, dynamic> json) {
    return MonthlyScheduleDay(
      date: DateTime.parse(json['date'] as String),
      hasWork: json['hasWork'] as bool,
      workingType: json['workingType'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlyScheduleDay &&
        other.date == date &&
        other.hasWork == hasWork &&
        other.workingType == workingType;
  }

  @override
  int get hashCode => Object.hash(date, hasWork, workingType);

  @override
  String toString() {
    return 'MonthlyScheduleDay(date: $date, hasWork: $hasWork, workingType: $workingType)';
  }
}
