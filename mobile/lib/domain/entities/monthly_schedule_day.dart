/// 월간 일정 - 특정 날짜의 근무 여부를 나타냄
class MonthlyScheduleDay {
  /// 날짜
  final DateTime date;

  /// 근무 여부
  final bool hasWork;

  const MonthlyScheduleDay({
    required this.date,
    required this.hasWork,
  });

  MonthlyScheduleDay copyWith({
    DateTime? date,
    bool? hasWork,
  }) {
    return MonthlyScheduleDay(
      date: date ?? this.date,
      hasWork: hasWork ?? this.hasWork,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'date': date.toIso8601String(),
      'hasWork': hasWork,
    };
  }

  factory MonthlyScheduleDay.fromJson(Map<String, dynamic> json) {
    return MonthlyScheduleDay(
      date: DateTime.parse(json['date'] as String),
      hasWork: json['hasWork'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlyScheduleDay &&
        other.date == date &&
        other.hasWork == hasWork;
  }

  @override
  int get hashCode => Object.hash(date, hasWork);

  @override
  String toString() {
    return 'MonthlyScheduleDay(date: $date, hasWork: $hasWork)';
  }
}
