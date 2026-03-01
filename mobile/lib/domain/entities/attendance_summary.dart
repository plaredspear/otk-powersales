/// 출근 현황 집계 엔티티
///
/// 홈화면에 표시되는 오늘의 출근 등록 현황 집계 정보를 나타낸다.
class AttendanceSummary {
  final int totalCount;
  final int registeredCount;

  const AttendanceSummary({
    required this.totalCount,
    required this.registeredCount,
  });

  AttendanceSummary copyWith({
    int? totalCount,
    int? registeredCount,
  }) {
    return AttendanceSummary(
      totalCount: totalCount ?? this.totalCount,
      registeredCount: registeredCount ?? this.registeredCount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceSummary &&
        other.totalCount == totalCount &&
        other.registeredCount == registeredCount;
  }

  @override
  int get hashCode {
    return Object.hash(totalCount, registeredCount);
  }

  @override
  String toString() {
    return 'AttendanceSummary(totalCount: $totalCount, registeredCount: $registeredCount)';
  }
}
