/// 출근등록 현황 엔티티
///
/// 거래처별 출근등록 상태 (완료/대기) 정보입니다.
class AttendanceStatus {
  final int storeId;
  final String storeName;
  final String status;
  final String? workType;
  final DateTime? registeredAt;

  const AttendanceStatus({
    required this.storeId,
    required this.storeName,
    required this.status,
    this.workType,
    this.registeredAt,
  });

  /// 등록 완료 여부
  bool get isCompleted => status == 'COMPLETED';

  /// 대기 중 여부
  bool get isPending => status == 'PENDING';

  AttendanceStatus copyWith({
    int? storeId,
    String? storeName,
    String? status,
    String? workType,
    DateTime? registeredAt,
  }) {
    return AttendanceStatus(
      storeId: storeId ?? this.storeId,
      storeName: storeName ?? this.storeName,
      status: status ?? this.status,
      workType: workType ?? this.workType,
      registeredAt: registeredAt ?? this.registeredAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'storeId': storeId,
      'storeName': storeName,
      'status': status,
      'workType': workType,
      'registeredAt': registeredAt?.toIso8601String(),
    };
  }

  factory AttendanceStatus.fromJson(Map<String, dynamic> json) {
    return AttendanceStatus(
      storeId: json['storeId'] as int,
      storeName: json['storeName'] as String,
      status: json['status'] as String,
      workType: json['workType'] as String?,
      registeredAt: json['registeredAt'] != null
          ? DateTime.parse(json['registeredAt'] as String)
          : null,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceStatus &&
        other.storeId == storeId &&
        other.storeName == storeName &&
        other.status == status &&
        other.workType == workType &&
        other.registeredAt == registeredAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      storeId,
      storeName,
      status,
      workType,
      registeredAt,
    );
  }

  @override
  String toString() {
    return 'AttendanceStatus(storeId: $storeId, storeName: $storeName, '
        'status: $status, workType: $workType, '
        'registeredAt: $registeredAt)';
  }
}
