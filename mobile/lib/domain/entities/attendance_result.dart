/// 출근등록 결과 엔티티
///
/// 출근등록 API 호출 후 반환되는 등록 결과 정보입니다.
class AttendanceResult {
  final int attendanceId;
  final int storeId;
  final String storeName;
  final String workType;
  final DateTime registeredAt;
  final int totalCount;
  final int registeredCount;

  const AttendanceResult({
    required this.attendanceId,
    required this.storeId,
    required this.storeName,
    required this.workType,
    required this.registeredAt,
    required this.totalCount,
    required this.registeredCount,
  });

  AttendanceResult copyWith({
    int? attendanceId,
    int? storeId,
    String? storeName,
    String? workType,
    DateTime? registeredAt,
    int? totalCount,
    int? registeredCount,
  }) {
    return AttendanceResult(
      attendanceId: attendanceId ?? this.attendanceId,
      storeId: storeId ?? this.storeId,
      storeName: storeName ?? this.storeName,
      workType: workType ?? this.workType,
      registeredAt: registeredAt ?? this.registeredAt,
      totalCount: totalCount ?? this.totalCount,
      registeredCount: registeredCount ?? this.registeredCount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'attendanceId': attendanceId,
      'storeId': storeId,
      'storeName': storeName,
      'workType': workType,
      'registeredAt': registeredAt.toIso8601String(),
      'totalCount': totalCount,
      'registeredCount': registeredCount,
    };
  }

  factory AttendanceResult.fromJson(Map<String, dynamic> json) {
    return AttendanceResult(
      attendanceId: json['attendanceId'] as int,
      storeId: json['storeId'] as int,
      storeName: json['storeName'] as String,
      workType: json['workType'] as String,
      registeredAt: DateTime.parse(json['registeredAt'] as String),
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
        other.attendanceId == attendanceId &&
        other.storeId == storeId &&
        other.storeName == storeName &&
        other.workType == workType &&
        other.registeredAt == registeredAt &&
        other.totalCount == totalCount &&
        other.registeredCount == registeredCount;
  }

  @override
  int get hashCode {
    return Object.hash(
      attendanceId,
      storeId,
      storeName,
      workType,
      registeredAt,
      totalCount,
      registeredCount,
    );
  }

  @override
  String toString() {
    return 'AttendanceResult(attendanceId: $attendanceId, storeId: $storeId, '
        'storeName: $storeName, workType: $workType, '
        'registeredAt: $registeredAt, totalCount: $totalCount, '
        'registeredCount: $registeredCount)';
  }
}
