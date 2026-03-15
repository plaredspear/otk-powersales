/// 출근등록 현황 엔티티
///
/// 거래처별 출근등록 상태 (완료/대기) 정보입니다.
class AttendanceStatus {
  final int scheduleId;
  final String storeName;
  final String workCategory;
  final String status;
  final String? workType;

  const AttendanceStatus({
    required this.scheduleId,
    required this.storeName,
    required this.workCategory,
    required this.status,
    this.workType,
  });

  /// 등록 완료 여부
  bool get isCompleted => status == 'REGISTERED';

  /// 대기 중 여부
  bool get isPending => status == 'PENDING';

  AttendanceStatus copyWith({
    int? scheduleId,
    String? storeName,
    String? workCategory,
    String? status,
    String? workType,
  }) {
    return AttendanceStatus(
      scheduleId: scheduleId ?? this.scheduleId,
      storeName: storeName ?? this.storeName,
      workCategory: workCategory ?? this.workCategory,
      status: status ?? this.status,
      workType: workType ?? this.workType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleId': scheduleId,
      'storeName': storeName,
      'workCategory': workCategory,
      'status': status,
      'workType': workType,
    };
  }

  factory AttendanceStatus.fromJson(Map<String, dynamic> json) {
    return AttendanceStatus(
      scheduleId: json['scheduleId'] as int,
      storeName: json['storeName'] as String,
      workCategory: json['workCategory'] as String,
      status: json['status'] as String,
      workType: json['workType'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceStatus &&
        other.scheduleId == scheduleId &&
        other.storeName == storeName &&
        other.workCategory == workCategory &&
        other.status == status &&
        other.workType == workType;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleId,
      storeName,
      workCategory,
      status,
      workType,
    );
  }

  @override
  String toString() {
    return 'AttendanceStatus(scheduleId: $scheduleId, storeName: $storeName, '
        'workCategory: $workCategory, status: $status, workType: $workType)';
  }
}
