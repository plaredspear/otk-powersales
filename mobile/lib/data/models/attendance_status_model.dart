import '../../domain/entities/attendance_status.dart';

/// 출근등록 현황 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceStatus 엔티티로 변환합니다.
class AttendanceStatusModel {
  final int storeId;
  final String storeName;
  final String status;
  final String? workType;
  final DateTime? registeredAt;

  const AttendanceStatusModel({
    required this.storeId,
    required this.storeName,
    required this.status,
    this.workType,
    this.registeredAt,
  });

  /// snake_case JSON에서 파싱
  factory AttendanceStatusModel.fromJson(Map<String, dynamic> json) {
    return AttendanceStatusModel(
      storeId: json['store_id'] as int,
      storeName: json['store_name'] as String,
      status: json['status'] as String,
      workType: json['work_type'] as String?,
      registeredAt: json['registered_at'] != null
          ? DateTime.parse(json['registered_at'] as String)
          : null,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'store_id': storeId,
      'store_name': storeName,
      'status': status,
      'work_type': workType,
      'registered_at': registeredAt?.toIso8601String(),
    };
  }

  /// Domain Entity로 변환
  AttendanceStatus toEntity() {
    return AttendanceStatus(
      storeId: storeId,
      storeName: storeName,
      status: status,
      workType: workType,
      registeredAt: registeredAt,
    );
  }

  /// Domain Entity에서 생성
  factory AttendanceStatusModel.fromEntity(AttendanceStatus entity) {
    return AttendanceStatusModel(
      storeId: entity.storeId,
      storeName: entity.storeName,
      status: entity.status,
      workType: entity.workType,
      registeredAt: entity.registeredAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceStatusModel &&
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
    return 'AttendanceStatusModel(storeId: $storeId, storeName: $storeName, '
        'status: $status, workType: $workType, '
        'registeredAt: $registeredAt)';
  }
}
