/// 출근등록 현황 엔티티
///
/// 거래처별 출근등록 상태 (완료/대기) 정보입니다.
class AttendanceStatus {
  final int scheduleId;
  final String accountName;
  final String workCategory;
  final String status;
  final String? secondWorkType;

  const AttendanceStatus({
    required this.scheduleId,
    required this.accountName,
    required this.workCategory,
    required this.status,
    this.secondWorkType,
  });

  /// 등록 완료 여부
  bool get isCompleted => status == 'REGISTERED';

  /// 대기 중 여부
  bool get isPending => status == 'PENDING';

  AttendanceStatus copyWith({
    int? scheduleId,
    String? accountName,
    String? workCategory,
    String? status,
    String? secondWorkType,
  }) {
    return AttendanceStatus(
      scheduleId: scheduleId ?? this.scheduleId,
      accountName: accountName ?? this.accountName,
      workCategory: workCategory ?? this.workCategory,
      status: status ?? this.status,
      secondWorkType: secondWorkType ?? this.secondWorkType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleId': scheduleId,
      'accountName': accountName,
      'workCategory': workCategory,
      'status': status,
      'secondWorkType': secondWorkType,
    };
  }

  factory AttendanceStatus.fromJson(Map<String, dynamic> json) {
    return AttendanceStatus(
      scheduleId: json['scheduleId'] as int,
      accountName: json['accountName'] as String,
      workCategory: json['workCategory'] as String,
      status: json['status'] as String,
      secondWorkType: json['secondWorkType'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceStatus &&
        other.scheduleId == scheduleId &&
        other.accountName == accountName &&
        other.workCategory == workCategory &&
        other.status == status &&
        other.secondWorkType == secondWorkType;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      accountName,
      workCategory,
      status,
      secondWorkType,
    );
  }

  @override
  String toString() {
    return 'AttendanceStatus(scheduleId: $scheduleId, accountName: $accountName, '
        'workCategory: $workCategory, status: $status, secondWorkType: $secondWorkType)';
  }
}
