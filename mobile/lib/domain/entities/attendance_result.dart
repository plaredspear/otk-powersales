/// 출근등록 결과 엔티티
///
/// 출근등록 API 호출 후 반환되는 등록 결과 정보입니다.
class AttendanceResult {
  final int scheduleId;
  final String accountName;
  final String workType;
  final double distanceKm;
  final int totalCount;
  final int registeredCount;

  const AttendanceResult({
    required this.scheduleId,
    required this.accountName,
    required this.workType,
    required this.distanceKm,
    required this.totalCount,
    required this.registeredCount,
  });

  AttendanceResult copyWith({
    int? scheduleId,
    String? accountName,
    String? workType,
    double? distanceKm,
    int? totalCount,
    int? registeredCount,
  }) {
    return AttendanceResult(
      scheduleId: scheduleId ?? this.scheduleId,
      accountName: accountName ?? this.accountName,
      workType: workType ?? this.workType,
      distanceKm: distanceKm ?? this.distanceKm,
      totalCount: totalCount ?? this.totalCount,
      registeredCount: registeredCount ?? this.registeredCount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleId': scheduleId,
      'accountName': accountName,
      'workType': workType,
      'distanceKm': distanceKm,
      'totalCount': totalCount,
      'registeredCount': registeredCount,
    };
  }

  factory AttendanceResult.fromJson(Map<String, dynamic> json) {
    return AttendanceResult(
      scheduleId: json['scheduleId'] as int,
      accountName: json['accountName'] as String,
      workType: json['workType'] as String,
      distanceKm: (json['distanceKm'] as num).toDouble(),
      totalCount: json['totalCount'] as int,
      registeredCount: json['registeredCount'] as int,
    );
  }

  /// 모든 거래처 등록 완료 여부
  bool get isAllRegistered => registeredCount >= totalCount;

  /// 남은 거래처 수
  int get remainingCount => totalCount - registeredCount;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceResult &&
        other.scheduleId == scheduleId &&
        other.accountName == accountName &&
        other.workType == workType &&
        other.distanceKm == distanceKm &&
        other.totalCount == totalCount &&
        other.registeredCount == registeredCount;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      accountName,
      workType,
      distanceKm,
      totalCount,
      registeredCount,
    );
  }

  @override
  String toString() {
    return 'AttendanceResult(scheduleId: $scheduleId, '
        'accountName: $accountName, workType: $workType, '
        'distanceKm: $distanceKm, totalCount: $totalCount, '
        'registeredCount: $registeredCount)';
  }
}
