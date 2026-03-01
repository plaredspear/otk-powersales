/// 출근등록 현황 엔티티
///
/// 거래처별 출근등록 상태 (완료/대기) 정보입니다.
class AttendanceStatus {
  final String scheduleSfid;
  final String storeName;
  final String workCategory;
  final String status;
  final String? workType;

  const AttendanceStatus({
    required this.scheduleSfid,
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
    String? scheduleSfid,
    String? storeName,
    String? workCategory,
    String? status,
    String? workType,
  }) {
    return AttendanceStatus(
      scheduleSfid: scheduleSfid ?? this.scheduleSfid,
      storeName: storeName ?? this.storeName,
      workCategory: workCategory ?? this.workCategory,
      status: status ?? this.status,
      workType: workType ?? this.workType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scheduleSfid': scheduleSfid,
      'storeName': storeName,
      'workCategory': workCategory,
      'status': status,
      'workType': workType,
    };
  }

  factory AttendanceStatus.fromJson(Map<String, dynamic> json) {
    return AttendanceStatus(
      scheduleSfid: json['scheduleSfid'] as String,
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
        other.scheduleSfid == scheduleSfid &&
        other.storeName == storeName &&
        other.workCategory == workCategory &&
        other.status == status &&
        other.workType == workType;
  }

  @override
  int get hashCode {
    return Object.hash(
      scheduleSfid,
      storeName,
      workCategory,
      status,
      workType,
    );
  }

  @override
  String toString() {
    return 'AttendanceStatus(scheduleSfid: $scheduleSfid, storeName: $storeName, '
        'workCategory: $workCategory, status: $status, workType: $workType)';
  }
}
