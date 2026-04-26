/// 오늘 안전점검 상태 엔티티
///
/// 오늘 날짜의 안전점검 완료 여부를 나타냅니다.
class SafetyCheckTodayStatus {
  /// 오늘 안전점검 완료 여부
  final bool completed;

  /// 완료 시각 (미완료 시 null)
  final DateTime? submittedAt;

  const SafetyCheckTodayStatus({
    required this.completed,
    this.submittedAt,
  });

  SafetyCheckTodayStatus copyWith({
    bool? completed,
    DateTime? submittedAt,
  }) {
    return SafetyCheckTodayStatus(
      completed: completed ?? this.completed,
      submittedAt: submittedAt ?? this.submittedAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'completed': completed,
      'submittedAt': submittedAt?.toIso8601String(),
    };
  }

  factory SafetyCheckTodayStatus.fromJson(Map<String, dynamic> json) {
    return SafetyCheckTodayStatus(
      completed: json['completed'] as bool,
      submittedAt: json['submittedAt'] != null
          ? DateTime.parse(json['submittedAt'] as String)
          : null,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckTodayStatus &&
        other.completed == completed &&
        other.submittedAt == submittedAt;
  }

  @override
  int get hashCode {
    return Object.hash(completed, submittedAt);
  }

  @override
  String toString() {
    return 'SafetyCheckTodayStatus(completed: $completed, submittedAt: $submittedAt)';
  }
}
