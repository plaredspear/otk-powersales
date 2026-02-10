import '../../domain/entities/attendance_result.dart';

/// 출근등록 결과 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceResult 엔티티로 변환합니다.
class AttendanceResultModel {
  final int attendanceId;
  final int storeId;
  final String storeName;
  final String workType;
  final DateTime registeredAt;
  final int totalCount;
  final int registeredCount;

  const AttendanceResultModel({
    required this.attendanceId,
    required this.storeId,
    required this.storeName,
    required this.workType,
    required this.registeredAt,
    required this.totalCount,
    required this.registeredCount,
  });

  /// snake_case JSON에서 파싱
  factory AttendanceResultModel.fromJson(Map<String, dynamic> json) {
    return AttendanceResultModel(
      attendanceId: json['attendance_id'] as int,
      storeId: json['store_id'] as int,
      storeName: json['store_name'] as String,
      workType: json['work_type'] as String,
      registeredAt: DateTime.parse(json['registered_at'] as String),
      totalCount: json['total_count'] as int,
      registeredCount: json['registered_count'] as int,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'attendance_id': attendanceId,
      'store_id': storeId,
      'store_name': storeName,
      'work_type': workType,
      'registered_at': registeredAt.toIso8601String(),
      'total_count': totalCount,
      'registered_count': registeredCount,
    };
  }

  /// Domain Entity로 변환
  AttendanceResult toEntity() {
    return AttendanceResult(
      attendanceId: attendanceId,
      storeId: storeId,
      storeName: storeName,
      workType: workType,
      registeredAt: registeredAt,
      totalCount: totalCount,
      registeredCount: registeredCount,
    );
  }

  /// Domain Entity에서 생성
  factory AttendanceResultModel.fromEntity(AttendanceResult entity) {
    return AttendanceResultModel(
      attendanceId: entity.attendanceId,
      storeId: entity.storeId,
      storeName: entity.storeName,
      workType: entity.workType,
      registeredAt: entity.registeredAt,
      totalCount: entity.totalCount,
      registeredCount: entity.registeredCount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceResultModel &&
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
    return 'AttendanceResultModel(attendanceId: $attendanceId, '
        'storeId: $storeId, storeName: $storeName, workType: $workType, '
        'registeredAt: $registeredAt, totalCount: $totalCount, '
        'registeredCount: $registeredCount)';
  }
}
